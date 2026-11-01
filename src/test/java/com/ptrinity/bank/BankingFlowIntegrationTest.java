package com.ptrinity.bank;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo: API REST + servicios + persistencia real (H2 en modo PostgreSQL).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BankingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private long createCustomer(String id, String email) throws Exception {
        String body = """
                {"identificationType":"CC","identificationNumber":"%s","firstName":"Laura",
                 "lastName":"Gómez","email":"%s","birthDate":"1991-04-10"}
                """.formatted(id, email);
        String response = mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String openAccount(long customerId, String type) throws Exception {
        String response = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":%d,\"accountType\":\"%s\",\"gmfExempt\":true}"
                                .formatted(customerId, type)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accountNumber");
    }

    private long accountId(String number) throws Exception {
        String response = mockMvc.perform(get("/api/accounts")).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.<java.util.List<Object>>read(response,
                "$[?(@.accountNumber == '" + number + "')].id").get(0)).longValue();
    }

    @Test
    void fullBankingFlow() throws Exception {
        long customerId = createCustomer("5551234", "laura.flow@mail.com");
        String savings = openAccount(customerId, "SAVINGS");
        String checking = openAccount(customerId, "CHECKING");

        // Consignación en la cuenta de ahorros
        mockMvc.perform(post("/api/transactions/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"%s\",\"amount\":500000}".formatted(savings)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(500000.00));

        // La cuenta de ahorros no puede quedar en negativo
        mockMvc.perform(post("/api/transactions/withdrawals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"%s\",\"amount\":600000}".formatted(savings)))
                .andExpect(status().isUnprocessableContent());

        // Transferencia: débito en origen y crédito en destino
        mockMvc.perform(post("/api/transactions/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountNumber":"%s","destinationAccountNumber":"%s","amount":200000}
                                """.formatted(savings, checking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].balanceAfter").value(300000.00))
                .andExpect(jsonPath("$[1].balanceAfter").value(200000.00));

        long savingsId = accountId(savings);
        mockMvc.perform(get("/api/accounts/{id}/statement", savingsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.balance").value(300000.00))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.totalCredits").value(500000.00))
                .andExpect(jsonPath("$.totalDebits").value(200000.00));

        // No se puede cancelar con saldo, ni eliminar el cliente con productos
        mockMvc.perform(patch("/api/accounts/{id}/status", savingsId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(delete("/api/customers/{id}", customerId))
                .andExpect(status().isUnprocessableContent());

        // Una cuenta inactiva no admite movimientos
        mockMvc.perform(patch("/api/accounts/{id}/status", savingsId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/transactions/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"%s\",\"amount\":1}".formatted(savings)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void minorCustomerIsRejected() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identificationType":"CC","identificationNumber":"777888","firstName":"Mateo",
                         "lastName":"Ruiz","email":"mateo@mail.com","birthDate":"2015-01-01"}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("El cliente debe ser mayor de edad"));
    }

    @Test
    void customerWithoutProductsCanBeDeletedAndUpdated() throws Exception {
        long id = createCustomer("9990001", "temp@mail.com");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"identificationType":"CC","identificationNumber":"9990001","firstName":"Laura",
                                 "lastName":"Gómez Ruiz","email":"temp@mail.com","birthDate":"1991-04-10"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/customers/{id}", id)).andExpect(status().isNotFound());
    }
}
