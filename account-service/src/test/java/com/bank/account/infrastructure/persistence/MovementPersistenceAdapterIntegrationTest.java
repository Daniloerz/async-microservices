package com.bank.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bank.account.TestcontainersConfiguration;
import com.bank.account.application.port.out.PageResult;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.Movement;
import com.bank.account.domain.model.MovementType;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MovementPersistenceAdapterIntegrationTest {

    @Autowired
    private MovementPersistenceAdapter movementPersistenceAdapter;

    @Autowired
    private AccountPersistenceAdapter accountPersistenceAdapter;

    @Autowired
    private CustomerSnapshotPersistenceAdapter customerSnapshotPersistenceAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Account account;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE movement, account, customer_snapshot, processed_event CASCADE");
        UUID customerId = UUID.randomUUID();
        customerSnapshotPersistenceAdapter.save(new CustomerSnapshot(customerId, "Marianela Montalvo", true, Instant.now()));
        account = accountPersistenceAdapter.save(
                Account.open("225487", AccountType.CHECKING, new BigDecimal("100"), customerId, true));
    }

    @Test
    void savedMovementGetsItsDateFromTheDatabase() {
        Movement saved = movementPersistenceAdapter.save(account.registerMovement(new BigDecimal("600")));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMovementDate()).isNotNull();
        assertThat(movementPersistenceAdapter.findByMovementId(saved.getMovementId())).get()
                .satisfies(movement -> {
                    assertThat(movement.getMovementType()).isEqualTo(MovementType.DEPOSIT);
                    assertThat(movement.getAmount()).isEqualTo(new BigDecimal("600.00"));
                    assertThat(movement.getBalance()).isEqualTo(new BigDecimal("700.00"));
                });
    }

    @Test
    void lastMovementAndListingFollowTheRegistrationOrder() {
        Movement first = movementPersistenceAdapter.save(account.registerMovement(new BigDecimal("600")));
        Movement second = movementPersistenceAdapter.save(account.registerMovement(new BigDecimal("-200")));
        Movement third = movementPersistenceAdapter.save(account.registerMovement(new BigDecimal("50")));

        assertThat(movementPersistenceAdapter.findLastOfAccount("225487")).get()
                .extracting(Movement::getMovementId).isEqualTo(third.getMovementId());

        PageResult<Movement> page = movementPersistenceAdapter.findAll("225487", 0, 10);
        assertThat(page.content()).extracting(Movement::getMovementId)
                .containsExactly(third.getMovementId(), second.getMovementId(), first.getMovementId());
        assertThat(page.content()).extracting(Movement::getMovementDate).isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat(movementPersistenceAdapter.findAll("999999", 0, 10).content()).isEmpty();
    }

    @Test
    void correctedMovementIsUpdatedInPlace() {
        Movement movement = movementPersistenceAdapter.save(account.registerMovement(new BigDecimal("600")));

        account.correctMovement(movement, new BigDecimal("-40"));
        movementPersistenceAdapter.save(movement);

        assertThat(movementPersistenceAdapter.findByMovementId(movement.getMovementId())).get()
                .satisfies(saved -> {
                    assertThat(saved.getMovementType()).isEqualTo(MovementType.WITHDRAWAL);
                    assertThat(saved.getBalance()).isEqualTo(new BigDecimal("60.00"));
                });
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM movement", Integer.class)).isEqualTo(1);
    }

    @Test
    void databaseRejectsANegativeAccountBalance() {
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE account SET balance = -1 WHERE account_number = '225487'"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_account_balance");
    }

    @Test
    void databaseRejectsATypeThatDoesNotMatchTheSign() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO movement (movement_id, account_number, movement_type, amount, balance)
                VALUES (gen_random_uuid(), '225487', 'DEPOSIT', -10, 90)
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_movement_type_sign");
    }

    @Test
    void databaseRejectsAMovementOfAnUnknownAccount() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO movement (movement_id, account_number, movement_type, amount, balance)
                VALUES (gen_random_uuid(), '999999', 'DEPOSIT', 10, 10)
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_movement_account");
    }
}
