package com.ptrinity.bank.infrastructure.adapter.in.web.controller;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.model.AccountStatement;
import com.ptrinity.bank.domain.model.AccountStatus;
import com.ptrinity.bank.domain.model.AccountType;
import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.IdentificationType;
import com.ptrinity.bank.domain.model.MovementType;
import com.ptrinity.bank.domain.model.Transaction;
import com.ptrinity.bank.domain.model.TransactionType;
import com.ptrinity.bank.domain.port.in.AccountUseCase;
import com.ptrinity.bank.domain.port.in.OpenAccountCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountUseCase accountUseCase;

    private Account checking(AccountStatus status) {
        return new Account(5L, AccountType.CHECKING, "3300000001", status, new BigDecimal("200.00"),
                new BigDecimal("1000000.00"), false, 1L, NOW, null);
    }

    @Test
    void opensAccount() throws Exception {
        when(accountUseCase.open(any(OpenAccountCommand.class))).thenReturn(checking(AccountStatus.ACTIVE));

        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"accountType\":\"CHECKING\",\"gmfExempt\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("3300000001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.availableBalance").value(1000200.00));
        verify(accountUseCase).open(new OpenAccountCommand(1L, AccountType.CHECKING, false));
    }

    @Test
    void rejectsAccountWithoutType() throws Exception {
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("accountType"));
    }

    @Test
    void listsAccountsByCustomer() throws Exception {
        when(accountUseCase.findAll(1L)).thenReturn(List.of(checking(AccountStatus.ACTIVE)));

        mockMvc.perform(get("/api/accounts").param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getsAccount() throws Exception {
        when(accountUseCase.findById(5L)).thenReturn(checking(AccountStatus.ACTIVE));

        mockMvc.perform(get("/api/accounts/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("CHECKING"));
    }

    @Test
    void changesStatus() throws Exception {
        when(accountUseCase.changeStatus(5L, AccountStatus.INACTIVE)).thenReturn(checking(AccountStatus.INACTIVE));

        mockMvc.perform(patch("/api/accounts/5/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void cancelWithBalanceReturns422() throws Exception {
        when(accountUseCase.changeStatus(5L, AccountStatus.CANCELLED))
                .thenThrow(new BusinessRuleException("Solo se pueden cancelar cuentas con saldo igual a $0"));

        mockMvc.perform(patch("/api/accounts/5/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void returnsStatement() throws Exception {
        Customer customer = new Customer(1L, IdentificationType.CC, "1", "Ana", "Pérez", "a@b.co",
                LocalDate.of(1990, 1, 1), NOW, null);
        Transaction tx = new Transaction(1L, "r", TransactionType.DEPOSIT, MovementType.CREDIT, 5L, "3300000001",
                new BigDecimal("200.00"), new BigDecimal("0.00"), new BigDecimal("200.00"), null, "Consignación", NOW);
        when(accountUseCase.getStatement(5L)).thenReturn(
                AccountStatement.of(checking(AccountStatus.ACTIVE), customer, List.of(tx), NOW));

        mockMvc.perform(get("/api/accounts/5/statement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.fullName").value("Ana Pérez"))
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.totalCredits").value(200.00));
    }
}
