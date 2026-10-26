package com.bank.customer.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.bank.customer.TestcontainersConfiguration;
import com.bank.customer.application.exception.ConcurrentCustomerChangeException;
import com.bank.customer.application.exception.DuplicateIdentificationException;
import com.bank.customer.application.port.out.PageResult;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.domain.model.CustomerChanges;
import com.bank.customer.domain.model.CustomerDetails;
import com.bank.customer.domain.model.Gender;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CustomerPersistenceAdapterIntegrationTest {

    @Autowired
    private CustomerPersistenceAdapter customerPersistenceAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE customer, person, outbox_event CASCADE");
    }

    @Test
    void saveAssignsIdAndTimestampsFromTheDatabase() {
        Customer saved = customerPersistenceAdapter.save(newCustomer("Jose Lema", "1712345678"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(customerPersistenceAdapter.findByCustomerId(saved.getCustomerId()))
                .get()
                .extracting(Customer::getIdentification, Customer::getPasswordHash)
                .containsExactly("1712345678", "$2a$10$hash");
    }

    @Test
    void changingOnlyCustomerColumnsStillRefreshesUpdatedAt() {
        Customer saved = customerPersistenceAdapter.save(newCustomer("Jose Lema", "1712345678"));
        Customer loaded = customerPersistenceAdapter.findByCustomerId(saved.getCustomerId()).orElseThrow();

        loaded.apply(new CustomerChanges(null, null, null, null, null, null, false));
        Customer updated = customerPersistenceAdapter.save(loaded);

        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfter(saved.getCreatedAt());
    }

    @Test
    void staleUpdateIsRejectedInsteadOfOverwritingAConcurrentChange() {
        Customer saved = customerPersistenceAdapter.save(newCustomer("Jose Lema", "1712345678"));

        transactionTemplate.executeWithoutResult(status -> {
            Customer loaded = customerPersistenceAdapter.findByCustomerId(saved.getCustomerId()).orElseThrow();
            changeInAnotherTransaction(saved.getId());

            loaded.apply(new CustomerChanges(null, null, 40, null, null, null, null));

            assertThatThrownBy(() -> customerPersistenceAdapter.save(loaded))
                    .isInstanceOf(ConcurrentCustomerChangeException.class);
            status.setRollbackOnly();
        });

        assertThat(jdbcTemplate.queryForObject("SELECT age FROM person WHERE id = ?", Integer.class, saved.getId()))
                .isEqualTo(30);
    }

    @Test
    void customerLoadedForUpdateCannotBeDeletedConcurrently() {
        Customer saved = customerPersistenceAdapter.save(newCustomer("Jose Lema", "1712345678"));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(customerPersistenceAdapter.findForUpdate(saved.getCustomerId())).isPresent();

            assertThatThrownBy(() -> CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(other -> {
                jdbcTemplate.execute("SET LOCAL lock_timeout = '500ms'");
                jdbcTemplate.update("DELETE FROM person WHERE id = ?", saved.getId());
            })).join())
                    .rootCause().hasMessageContaining("canceling statement due to lock timeout");
        });

        assertThat(customerPersistenceAdapter.findByCustomerId(saved.getCustomerId())).isPresent();
    }

    @Test
    void duplicatedIdentificationIsTranslatedToABusinessException() {
        customerPersistenceAdapter.save(newCustomer("Jose Lema", "1712345678"));

        assertThatThrownBy(() -> customerPersistenceAdapter.save(newCustomer("Other Person", "1712345678")))
                .isInstanceOf(DuplicateIdentificationException.class);
    }

    @Test
    void identificationChecksDistinguishTheSameCustomerFromOthers() {
        Customer saved = customerPersistenceAdapter.save(newCustomer("Jose Lema", "1712345678"));

        assertThat(customerPersistenceAdapter.existsByIdentification("1712345678")).isTrue();
        assertThat(customerPersistenceAdapter.existsByIdentificationForOtherCustomer("1712345678", saved.getCustomerId()))
                .isFalse();
        assertThat(customerPersistenceAdapter.existsByIdentificationForOtherCustomer("1712345678", UUID.randomUUID()))
                .isTrue();
    }

    @Test
    void findAllReturnsPagesOrderedByName() {
        customerPersistenceAdapter.save(newCustomer("Marianela Montalvo", "1700000002"));
        customerPersistenceAdapter.save(newCustomer("Jose Lema", "1700000001"));
        customerPersistenceAdapter.save(newCustomer("Juan Osorio", "1700000003"));

        PageResult<Customer> firstPage = customerPersistenceAdapter.findAll(0, 2);
        PageResult<Customer> secondPage = customerPersistenceAdapter.findAll(1, 2);

        assertThat(firstPage.content()).extracting(Customer::getName).containsExactly("Jose Lema", "Juan Osorio");
        assertThat(secondPage.content()).extracting(Customer::getName).containsExactly("Marianela Montalvo");
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
    }

    @Test
    void deleteRemovesPersonAndCustomerRows() {
        Customer saved = customerPersistenceAdapter.save(newCustomer("Jose Lema", "1712345678"));

        customerPersistenceAdapter.delete(saved);

        assertThat(customerPersistenceAdapter.findByCustomerId(saved.getCustomerId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM person", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM customer", Integer.class)).isZero();
    }

    private void changeInAnotherTransaction(Long personId) {
        CompletableFuture.runAsync(() ->
                jdbcTemplate.update("UPDATE person SET version = version + 1 WHERE id = ?", personId)).join();
    }

    private static Customer newCustomer(String name, String identification) {
        return Customer.create(
                new CustomerDetails(name, Gender.FEMALE, 30, identification, "Amazonas y NNUU", "097548965"),
                "$2a$10$hash", true);
    }
}
