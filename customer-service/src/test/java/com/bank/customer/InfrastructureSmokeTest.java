package com.bank.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@Import(TestcontainersConfiguration.class)
class InfrastructureSmokeTest {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Test
    void liquibaseRunsAgainstPostgres() {
        Integer changelogTables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'databasechangelog'",
                Integer.class);

        assertThat(changelogTables).isEqualTo(1);
    }

    @Test
    void messageRoundTripsThroughKafka() throws Exception {
        String topic = "smoke-" + UUID.randomUUID();
        String payload = "ping";

        kafkaTemplate.send(topic, "key", payload).get();

        try (Consumer<String, String> consumer = consumerFactory.createConsumer("smoke-group", null)) {
            consumer.subscribe(List.of(topic));
            ConsumerRecord<String, String> received = KafkaTestUtils.getSingleRecord(consumer, topic, POLL_TIMEOUT);

            assertThat(received.value()).isEqualTo(payload);
        }
    }
}
