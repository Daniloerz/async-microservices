package com.bank.account.interfaces.rest;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bank.account.application.exception.AccountNotAvailableException;
import com.bank.account.application.exception.MovementNotCorrectableException;
import com.bank.account.application.exception.MovementNotFoundException;
import com.bank.account.application.port.out.PageResult;
import com.bank.account.application.usecase.CorrectMovementUseCase;
import com.bank.account.application.usecase.GetMovementUseCase;
import com.bank.account.application.usecase.ListMovementsUseCase;
import com.bank.account.application.usecase.RegisterMovementUseCase;
import com.bank.account.domain.exception.InactiveAccountException;
import com.bank.account.domain.exception.InsufficientBalanceException;
import com.bank.account.domain.model.Movement;
import com.bank.account.domain.model.MovementType;

@WebMvcTest(MovementController.class)
@Import(MovementRestMapperImpl.class)
class MovementControllerTest {

    private static final String MOVEMENTS_URL = "/api/v1/movements";
    private static final UUID MOVEMENT_ID = UUID.fromString("6b1f2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterMovementUseCase registerMovementUseCase;

    @MockitoBean
    private GetMovementUseCase getMovementUseCase;

    @MockitoBean
    private ListMovementsUseCase listMovementsUseCase;

    @MockitoBean
    private CorrectMovementUseCase correctMovementUseCase;

    @Test
    void registerReturns201WithLocationAndResultingBalance() throws Exception {
        when(registerMovementUseCase.register("478758", new BigDecimal("-575.00"))).thenReturn(storedMovement());

        mockMvc.perform(post(MOVEMENTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\": \"478758\", \"amount\": -575.00}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(MOVEMENTS_URL + "/" + MOVEMENT_ID)))
                .andExpect(jsonPath("$.movementId").value(MOVEMENT_ID.toString()))
                .andExpect(jsonPath("$.movementType").value("WITHDRAWAL"))
                .andExpect(content().string(Matchers.containsString("\"amount\":-575.00")))
                .andExpect(content().string(Matchers.containsString("\"balance\":1425.00")))
                .andExpect(jsonPath("$.movementDate").value("2026-10-23T22:10:00Z"));
    }

    @Test
    void insufficientBalanceReturns422WithTheChallengeMessage() throws Exception {
        when(registerMovementUseCase.register(eq("496825"), any())).thenThrow(new InsufficientBalanceException());

        mockMvc.perform(post(MOVEMENTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\": \"496825\", \"amount\": -541}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Unprocessable Content"))
                .andExpect(jsonPath("$.detail").value("Insufficient balance"));
    }

    @Test
    void unknownAccountReturns422() throws Exception {
        when(registerMovementUseCase.register(eq("999999"), any())).thenThrow(new AccountNotAvailableException());

        mockMvc.perform(post(MOVEMENTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\": \"999999\", \"amount\": 10}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Account does not exist"));
    }

    @Test
    void inactiveAccountReturns422() throws Exception {
        when(registerMovementUseCase.register(eq("478758"), any())).thenThrow(new InactiveAccountException());

        mockMvc.perform(post(MOVEMENTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\": \"478758\", \"amount\": 10}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Account is not active"));
    }

    @Test
    void zeroAmountAndTooManyDecimalsAreRejected() throws Exception {
        mockMvc.perform(post(MOVEMENTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\": \"478758\", \"amount\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be zero"));

        mockMvc.perform(post(MOVEMENTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\": \"478758\", \"amount\": 1.005}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));

        verifyNoInteractions(registerMovementUseCase);
    }

    @Test
    void missingFieldsAreRejected() throws Exception {
        mockMvc.perform(post(MOVEMENTS_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("accountNumber")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("amount")));
    }

    @Test
    void typeCannotBeSentByTheClient() throws Exception {
        mockMvc.perform(post(MOVEMENTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\": \"478758\", \"amount\": 10, \"movementType\": \"WITHDRAWAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("movementType"));
    }

    @Test
    void getUnknownMovementReturns404() throws Exception {
        when(getMovementUseCase.get(MOVEMENT_ID)).thenThrow(new MovementNotFoundException(MOVEMENT_ID));

        mockMvc.perform(get(MOVEMENTS_URL + "/" + MOVEMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Movement not found"));
    }

    @Test
    void listFiltersByAccount() throws Exception {
        when(listMovementsUseCase.list("478758", 0, 20))
                .thenReturn(new PageResult<>(List.of(storedMovement()), 0, 20, 1, 1));

        mockMvc.perform(get(MOVEMENTS_URL).param("accountNumber", "478758"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movementId").value(MOVEMENT_ID.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listWithInvalidAccountNumberReturns400() throws Exception {
        mockMvc.perform(get(MOVEMENTS_URL).param("accountNumber", "12AB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("accountNumber"));
    }

    @Test
    void correctingAnOlderMovementReturns409() throws Exception {
        when(correctMovementUseCase.correct(MOVEMENT_ID, new BigDecimal("-500.00")))
                .thenThrow(new MovementNotCorrectableException());

        mockMvc.perform(put(MOVEMENTS_URL + "/" + MOVEMENT_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -500.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Only the last movement of an account can be corrected"));
    }

    @Test
    void correctingTheLastMovementReturns200() throws Exception {
        when(correctMovementUseCase.correct(MOVEMENT_ID, new BigDecimal("-575.00"))).thenReturn(storedMovement());

        mockMvc.perform(put(MOVEMENTS_URL + "/" + MOVEMENT_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -575.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementType").value("WITHDRAWAL"));
    }

    @Test
    void patchAndDeleteAreNotSupported() throws Exception {
        mockMvc.perform(patch(MOVEMENTS_URL + "/" + MOVEMENT_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete(MOVEMENTS_URL + "/" + MOVEMENT_ID))
                .andExpect(status().isMethodNotAllowed());
    }

    private static Movement storedMovement() {
        return Movement.builder()
                .id(1L)
                .movementId(MOVEMENT_ID)
                .accountNumber("478758")
                .movementType(MovementType.WITHDRAWAL)
                .amount(new BigDecimal("-575"))
                .balance(new BigDecimal("1425"))
                .movementDate(Instant.parse("2026-10-23T22:10:00Z"))
                .build();
    }
}
