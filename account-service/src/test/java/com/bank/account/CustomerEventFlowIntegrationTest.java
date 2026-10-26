package com.bank.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = "app.kafka.customer-events.retry-interval=200ms")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CustomerEventFlowIntegrationTest {

    private static final String TOPIC = "customer-events";
    private static final String DEAD_LETTER_TOPIC = "customer-events.DLT";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void customerEventsDriveTheProjectionAndTheAccounts() throws Exception {
        UUID customerId = UUID.randomUUID();
        String accountNumber = uniqueAccountNumber();
        String created = event(UUID.randomUUID(), "CUSTOMER_CREATED", customerId, "Jose Lema", true, Instant.now());

        publish(customerId, created);
        awaitSnapshotActive(customerId, true);

        openAccount(accountNumber, customerId).andExpect(status().isCreated());

        publish(customerId, created);
        UUID deletedEventId = UUID.randomUUID();
        publish(customerId, event(deletedEventId, "CUSTOMER_DELETED", customerId, "Jose Lema", false, Instant.now()));

        awaitSnapshotActive(customerId, false);
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT active FROM account WHERE account_number = ?", Boolean.class, accountNumber)).isFalse());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, deletedEventId)).isEqualTo(1);

        openAccount(uniqueAccountNumber(), customerId).andExpect(status().isUnprocessableContent());
    }

    @Test
    void accountCannotBeOpenedBeforeTheCustomerIsKnown() throws Exception {
        openAccount(uniqueAccountNumber(), UUID.randomUUID()).andExpect(status().isUnprocessableContent());
    }

    @Test
    void olderEventDoesNotOverwriteANewerState() {
        UUID customerId = UUID.randomUUID();
        Instant now = Instant.now();
        UUID olderEventId = UUID.randomUUID();

        publish(customerId, event(UUID.randomUUID(), "CUSTOMER_UPDATED", customerId, "New Name", true, now));
        publish(customerId, event(olderEventId, "CUSTOMER_UPDATED", customerId, "Old Name", false, now.minusSeconds(60)));

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, olderEventId)).isEqualTo(1));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM customer_snapshot WHERE customer_id = ?", String.class, customerId))
                .isEqualTo("New Name");
    }

    @Test
    void invalidMessageGoesToTheDeadLetterTopicAndProcessingContinues() {
        String key = "invalid-" + UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        kafkaTemplate.send(TOPIC, key, "{\"eventId\": ");
        publish(customerId, event(UUID.randomUUID(), "CUSTOMER_CREATED", customerId, "After Invalid", true, Instant.now()));

        assertThat(awaitDeadLetter(key).value()).isEqualTo("{\"eventId\": ");
        awaitSnapshotActive(customerId, true);
    }

    @Test
    void permanentDataErrorGoesStraightToTheDeadLetterTopic() {
        UUID customerId = UUID.randomUUID();
        String tooLongName = "x".repeat(101);

        publish(customerId, event(UUID.randomUUID(), "CUSTOMER_CREATED", customerId, tooLongName, true, Instant.now()));

        ConsumerRecord<String, String> deadLetter = awaitDeadLetter(customerId.toString());
        assertThat(deadLetter.value()).contains(tooLongName);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM customer_snapshot WHERE customer_id = ?", Integer.class, customerId)).isZero();
    }

    private void publish(UUID customerId, String payload) {
        kafkaTemplate.send(TOPIC, customerId.toString(), payload);
    }

    private ResultActions openAccount(String accountNumber, UUID customerId) throws Exception {
        String body = """
                {
                  "accountNumber": "%s",
                  "accountType": "SAVINGS",
                  "initialBalance": 2000.00,
                  "customerId": "%s"
                }
                """.formatted(accountNumber, customerId);
        return mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void awaitSnapshotActive(UUID customerId, boolean active) {
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> assertThat(jdbcTemplate.queryForList(
                "SELECT active FROM customer_snapshot WHERE customer_id = ?", Boolean.class, customerId))
                .containsExactly(active));
    }

    private ConsumerRecord<String, String> awaitDeadLetter(String key) {
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        try (Consumer<String, String> consumer = consumerFactory.createConsumer("dlt-test-" + UUID.randomUUID(), null)) {
            consumer.subscribe(List.of(DEAD_LETTER_TOPIC));
            return await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).until(() -> {
                KafkaTestUtils.getRecords(consumer, POLL_INTERVAL).forEach(received::add);
                return received.stream().filter(record -> key.equals(record.key())).findFirst().orElse(null);
            }, record -> record != null);
        }
    }

    private static String uniqueAccountNumber() {
        return String.valueOf(100_000_000L + Math.abs(UUID.randomUUID().getMostSignificantBits() % 900_000_000L));
    }

    private static String event(UUID eventId, String eventType, UUID customerId, String name, boolean active,
            Instant occurredAt) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "version": 1,
                  "occurredAt": "%s",
                  "customer": { "customerId": "%s", "name": "%s", "active": %s }
                }
                """.formatted(eventId, eventType, occurredAt, customerId, name, active);
    }
}
