package com.bank.customer.interfaces.rest;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.bank.customer.application.exception.ConcurrentCustomerChangeException;
import com.bank.customer.application.exception.CustomerNotFoundException;
import com.bank.customer.application.exception.DuplicateIdentificationException;
import com.bank.customer.application.port.out.PageResult;
import com.bank.customer.application.usecase.CreateCustomerUseCase;
import com.bank.customer.application.usecase.DeleteCustomerUseCase;
import com.bank.customer.application.usecase.GetCustomerUseCase;
import com.bank.customer.application.usecase.ListCustomersUseCase;
import com.bank.customer.application.usecase.UpdateCustomerUseCase;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.domain.model.CustomerChanges;
import com.bank.customer.domain.model.CustomerDetails;
import com.bank.customer.domain.model.Gender;

@WebMvcTest(CustomerController.class)
@Import(CustomerRestMapperImpl.class)
class CustomerControllerTest {

    private static final String CUSTOMERS_URL = "/api/v1/customers";
    private static final UUID CUSTOMER_ID = UUID.fromString("3f6c2a0e-8f1d-4c55-9a51-0d1f3e7b9a10");
    private static final String VALID_BODY = """
            {
              "name": "Jose Lema",
              "gender": "MALE",
              "age": 35,
              "identification": "1712345678",
              "address": "Otavalo sn y principal",
              "phone": "098254785",
              "password": "1234"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCustomerUseCase createCustomerUseCase;

    @MockitoBean
    private GetCustomerUseCase getCustomerUseCase;

    @MockitoBean
    private ListCustomersUseCase listCustomersUseCase;

    @MockitoBean
    private UpdateCustomerUseCase updateCustomerUseCase;

    @MockitoBean
    private DeleteCustomerUseCase deleteCustomerUseCase;

    @Test
    void createReturns201WithLocationAndWithoutPassword() throws Exception {
        when(createCustomerUseCase.create(any(CustomerDetails.class), eq("1234"), eq(true))).thenReturn(storedCustomer());

        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(CUSTOMERS_URL + "/" + CUSTOMER_ID)))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Jose Lema"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").value("2026-10-22T21:00:00Z"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void createWithActiveFalseCreatesAnInactiveCustomer() throws Exception {
        when(createCustomerUseCase.create(any(CustomerDetails.class), eq("1234"), eq(false))).thenReturn(storedCustomer());
        String body = VALID_BODY.replace("\"password\": \"1234\"", "\"password\": \"1234\", \"active\": false");

        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(createCustomerUseCase).create(any(CustomerDetails.class), eq("1234"), eq(false));
    }

    @Test
    void createWithInvalidFieldsReturns400WithFieldErrors() throws Exception {
        String body = """
                {
                  "name": "",
                  "gender": "MALE",
                  "age": 17,
                  "identification": "1712345678",
                  "address": "Otavalo sn y principal",
                  "phone": "abc",
                  "password": "123"
                }
                """;

        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("age")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("phone")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("must be between 4 and 72 bytes")));

        verifyNoInteractions(createCustomerUseCase);
    }

    @Test
    void passwordLongerThan72BytesIsRejected() throws Exception {
        String body = VALID_BODY.replace("\"1234\"", "\"" + "ñ".repeat(37) + "\"");

        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void unknownFieldReturns400NamingTheField() throws Exception {
        String body = VALID_BODY.replace("\"password\": \"1234\"", "\"password\": \"1234\", \"role\": \"ADMIN\"");

        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("role"))
                .andExpect(jsonPath("$.errors[0].message").value("is not a known field"));
    }

    @Test
    void invalidGenderReturns400NamingTheField() throws Exception {
        String body = VALID_BODY.replace("\"MALE\"", "\"UNKNOWN\"");

        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("gender"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed JSON request"));
    }

    @Test
    void duplicatedIdentificationReturns409() throws Exception {
        when(createCustomerUseCase.create(any(CustomerDetails.class), eq("1234"), anyBoolean()))
                .thenThrow(new DuplicateIdentificationException());

        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("A customer with this identification already exists"));
    }

    @Test
    void getReturnsTheCustomer() throws Exception {
        when(getCustomerUseCase.get(CUSTOMER_ID)).thenReturn(storedCustomer());

        mockMvc.perform(get(CUSTOMERS_URL + "/" + CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identification").value("1712345678"));
    }

    @Test
    void getUnknownCustomerReturns404() throws Exception {
        when(getCustomerUseCase.get(CUSTOMER_ID)).thenThrow(new CustomerNotFoundException(CUSTOMER_ID));

        mockMvc.perform(get(CUSTOMERS_URL + "/" + CUSTOMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer not found"))
                .andExpect(jsonPath("$.instance").value(CUSTOMERS_URL + "/" + CUSTOMER_ID));
    }

    @Test
    void invalidCustomerIdReturns400WithoutEchoingTheValue() throws Exception {
        mockMvc.perform(get(CUSTOMERS_URL + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'customerId'"));

        verifyNoInteractions(getCustomerUseCase);
    }

    @Test
    void listReturnsAPage() throws Exception {
        when(listCustomersUseCase.list(0, 20)).thenReturn(new PageResult<>(List.of(storedCustomer()), 0, 20, 1, 1));

        mockMvc.perform(get(CUSTOMERS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Jose Lema"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listWithSizeAboveTheMaximumReturns400() throws Exception {
        mockMvc.perform(get(CUSTOMERS_URL).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));

        verifyNoInteractions(listCustomersUseCase);
    }

    @Test
    void putReplacesTheData() throws Exception {
        when(updateCustomerUseCase.update(eq(CUSTOMER_ID), any(CustomerChanges.class), isNull()))
                .thenReturn(storedCustomer());
        String body = VALID_BODY.replace("\"password\": \"1234\"", "\"active\": true");

        mockMvc.perform(put(CUSTOMERS_URL + "/" + CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()));

        verify(updateCustomerUseCase).update(CUSTOMER_ID,
                new CustomerChanges("Jose Lema", Gender.MALE, 35, "1712345678", "Otavalo sn y principal", "098254785", true),
                null);
    }

    @Test
    void putWithoutActiveReturns400() throws Exception {
        String body = VALID_BODY.replace(",\n  \"password\": \"1234\"", "");

        mockMvc.perform(put(CUSTOMERS_URL + "/" + CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("active"));
    }

    @Test
    void patchChangesOnlyTheSentFields() throws Exception {
        when(updateCustomerUseCase.update(eq(CUSTOMER_ID), any(CustomerChanges.class), isNull()))
                .thenReturn(storedCustomer());

        mockMvc.perform(patch(CUSTOMERS_URL + "/" + CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk());

        verify(updateCustomerUseCase).update(CUSTOMER_ID,
                new CustomerChanges(null, null, null, null, null, null, false), null);
    }

    @Test
    void patchWithEmptyBodyReturns400() throws Exception {
        mockMvc.perform(patch(CUSTOMERS_URL + "/" + CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("at least one field must be provided"));

        verifyNoInteractions(updateCustomerUseCase);
    }

    @Test
    void patchOfUnknownCustomerReturns404() throws Exception {
        when(updateCustomerUseCase.update(eq(CUSTOMER_ID), any(CustomerChanges.class), eq("5678")))
                .thenThrow(new CustomerNotFoundException(CUSTOMER_ID));

        mockMvc.perform(patch(CUSTOMERS_URL + "/" + CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"5678\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void concurrentChangeReturns409() throws Exception {
        when(updateCustomerUseCase.update(eq(CUSTOMER_ID), any(CustomerChanges.class), isNull()))
                .thenThrow(new ConcurrentCustomerChangeException());

        mockMvc.perform(patch(CUSTOMERS_URL + "/" + CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\": 40}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The customer was changed or deleted by another request, please retry"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete(CUSTOMERS_URL + "/" + CUSTOMER_ID))
                .andExpect(status().isNoContent());

        verify(deleteCustomerUseCase).delete(CUSTOMER_ID);
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        when(getCustomerUseCase.get(CUSTOMER_ID)).thenThrow(new IllegalStateException("database password is wrong"));

        mockMvc.perform(get(CUSTOMERS_URL + "/" + CUSTOMER_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    private static Customer storedCustomer() {
        return Customer.builder()
                .id(1L)
                .customerId(CUSTOMER_ID)
                .name("Jose Lema")
                .gender(Gender.MALE)
                .age(35)
                .identification("1712345678")
                .address("Otavalo sn y principal")
                .phone("098254785")
                .passwordHash("$2a$10$hash")
                .active(true)
                .createdAt(Instant.parse("2026-10-22T21:00:00Z"))
                .updatedAt(Instant.parse("2026-10-22T21:00:00Z"))
                .build();
    }
}
