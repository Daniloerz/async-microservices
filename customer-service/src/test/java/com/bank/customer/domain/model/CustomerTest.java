package com.bank.customer.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final String PASSWORD_HASH = "$2a$10$hashedPasswordValue";

    @Test
    void createAssignsCustomerIdAndCopiesDetails() {
        Customer customer = Customer.create(joseLemaDetails(), PASSWORD_HASH, true);

        assertThat(customer.getCustomerId()).isNotNull();
        assertThat(customer.getId()).isNull();
        assertThat(customer.getName()).isEqualTo("Jose Lema");
        assertThat(customer.getGender()).isEqualTo(Gender.MALE);
        assertThat(customer.getAge()).isEqualTo(35);
        assertThat(customer.getIdentification()).isEqualTo("1712345678");
        assertThat(customer.getAddress()).isEqualTo("Otavalo sn y principal");
        assertThat(customer.getPhone()).isEqualTo("098254785");
        assertThat(customer.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(customer.isActive()).isTrue();
    }

    @Test
    void createGivesEachCustomerADifferentCustomerId() {
        Customer first = Customer.create(joseLemaDetails(), PASSWORD_HASH, true);
        Customer second = Customer.create(joseLemaDetails(), PASSWORD_HASH, true);

        assertThat(first.getCustomerId()).isNotEqualTo(second.getCustomerId());
    }

    @Test
    void createRejectsMissingPasswordHash() {
        assertThatNullPointerException()
                .isThrownBy(() -> Customer.create(joseLemaDetails(), null, true));
    }

    @Test
    void applyChangesOnlyTheProvidedFields() {
        Customer customer = Customer.create(joseLemaDetails(), PASSWORD_HASH, true);
        CustomerChanges changes = new CustomerChanges(null, null, 36, null, "Quito centro", null, false);

        customer.apply(changes);

        assertThat(customer.getAge()).isEqualTo(36);
        assertThat(customer.getAddress()).isEqualTo("Quito centro");
        assertThat(customer.isActive()).isFalse();
        assertThat(customer.getName()).isEqualTo("Jose Lema");
        assertThat(customer.getIdentification()).isEqualTo("1712345678");
        assertThat(customer.getPhone()).isEqualTo("098254785");
    }

    @Test
    void applyCanReactivateACustomer() {
        Customer customer = Customer.create(joseLemaDetails(), PASSWORD_HASH, false);

        customer.apply(new CustomerChanges(null, null, null, null, null, null, true));

        assertThat(customer.isActive()).isTrue();
    }

    @Test
    void changePasswordReplacesTheHash() {
        Customer customer = Customer.create(joseLemaDetails(), PASSWORD_HASH, true);

        customer.changePassword("$2a$10$anotherHash");

        assertThat(customer.getPasswordHash()).isEqualTo("$2a$10$anotherHash");
    }

    @Test
    void toStringNeverShowsThePasswordHash() {
        Customer customer = Customer.create(joseLemaDetails(), PASSWORD_HASH, true);

        assertThat(customer.toString())
                .contains("Jose Lema")
                .doesNotContain(PASSWORD_HASH);
    }

    @Test
    void emptyChangesAreDetected() {
        assertThat(new CustomerChanges(null, null, null, null, null, null, null).isEmpty()).isTrue();
        assertThat(new CustomerChanges(null, null, null, null, null, null, false).isEmpty()).isFalse();
    }

    private static CustomerDetails joseLemaDetails() {
        return new CustomerDetails("Jose Lema", Gender.MALE, 35, "1712345678", "Otavalo sn y principal", "098254785");
    }
}
