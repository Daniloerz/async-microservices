package com.bank.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.account.application.exception.CustomerNotFoundException;
import com.bank.account.application.exception.InvalidReportPeriodException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.CustomerSnapshotRepository;
import com.bank.account.application.port.out.MovementRepository;
import com.bank.account.application.report.AccountStatement;
import com.bank.account.application.report.AccountStatementAccount;
import com.bank.account.application.report.ReportSettings;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.Movement;

@ExtendWith(MockitoExtension.class)
class GenerateAccountStatementUseCaseTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.parse("2026-10-23");
    private static final ZoneId GUAYAQUIL = ZoneId.of("America/Guayaquil");
    private static final Instant FROM = Instant.parse("2026-10-23T05:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-24T05:00:00Z");

    @Mock
    private CustomerSnapshotRepository customerSnapshotRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MovementRepository movementRepository;

    private GenerateAccountStatementUseCase generateAccountStatementUseCase;

    @BeforeEach
    void setUp() {
        generateAccountStatementUseCase = new GenerateAccountStatementUseCase(customerSnapshotRepository,
                accountRepository, movementRepository, new ReportSettings(GUAYAQUIL, 366));
    }

    @Test
    void statementGroupsMovementsByAccountWithOpeningBalances() {
        Account checking = Account.open("225487", AccountType.CHECKING, new BigDecimal("100"), CUSTOMER_ID, true);
        Account savings = Account.open("496825", AccountType.SAVINGS, new BigDecimal("540"), CUSTOMER_ID, true);
        Movement earlier = checking.registerMovement(new BigDecimal("50"));
        Movement deposit = checking.registerMovement(new BigDecimal("600"));
        Movement withdrawal = savings.registerMovement(new BigDecimal("-540"));
        when(customerSnapshotRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer(true)));
        when(accountRepository.findAllOfCustomerOpenedBefore(CUSTOMER_ID, TO)).thenReturn(List.of(checking, savings));
        when(movementRepository.findOfAccountsBetween(List.of("225487", "496825"), FROM, TO))
                .thenReturn(List.of(deposit, withdrawal));
        when(movementRepository.findLastBefore("225487", FROM)).thenReturn(Optional.of(earlier));
        when(movementRepository.findLastBefore("496825", FROM)).thenReturn(Optional.empty());

        AccountStatement statement = generateAccountStatementUseCase.generate(CUSTOMER_ID, DAY, DAY);

        assertThat(statement.customer().name()).isEqualTo("Marianela Montalvo");
        assertThat(statement.period().zone()).isEqualTo(GUAYAQUIL);
        assertThat(statement.accounts()).extracting(AccountStatementAccount::accountNumber)
                .containsExactly("225487", "496825");
        AccountStatementAccount checkingLine = statement.accounts().getFirst();
        assertThat(checkingLine.openingBalance()).isEqualByComparingTo("150");
        assertThat(checkingLine.closingBalance()).isEqualByComparingTo("750");
        assertThat(checkingLine.movements()).containsExactly(deposit);
        AccountStatementAccount savingsLine = statement.accounts().get(1);
        assertThat(savingsLine.openingBalance()).isEqualByComparingTo("540");
        assertThat(savingsLine.closingBalance()).isEqualByComparingTo("0");
        assertThat(savingsLine.totalWithdrawals()).isEqualByComparingTo("540");
    }

    @Test
    void customerWithoutAccountsGetsAnEmptyStatementWithoutQueryingMovements() {
        when(customerSnapshotRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer(false)));
        when(accountRepository.findAllOfCustomerOpenedBefore(CUSTOMER_ID, TO)).thenReturn(List.of());

        AccountStatement statement = generateAccountStatementUseCase.generate(CUSTOMER_ID, DAY, DAY);

        assertThat(statement.accounts()).isEmpty();
        assertThat(statement.customer().active()).isFalse();
        verifyNoInteractions(movementRepository);
    }

    @Test
    void unknownCustomerIsReportedAsNotFound() {
        when(customerSnapshotRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> generateAccountStatementUseCase.generate(CUSTOMER_ID, DAY, DAY))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found");
    }

    @Test
    void invalidPeriodIsRejectedBeforeReadingData() {
        assertThatThrownBy(() -> generateAccountStatementUseCase.generate(CUSTOMER_ID, DAY, DAY.minusDays(1)))
                .isInstanceOf(InvalidReportPeriodException.class);

        verifyNoInteractions(customerSnapshotRepository, accountRepository, movementRepository);
    }

    private static CustomerSnapshot customer(boolean active) {
        return new CustomerSnapshot(CUSTOMER_ID, "Marianela Montalvo", active, Instant.parse("2026-10-22T10:00:00Z"));
    }
}
