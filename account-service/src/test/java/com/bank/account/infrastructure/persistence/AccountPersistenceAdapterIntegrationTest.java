package com.bank.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.bank.account.TestcontainersConfiguration;
import com.bank.account.application.exception.CustomerNotAvailableException;
import com.bank.account.application.exception.DuplicateAccountNumberException;
import com.bank.account.application.port.out.PageResult;
import com.bank.account.application.usecase.UpdateAccountUseCase;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountChanges;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerSnapshot;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountPersistenceAdapterIntegrationTest {

    private static final Instant EVENT_TIME = Instant.parse("2026-10-23T10:00:00Z");

    @Autowired
    private AccountPersistenceAdapter accountPersistenceAdapter;

    @Autowired
    private CustomerSnapshotPersistenceAdapter customerSnapshotPersistenceAdapter;

    @Autowired
    private ProcessedEventPersistenceAdapter processedEventPersistenceAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UpdateAccountUseCase updateAccountUseCase;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE account, customer_snapshot, processed_event CASCADE");
        customerId = UUID.randomUUID();
        customerSnapshotPersistenceAdapter.save(new CustomerSnapshot(customerId, "Jose Lema", true, EVENT_TIME));
    }

    @Test
    void snapshotSaveInsertsAndThenReplaces() {
        customerSnapshotPersistenceAdapter.save(
                new CustomerSnapshot(customerId, "Jose A. Lema", false, EVENT_TIME.plusSeconds(5)));

        assertThat(customerSnapshotPersistenceAdapter.findById(customerId))
                .contains(new CustomerSnapshot(customerId, "Jose A. Lema", false, EVENT_TIME.plusSeconds(5)));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM customer_snapshot", Integer.class)).isEqualTo(1);
    }

    @Test
    void snapshotCanBeReadWithASharedLockForAccountOpening() {
        CustomerSnapshot snapshot = transactionTemplate.execute(status ->
                customerSnapshotPersistenceAdapter.findWithSharedLock(customerId).orElseThrow());

        assertThat(snapshot.name()).isEqualTo("Jose Lema");
    }

    @Test
    void savedAccountKeepsMoneyExactlyAndGetsTimestampsFromTheDatabase() {
        Account saved = accountPersistenceAdapter.save(
                Account.open("478758", AccountType.SAVINGS, new BigDecimal("2000.10"), customerId, true));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(accountPersistenceAdapter.findByAccountNumber("478758")).get()
                .satisfies(account -> {
                    assertThat(account.getInitialBalance()).isEqualByComparingTo("2000.10");
                    assertThat(account.getBalance()).isEqualByComparingTo("2000.10");
                    assertThat(account.getCustomerId()).isEqualTo(customerId);
                });
    }

    @Test
    void duplicatedAccountNumberIsTranslatedToABusinessException() {
        accountPersistenceAdapter.save(Account.open("478758", AccountType.SAVINGS, BigDecimal.TEN, customerId, true));

        assertThatThrownBy(() -> accountPersistenceAdapter.save(
                Account.open("478758", AccountType.CHECKING, BigDecimal.ONE, customerId, true)))
                .isInstanceOf(DuplicateAccountNumberException.class);
    }

    @Test
    void accountForACustomerUnknownToTheProjectionIsRejectedByTheForeignKey() {
        assertThatThrownBy(() -> accountPersistenceAdapter.save(
                Account.open("478758", AccountType.SAVINGS, BigDecimal.TEN, UUID.randomUUID(), true)))
                .isInstanceOf(CustomerNotAvailableException.class);
    }

    @Test
    void findAllFiltersByCustomerAndOrdersByAccountNumber() {
        UUID otherCustomerId = UUID.randomUUID();
        customerSnapshotPersistenceAdapter.save(new CustomerSnapshot(otherCustomerId, "Juan Osorio", true, EVENT_TIME));
        accountPersistenceAdapter.save(Account.open("585545", AccountType.CHECKING, BigDecimal.TEN, customerId, true));
        accountPersistenceAdapter.save(Account.open("478758", AccountType.SAVINGS, BigDecimal.TEN, customerId, true));
        accountPersistenceAdapter.save(Account.open("495878", AccountType.SAVINGS, BigDecimal.ZERO, otherCustomerId, true));

        PageResult<Account> ofCustomer = accountPersistenceAdapter.findAll(customerId, 0, 10);
        PageResult<Account> all = accountPersistenceAdapter.findAll(null, 0, 2);

        assertThat(ofCustomer.content()).extracting(Account::getAccountNumber).containsExactly("478758", "585545");
        assertThat(all.content()).extracting(Account::getAccountNumber).containsExactly("478758", "495878");
        assertThat(all.totalElements()).isEqualTo(3);
        assertThat(all.totalPages()).isEqualTo(2);
    }

    @Test
    void deactivateAllOfCustomerTouchesOnlyItsActiveAccounts() {
        UUID otherCustomerId = UUID.randomUUID();
        customerSnapshotPersistenceAdapter.save(new CustomerSnapshot(otherCustomerId, "Juan Osorio", true, EVENT_TIME));
        accountPersistenceAdapter.save(Account.open("478758", AccountType.SAVINGS, BigDecimal.TEN, customerId, true));
        accountPersistenceAdapter.save(Account.open("585545", AccountType.CHECKING, BigDecimal.TEN, customerId, false));
        accountPersistenceAdapter.save(Account.open("495878", AccountType.SAVINGS, BigDecimal.TEN, otherCustomerId, true));

        Integer deactivated = transactionTemplate.execute(status ->
                accountPersistenceAdapter.deactivateAllOfCustomer(customerId));

        assertThat(deactivated).isEqualTo(1);
        assertThat(accountPersistenceAdapter.findByAccountNumber("478758")).get()
                .satisfies(account -> {
                    assertThat(account.isActive()).isFalse();
                    assertThat(account.getUpdatedAt()).isAfter(account.getCreatedAt());
                });
        assertThat(accountPersistenceAdapter.findByAccountNumber("495878")).get()
                .extracting(Account::isActive).isEqualTo(true);
    }

    @Test
    void accountRowStaysLockedWhileItIsBeingUpdated() {
        accountPersistenceAdapter.save(Account.open("478758", AccountType.SAVINGS, BigDecimal.TEN, customerId, true));
        CustomerSnapshot owner = customerSnapshotPersistenceAdapter.findById(customerId).orElseThrow();

        transactionTemplate.executeWithoutResult(status -> {
            Account locked = accountPersistenceAdapter.findForUpdate("478758").orElseThrow();

            assertThatThrownBy(() -> changeInAnotherTransaction("478758"))
                    .rootCause().hasMessageContaining("canceling statement due to lock timeout");

            locked.apply(new AccountChanges(AccountType.CHECKING, null), owner);
            accountPersistenceAdapter.save(locked);
        });

        assertThat(accountPersistenceAdapter.findByAccountNumber("478758")).get()
                .satisfies(account -> {
                    assertThat(account.getAccountType()).isEqualTo(AccountType.CHECKING);
                    assertThat(account.isActive()).isTrue();
                });
    }

    @Test
    void updatingAnAccountHoldsTheOwnerSnapshotUntilCommit() {
        accountPersistenceAdapter.save(Account.open("478758", AccountType.SAVINGS, BigDecimal.TEN, customerId, false));

        transactionTemplate.executeWithoutResult(status -> {
            updateAccountUseCase.update("478758", new AccountChanges(null, true));

            assertThatThrownBy(() -> runInAnotherTransaction(
                    "UPDATE customer_snapshot SET active = false WHERE customer_id = ?", customerId))
                    .rootCause().hasMessageContaining("canceling statement due to lock timeout");
        });

        assertThat(accountPersistenceAdapter.findByAccountNumber("478758")).get()
                .extracting(Account::isActive).isEqualTo(true);
    }

    @Test
    void eventIsMarkedAsProcessedOnlyOnce() {
        UUID eventId = UUID.randomUUID();

        Boolean first = transactionTemplate.execute(status -> processedEventPersistenceAdapter.markAsProcessed(eventId));
        Boolean second = transactionTemplate.execute(status -> processedEventPersistenceAdapter.markAsProcessed(eventId));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    private void changeInAnotherTransaction(String accountNumber) {
        runInAnotherTransaction("UPDATE account SET active = false WHERE account_number = ?", accountNumber);
    }

    private void runInAnotherTransaction(String sql, Object parameter) {
        CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("SET LOCAL lock_timeout = '500ms'");
            jdbcTemplate.update(sql, parameter);
        })).join();
    }
}
