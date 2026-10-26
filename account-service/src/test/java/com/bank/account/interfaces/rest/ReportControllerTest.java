package com.bank.account.interfaces.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bank.account.application.exception.CustomerNotFoundException;
import com.bank.account.application.exception.InvalidReportPeriodException;
import com.bank.account.application.report.AccountStatement;
import com.bank.account.application.report.AccountStatementAccount;
import com.bank.account.application.report.ReportPeriod;
import com.bank.account.application.usecase.GenerateAccountStatementUseCase;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.Movement;

@WebMvcTest(ReportController.class)
@Import(ReportRestMapperImpl.class)
class ReportControllerTest {

    private static final String REPORTS_URL = "/api/v1/reports";
    private static final UUID CUSTOMER_ID = UUID.fromString("3f6c2a0e-8f1d-4c55-9a51-0d1f3e7b9a10");
    private static final LocalDate DAY = LocalDate.parse("2026-10-23");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateAccountStatementUseCase generateAccountStatementUseCase;

    @Test
    void statementIsReturnedGroupedByAccount() throws Exception {
        when(generateAccountStatementUseCase.generate(CUSTOMER_ID, DAY, DAY)).thenReturn(statement());

        mockMvc.perform(get(REPORTS_URL).param("customerId", CUSTOMER_ID.toString())
                        .param("startDate", "2026-10-23").param("endDate", "2026-10-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.customer.name").value("Marianela Montalvo"))
                .andExpect(jsonPath("$.startDate").value("2026-10-23"))
                .andExpect(jsonPath("$.endDate").value("2026-10-23"))
                .andExpect(jsonPath("$.timeZone").value("America/Guayaquil"))
                .andExpect(jsonPath("$.accounts[0].accountNumber").value("225487"))
                .andExpect(jsonPath("$.accounts[0].accountType").value("CHECKING"))
                .andExpect(jsonPath("$.accounts[0].movements[0].movementType").value("DEPOSIT"))
                .andExpect(content().string(containsString("\"openingBalance\":100.00")))
                .andExpect(content().string(containsString("\"closingBalance\":700.00")))
                .andExpect(content().string(containsString("\"totalWithdrawals\":0.00")));
    }

    @Test
    void missingParameterReturns400NamingIt() throws Exception {
        mockMvc.perform(get(REPORTS_URL).param("customerId", CUSTOMER_ID.toString()).param("startDate", "2026-10-23"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("endDate"))
                .andExpect(jsonPath("$.errors[0].message").value("is required"));

        verifyNoInteractions(generateAccountStatementUseCase);
    }

    @Test
    void invalidDateFormatReturns400() throws Exception {
        mockMvc.perform(get(REPORTS_URL).param("customerId", CUSTOMER_ID.toString())
                        .param("startDate", "23/10/2026").param("endDate", "2026-10-23"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startDate"));
    }

    @Test
    void invalidPeriodReturns400() throws Exception {
        when(generateAccountStatementUseCase.generate(CUSTOMER_ID, DAY, DAY.minusDays(1)))
                .thenThrow(new InvalidReportPeriodException("endDate", "must not be before startDate"));

        mockMvc.perform(get(REPORTS_URL).param("customerId", CUSTOMER_ID.toString())
                        .param("startDate", "2026-10-23").param("endDate", "2026-10-22"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("endDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be before startDate"));
    }

    @Test
    void extremeDateIsParsedAndPassedToTheUseCase() throws Exception {
        LocalDate farFuture = LocalDate.parse("+999999999-12-31");
        when(generateAccountStatementUseCase.generate(CUSTOMER_ID, farFuture, farFuture))
                .thenThrow(new InvalidReportPeriodException("startDate", "must be between 1900-01-01 and 9999-12-31"));

        mockMvc.perform(get(REPORTS_URL).param("customerId", CUSTOMER_ID.toString())
                        .param("startDate", "+999999999-12-31").param("endDate", "+999999999-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startDate"));
    }

    @Test
    void unknownCustomerReturns404() throws Exception {
        when(generateAccountStatementUseCase.generate(CUSTOMER_ID, DAY, DAY))
                .thenThrow(new CustomerNotFoundException(CUSTOMER_ID));

        mockMvc.perform(get(REPORTS_URL).param("customerId", CUSTOMER_ID.toString())
                        .param("startDate", "2026-10-23").param("endDate", "2026-10-23"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer not found"));
    }

    private static AccountStatement statement() {
        Account account = Account.open("225487", AccountType.CHECKING, new BigDecimal("100"), CUSTOMER_ID, true);
        Movement deposit = account.registerMovement(new BigDecimal("600"));
        CustomerSnapshot customer = new CustomerSnapshot(CUSTOMER_ID, "Marianela Montalvo", true, Instant.now());
        ReportPeriod period = new ReportPeriod(DAY, DAY, ZoneId.of("America/Guayaquil"));
        return new AccountStatement(customer, period,
                List.of(AccountStatementAccount.from(account, new BigDecimal("100.00"), List.of(deposit))));
    }
}
