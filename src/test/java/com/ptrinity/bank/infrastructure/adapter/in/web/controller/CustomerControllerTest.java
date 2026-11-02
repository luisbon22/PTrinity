package com.ptrinity.bank.infrastructure.adapter.in.web.controller;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import com.ptrinity.bank.domain.exception.DuplicateResourceException;
import com.ptrinity.bank.domain.exception.ResourceNotFoundException;
import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.CustomerData;
import com.ptrinity.bank.domain.model.IdentificationType;
import com.ptrinity.bank.domain.port.in.CustomerUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final String VALID_BODY = """
            {"identificationType":"CC","identificationNumber":"1020304050","firstName":"Ana",
             "lastName":"Pérez","email":"ana@mail.com","birthDate":"1990-05-20"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerUseCase customerUseCase;

    private Customer customer() {
        return new Customer(1L, IdentificationType.CC, "1020304050", "Ana", "Pérez", "ana@mail.com",
                LocalDate.of(1990, 5, 20), LocalDateTime.of(2026, 11, 1, 10, 0), null);
    }

    @Test
    void listsCustomers() throws Exception {
        when(customerUseCase.findAll()).thenReturn(List.of(customer()));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Ana Pérez"));
    }

    @Test
    void getsCustomerById() throws Exception {
        when(customerUseCase.findById(1L)).thenReturn(customer());

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void returns404WhenCustomerDoesNotExist() throws Exception {
        when(customerUseCase.findById(9L)).thenThrow(new ResourceNotFoundException("Cliente", 9L));

        mockMvc.perform(get("/api/customers/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente no encontrado: 9"));
    }

    @Test
    void createsCustomer() throws Exception {
        when(customerUseCase.create(any(CustomerData.class))).thenReturn(customer());

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/customers/1"))
                .andExpect(jsonPath("$.identificationNumber").value("1020304050"));
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        String invalid = """
                {"identificationType":"CC","identificationNumber":"1020","firstName":"A",
                 "lastName":"Pérez","email":"no-es-correo","birthDate":"1990-05-20"}
                """;

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
        verify(customerUseCase, never()).create(any());
    }

    @Test
    void rejectsUnknownIdentificationType() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"CC\"", "\"XX\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns422ForBusinessRuleViolation() throws Exception {
        when(customerUseCase.create(any())).thenThrow(new BusinessRuleException("El cliente debe ser mayor de edad"));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("El cliente debe ser mayor de edad"));
    }

    @Test
    void returns409ForDuplicates() throws Exception {
        when(customerUseCase.create(any())).thenThrow(new DuplicateResourceException("Duplicado"));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesCustomer() throws Exception {
        when(customerUseCase.update(eq(1L), any())).thenReturn(customer());

        mockMvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deletesCustomer() throws Exception {
        mockMvc.perform(delete("/api/customers/1")).andExpect(status().isNoContent());

        verify(customerUseCase).delete(1L);
    }

    @Test
    void cannotDeleteCustomerWithProducts() throws Exception {
        doThrow(new BusinessRuleException("No se puede eliminar el cliente porque tiene productos vinculados"))
                .when(customerUseCase).delete(1L);

        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(
                        "No se puede eliminar el cliente porque tiene productos vinculados"));
    }
}
