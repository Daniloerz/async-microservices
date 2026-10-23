package com.bank.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.infrastructure.persistence.CustomerSnapshotPersistenceAdapter;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "app.kafka.customer-events.retry-interval=200ms")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MovementApiIntegrationTest {

    private static final int PARALLEL_WITHDRAWALS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CustomerSnapshotPersistenceAdapter customerSnapshotPersistenceAdapter;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE movement, account CASCADE");
        customerId = UUID.randomUUID();
        customerSnapshotPersistenceAdapter.save(new CustomerSnapshot(customerId, "Marianela Montalvo", true, Instant.now()));
    }

    @Test
    void challengeMovementsLeaveTheExpectedBalances() throws Exception {
        openAccount("478758", "2000");
        openAccount("225487", "100");
        openAccount("495878", "0");
        openAccount("496825", "540");

        registerMovement("478758", "-575").andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("WITHDRAWAL")).andExpect(jsonPath("$.balance").value(1425.0));
        registerMovement("225487", "600").andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("DEPOSIT")).andExpect(jsonPath("$.balance").value(700.0));
        registerMovement("495878", "150").andExpect(status().isCreated()).andExpect(jsonPath("$.balance").value(150.0));
        registerMovement("496825", "-540").andExpect(status().isCreated()).andExpect(jsonPath("$.balance").value(0.0));

        assertThat(balanceOf("478758")).isEqualByComparingTo("1425");
        assertThat(balanceOf("225487")).isEqualByComparingTo("700");
        assertThat(balanceOf("495878")).isEqualByComparingTo("150");
        assertThat(balanceOf("496825")).isEqualByComparingTo("0");
    }

    @Test
    void movementWithoutBalanceIsRejectedAndNothingChanges() throws Exception {
        openAccount("496825", "540");

        registerMovement("496825", "-540.01")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Insufficient balance"));

        assertThat(balanceOf("496825")).isEqualByComparingTo("540");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM movement", Integer.class)).isZero();
    }

    @Test
    void concurrentWithdrawalsNeverOverdraw() throws Exception {
        openAccount("300100", "100");
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> withdrawal = () -> {
            start.await();
            return registerMovement("300100", "-30").andReturn().getResponse().getStatus();
        };

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_WITHDRAWALS)) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int request = 0; request < PARALLEL_WITHDRAWALS; request++) {
                futures.add(executor.submit(withdrawal));
            }
            start.countDown();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
        }

        assertThat(statuses).containsExactlyInAnyOrder(201, 201, 201, 422, 422);
        assertThat(balanceOf("300100")).isEqualByComparingTo("10");
        assertThat(jdbcTemplate.queryForList(
                "SELECT balance FROM movement WHERE account_number = '300100' ORDER BY id", BigDecimal.class))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("70"), new BigDecimal("40"), new BigDecimal("10"));
        assertThat(jdbcTemplate.queryForList(
                "SELECT movement_date FROM movement WHERE account_number = '300100' ORDER BY id", Instant.class))
                .isSorted();
    }

    @Test
    void onlyTheLastMovementCanBeCorrected() throws Exception {
        openAccount("225487", "100");
        String first = movementIdOf(registerMovement("225487", "600"));
        String last = movementIdOf(registerMovement("225487", "-200"));

        correctMovement(first, "500").andExpect(status().isConflict());

        correctMovement(last, "-700.01")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Insufficient balance"));

        correctMovement(last, "-650")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(-650.0))
                .andExpect(jsonPath("$.balance").value(50.0));
        assertThat(balanceOf("225487")).isEqualByComparingTo("50");
    }

    private void openAccount(String accountNumber, String initialBalance) throws Exception {
        String body = """
                { "accountNumber": "%s", "accountType": "SAVINGS", "initialBalance": %s, "customerId": "%s" }
                """.formatted(accountNumber, initialBalance, customerId);
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private ResultActions registerMovement(String accountNumber, String amount) throws Exception {
        String body = "{ \"accountNumber\": \"%s\", \"amount\": %s }".formatted(accountNumber, amount);
        return mockMvc.perform(post("/api/v1/movements").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions correctMovement(String movementId, String amount) throws Exception {
        return mockMvc.perform(put("/api/v1/movements/" + movementId).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"amount\": %s }".formatted(amount)));
    }

    private static String movementIdOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.movementId");
    }

    private BigDecimal balanceOf(String accountNumber) {
        return jdbcTemplate.queryForObject("SELECT balance FROM account WHERE account_number = ?", BigDecimal.class,
                accountNumber);
    }
}
