package com.bank.account.interfaces.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bank.account.application.exception.AccountNotFoundException;
import com.bank.account.application.exception.CustomerNotAvailableException;
import com.bank.account.application.exception.DuplicateAccountNumberException;
import com.bank.account.application.port.out.PageResult;
import com.bank.account.application.usecase.CreateAccountUseCase;
import com.bank.account.application.usecase.GetAccountUseCase;
import com.bank.account.application.usecase.ListAccountsUseCase;
import com.bank.account.application.usecase.UpdateAccountUseCase;
import com.bank.account.domain.exception.AccountActivationNotAllowedException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountChanges;
import com.bank.account.domain.model.AccountType;

@WebMvcTest(AccountController.class)
@Import(AccountRestMapperImpl.class)
class AccountControllerTest {

    private static final String ACCOUNTS_URL = "/api/v1/accounts";
    private static final UUID CUSTOMER_ID = UUID.fromString("3f6c2a0e-8f1d-4c55-9a51-0d1f3e7b9a10");
    private static final String VALID_BODY = """
            {
              "accountNumber": "478758",
              "accountType": "SAVINGS",
              "initialBalance": 2000.00,
              "customerId": "3f6c2a0e-8f1d-4c55-9a51-0d1f3e7b9a10"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateAccountUseCase createAccountUseCase;

    @MockitoBean
    private GetAccountUseCase getAccountUseCase;

    @MockitoBean
    private ListAccountsUseCase listAccountsUseCase;

    @MockitoBean
    private UpdateAccountUseCase updateAccountUseCase;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(createAccountUseCase.create("478758", AccountType.SAVINGS, new BigDecimal("2000.00"), CUSTOMER_ID, true))
                .thenReturn(storedAccount());

        mockMvc.perform(post(ACCOUNTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(ACCOUNTS_URL + "/478758")))
                .andExpect(jsonPath("$.accountNumber").value("478758"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(content().string(containsString("\"initialBalance\":2000.00")))
                .andExpect(jsonPath("$.balance").value(2000.00))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void invalidFieldsReturn400WithFieldErrors() throws Exception {
        String body = """
                {
                  "accountNumber": "12AB",
                  "accountType": "SAVINGS",
                  "initialBalance": -1.555
                }
                """;

        mockMvc.perform(post(ACCOUNTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("accountNumber")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("initialBalance")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("customerId")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("must be 6 to 20 digits")));

        verifyNoInteractions(createAccountUseCase);
    }

    @Test
    void invalidAccountTypeReturns400NamingTheField() throws Exception {
        mockMvc.perform(post(ACCOUNTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("SAVINGS", "GOLD")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("accountType"));
    }

    @Test
    void unknownCustomerReturns422() throws Exception {
        when(createAccountUseCase.create(any(), any(), any(), any(), eq(true)))
                .thenThrow(CustomerNotAvailableException.unknown());

        mockMvc.perform(post(ACCOUNTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Customer does not exist or is not yet known by this service"));
    }

    @Test
    void duplicatedAccountNumberReturns409() throws Exception {
        when(createAccountUseCase.create(any(), any(), any(), any(), eq(true)))
                .thenThrow(new DuplicateAccountNumberException());

        mockMvc.perform(post(ACCOUNTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account with this number already exists"));
    }

    @Test
    void getReturnsTheAccount() throws Exception {
        when(getAccountUseCase.get("478758")).thenReturn(storedAccount());

        mockMvc.perform(get(ACCOUNTS_URL + "/478758"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("478758"));
    }

    @Test
    void getUnknownAccountReturns404() throws Exception {
        when(getAccountUseCase.get("999999")).thenThrow(new AccountNotFoundException("999999"));

        mockMvc.perform(get(ACCOUNTS_URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    @Test
    void invalidAccountNumberInThePathReturns400() throws Exception {
        mockMvc.perform(get(ACCOUNTS_URL + "/12AB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("accountNumber"));

        verifyNoInteractions(getAccountUseCase);
    }

    @Test
    void listFiltersByCustomer() throws Exception {
        when(listAccountsUseCase.list(CUSTOMER_ID, 0, 20))
                .thenReturn(new PageResult<>(List.of(storedAccount()), 0, 20, 1, 1));

        mockMvc.perform(get(ACCOUNTS_URL).param("customerId", CUSTOMER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountNumber").value("478758"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listWithInvalidCustomerIdReturns400() throws Exception {
        mockMvc.perform(get(ACCOUNTS_URL).param("customerId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'customerId'"));
    }

    @Test
    void putReplacesTypeAndStatus() throws Exception {
        when(updateAccountUseCase.update("478758", new AccountChanges(AccountType.CHECKING, false)))
                .thenReturn(storedAccount());

        mockMvc.perform(put(ACCOUNTS_URL + "/478758").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountType\": \"CHECKING\", \"active\": false}"))
                .andExpect(status().isOk());

        verify(updateAccountUseCase).update("478758", new AccountChanges(AccountType.CHECKING, false));
    }

    @Test
    void putCannotChangeTheInitialBalance() throws Exception {
        mockMvc.perform(put(ACCOUNTS_URL + "/478758").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountType\": \"CHECKING\", \"active\": true, \"initialBalance\": 10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("initialBalance"))
                .andExpect(jsonPath("$.errors[0].message").value("is not a known field"));

        verifyNoInteractions(updateAccountUseCase);
    }

    @Test
    void patchWithEmptyBodyReturns400() throws Exception {
        mockMvc.perform(patch(ACCOUNTS_URL + "/478758").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("at least one field must be provided"));
    }

    @Test
    void activationWithInactiveCustomerReturns422() throws Exception {
        when(updateAccountUseCase.update("478758", new AccountChanges(null, true)))
                .thenThrow(new AccountActivationNotAllowedException());

        mockMvc.perform(patch(ACCOUNTS_URL + "/478758").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("An account cannot be activated while its customer is not active"));
    }

    private static Account storedAccount() {
        return Account.builder()
                .id(1L)
                .accountNumber("478758")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("2000.00"))
                .balance(new BigDecimal("2000.00"))
                .active(true)
                .customerId(CUSTOMER_ID)
                .createdAt(Instant.parse("2026-10-23T10:00:00Z"))
                .updatedAt(Instant.parse("2026-10-23T10:00:00Z"))
                .build();
    }
}
