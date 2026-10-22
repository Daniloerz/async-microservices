package com.bank.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CustomerApiIntegrationTest {

    private static final String CUSTOMERS_URL = "/api/v1/customers";
    private static final String TOPIC = "customer-events";
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Autowired
    private JsonMapper jsonMapper;

    private Consumer<String, String> consumer;
    private final List<ConsumerRecord<String, String>> receivedRecords = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE customer, person, outbox_event CASCADE");
        consumer = consumerFactory.createConsumer("api-test-" + UUID.randomUUID(), null);
        consumer.subscribe(List.of(TOPIC));
    }

    @AfterEach
    void closeConsumer() {
        consumer.close();
    }

    @Test
    void customerLifecycleIsStoredAndPublishedThroughTheOutbox() throws Exception {
        String customerId = createCustomer("1712345678");

        JsonNode created = awaitEvent(customerId, "CUSTOMER_CREATED");
        assertThat(created.path("version").asInt()).isEqualTo(1);
        assertThat(created.path("occurredAt").asString()).matches("\\d{4}-\\d{2}-\\d{2}T.*Z");
        assertThat(created.path("customer").path("name").asString()).isEqualTo("Jose Lema");
        assertThat(created.path("customer").path("active").asBoolean()).isTrue();
        assertThat(created.has("password") || created.path("customer").has("identification")).isFalse();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM customer WHERE customer_id = ?::uuid", String.class, customerId))
                .startsWith("$2a$");
        await().atMost(EVENT_TIMEOUT).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE published_at IS NULL", Integer.class)).isZero());

        mockMvc.perform(patch(CUSTOMERS_URL + "/" + customerId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk());
        JsonNode updated = awaitEvent(customerId, "CUSTOMER_UPDATED");
        assertThat(updated.path("customer").path("active").asBoolean()).isFalse();

        mockMvc.perform(delete(CUSTOMERS_URL + "/" + customerId)).andExpect(status().isNoContent());
        mockMvc.perform(get(CUSTOMERS_URL + "/" + customerId)).andExpect(status().isNotFound());
        JsonNode deleted = awaitEvent(customerId, "CUSTOMER_DELETED");
        assertThat(deleted.path("customer").path("active").asBoolean()).isFalse();

        assertThat(jdbcTemplate.queryForList(
                "SELECT event_type FROM outbox_event WHERE aggregate_id = ?::uuid ORDER BY id", String.class, customerId))
                .containsExactly("CUSTOMER_CREATED", "CUSTOMER_UPDATED", "CUSTOMER_DELETED");
    }

    @Test
    void failedRequestLeavesNoEventInTheOutbox() throws Exception {
        createCustomer("1712345678");
        int eventsBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_event", Integer.class);

        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content(customerBody("1712345678")))
                .andExpect(status().isConflict());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_event", Integer.class))
                .isEqualTo(eventsBefore);
    }

    @Test
    void concurrentCreationsWithTheSameIdentificationLetOnlyOneSucceed() throws Exception {
        int requests = 2;
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> createRequest = () -> {
            start.await();
            return mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON)
                            .content(customerBody("1799999999")))
                    .andReturn().getResponse().getStatus();
        };

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
            List<Future<Integer>> futures = List.of(executor.submit(createRequest), executor.submit(createRequest));
            start.countDown();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
        }

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM person WHERE identification = '1799999999'", Integer.class)).isEqualTo(1);
    }

    private String createCustomer(String identification) throws Exception {
        MvcResult result = mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(customerBody(identification)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.customerId");
    }

    private JsonNode awaitEvent(String customerId, String eventType) {
        return await().atMost(EVENT_TIMEOUT).pollInterval(POLL_INTERVAL).until(() -> {
            KafkaTestUtils.getRecords(consumer, POLL_INTERVAL).forEach(receivedRecords::add);
            return receivedRecords.stream()
                    .filter(record -> customerId.equals(record.key()))
                    .map(record -> jsonMapper.readTree(record.value()))
                    .filter(event -> eventType.equals(event.path("eventType").asString()))
                    .findFirst()
                    .orElse(null);
        }, event -> event != null);
    }

    private static String customerBody(String identification) {
        return """
                {
                  "name": "Jose Lema",
                  "gender": "MALE",
                  "age": 35,
                  "identification": "%s",
                  "address": "Otavalo sn y principal",
                  "phone": "098254785",
                  "password": "1234"
                }
                """.formatted(identification);
    }
}
