package com.bank.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.infrastructure.persistence.CustomerSnapshotPersistenceAdapter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "app.kafka.customer-events.retry-interval=200ms")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReportApiIntegrationTest {

    private static final Instant ACCOUNTS_OPENED_AT = Instant.parse("2026-10-01T15:00:00Z");

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
        customerSnapshotPersistenceAdapter.save(new CustomerSnapshot(customerId, "Jose Lema", true, Instant.now()));

        insertAccount("111111", "100.00", "190.00", true, ACCOUNTS_OPENED_AT);
        insertMovement("111111", "DEPOSIT", "50.00", "150.00", "2026-10-10T04:00:00Z");
        insertMovement("111111", "WITHDRAWAL", "-20.00", "130.00", "2026-10-10T05:00:00Z");
        insertMovement("111111", "DEPOSIT", "70.00", "200.00", "2026-10-13T04:59:59Z");
        insertMovement("111111", "WITHDRAWAL", "-10.00", "190.00", "2026-10-13T05:00:00Z");
        insertAccount("222222", "0.00", "0.00", false, ACCOUNTS_OPENED_AT);
        insertAccount("333333", "500.00", "500.00", true, Instant.parse("2026-10-20T15:00:00Z"));
    }

    @Test
    void statementIncludesOnlyThePeriodMovementsInTheConfiguredTimeZone() throws Exception {
        DocumentContext report = report("2026-10-10", "2026-10-12");

        assertThat(report.read("$.customer.name", String.class)).isEqualTo("Jose Lema");
        assertThat(report.read("$.timeZone", String.class)).isEqualTo("America/Guayaquil");
        assertThat(report.read("$.accounts[*].accountNumber", List.class)).containsExactly("111111", "222222");

        Map<String, Object> account = report.read("$.accounts[0]");
        assertThat(decimal(account.get("initialBalance"))).isEqualByComparingTo("100");
        assertThat(decimal(account.get("openingBalance"))).isEqualByComparingTo("150");
        assertThat(decimal(account.get("closingBalance"))).isEqualByComparingTo("200");
        assertThat(decimal(account.get("availableBalance"))).isEqualByComparingTo("190");
        assertThat(decimal(account.get("totalDeposits"))).isEqualByComparingTo("70");
        assertThat(decimal(account.get("totalWithdrawals"))).isEqualByComparingTo("20");
        List<Object> balances = report.read("$.accounts[0].movements[*].balance");
        assertThat(balances.stream().map(ReportApiIntegrationTest::decimal).toList())
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("130"), new BigDecimal("200"));
        assertThat(report.read("$.accounts[0].movements[0].movementDate", String.class))
                .isEqualTo("2026-10-10T05:00:00Z");
    }

    @Test
    void openingPlusMovementsEqualsClosingForEveryAccount() throws Exception {
        DocumentContext report = report("2026-10-10", "2026-10-12");

        List<Map<String, Object>> accounts = report.read("$.accounts");
        assertThat(accounts).allSatisfy(account -> assertThat(
                decimal(account.get("openingBalance"))
                        .add(decimal(account.get("totalDeposits")))
                        .subtract(decimal(account.get("totalWithdrawals"))))
                .isEqualByComparingTo(decimal(account.get("closingBalance"))));
    }

    @Test
    void inactiveAccountWithoutMovementsIsListedAndLaterAccountsAppearInLaterPeriods() throws Exception {
        DocumentContext early = report("2026-10-10", "2026-10-12");
        assertThat(early.read("$.accounts[1].active", Boolean.class)).isFalse();
        assertThat(early.read("$.accounts[1].movements", List.class)).isEmpty();
        assertThat(decimal(early.read("$.accounts[1].closingBalance"))).isEqualByComparingTo("0");

        DocumentContext late = report("2026-10-01", "2026-10-31");
        assertThat(late.read("$.accounts[*].accountNumber", List.class)).containsExactly("111111", "222222", "333333");
        assertThat(late.read("$.accounts[0].movements", List.class)).hasSize(4);
    }

    @Test
    void deletedCustomerStillHasAStatementAndUnknownCustomerIsNotFound() throws Exception {
        customerSnapshotPersistenceAdapter.save(new CustomerSnapshot(customerId, "Jose Lema", false, Instant.now()));

        assertThat(report("2026-10-10", "2026-10-12").read("$.customer.active", Boolean.class)).isFalse();

        mockMvc.perform(get("/api/v1/reports").param("customerId", UUID.randomUUID().toString())
                        .param("startDate", "2026-10-10").param("endDate", "2026-10-12"))
                .andExpect(status().isNotFound());
    }

    @Test
    void periodUpToTheMaximumIsAcceptedAndLongerIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/reports").param("customerId", customerId.toString())
                        .param("startDate", "2025-01-01").param("endDate", "2026-01-01"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports").param("customerId", customerId.toString())
                        .param("startDate", "2025-01-01").param("endDate", "2026-01-02"))
                .andExpect(status().isBadRequest());
    }

    private DocumentContext report(String startDate, String endDate) throws Exception {
        String body = mockMvc.perform(get("/api/v1/reports").param("customerId", customerId.toString())
                        .param("startDate", startDate).param("endDate", endDate))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body);
    }

    private void insertAccount(String accountNumber, String initialBalance, String balance, boolean active,
            Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO account (account_number, account_type, initial_balance, balance, active, customer_id, created_at)
                VALUES (?, 'SAVINGS', ?, ?, ?, ?, ?)
                """, accountNumber, new BigDecimal(initialBalance), new BigDecimal(balance), active, customerId,
                Timestamp.from(createdAt));
    }

    private void insertMovement(String accountNumber, String type, String amount, String balance, String date) {
        jdbcTemplate.update("""
                INSERT INTO movement (movement_id, account_number, movement_type, amount, balance, movement_date)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?)
                """, accountNumber, type, new BigDecimal(amount), new BigDecimal(balance),
                Timestamp.from(Instant.parse(date)));
    }

    private static BigDecimal decimal(Object value) {
        return new BigDecimal(String.valueOf(value));
    }
}
