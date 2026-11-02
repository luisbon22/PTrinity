package com.ptrinity.bank.infrastructure.adapter.in.web.controller;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import com.ptrinity.bank.domain.exception.ResourceNotFoundException;
import com.ptrinity.bank.domain.model.MovementType;
import com.ptrinity.bank.domain.model.Transaction;
import com.ptrinity.bank.domain.model.TransactionType;
import com.ptrinity.bank.domain.port.in.DepositCommand;
import com.ptrinity.bank.domain.port.in.TransactionUseCase;
import com.ptrinity.bank.domain.port.in.TransferCommand;
import com.ptrinity.bank.domain.port.in.WithdrawalCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionUseCase transactionUseCase;

    private Transaction tx(TransactionType type, MovementType movement, long accountId, String number) {
        return new Transaction(1L, "ref", type, movement, accountId, number, new BigDecimal("100.00"),
                new BigDecimal("0.00"), new BigDecimal("100.00"), null, type.getDescription(), NOW);
    }

    @Test
    void deposits() throws Exception {
        when(transactionUseCase.deposit(any())).thenReturn(
                tx(TransactionType.DEPOSIT, MovementType.CREDIT, 1L, "5300000001"));

        mockMvc.perform(post("/api/transactions/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"5300000001\",\"amount\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movement").value("CREDIT"));
        verify(transactionUseCase).deposit(new DepositCommand("5300000001", new BigDecimal("100"), null));
    }

    @Test
    void withdraws() throws Exception {
        when(transactionUseCase.withdraw(any())).thenReturn(
                tx(TransactionType.WITHDRAWAL, MovementType.DEBIT, 1L, "5300000001"));

        mockMvc.perform(post("/api/transactions/withdrawals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"5300000001\",\"amount\":100,\"description\":\"Cajero\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));
        verify(transactionUseCase).withdraw(new WithdrawalCommand("5300000001", new BigDecimal("100"), "Cajero"));
    }

    @Test
    void insufficientFundsReturns422() throws Exception {
        when(transactionUseCase.withdraw(any())).thenThrow(new BusinessRuleException("Saldo insuficiente"));

        mockMvc.perform(post("/api/transactions/withdrawals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"5300000001\",\"amount\":100}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Saldo insuficiente"));
    }

    @Test
    void rejectsInvalidAmountsAndAccountNumbers() throws Exception {
        mockMvc.perform(post("/api/transactions/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"123\",\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
        verify(transactionUseCase, never()).deposit(any());
    }

    @Test
    void transfersAndReturnsBothMovements() throws Exception {
        when(transactionUseCase.transfer(any())).thenReturn(List.of(
                tx(TransactionType.TRANSFER, MovementType.DEBIT, 1L, "5300000001"),
                tx(TransactionType.TRANSFER, MovementType.CREDIT, 2L, "3300000001")));

        mockMvc.perform(post("/api/transactions/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountNumber":"5300000001","destinationAccountNumber":"3300000001",
                                 "amount":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].movement").value("DEBIT"))
                .andExpect(jsonPath("$[1].movement").value("CREDIT"));
        verify(transactionUseCase).transfer(
                new TransferCommand("5300000001", "3300000001", new BigDecimal("100"), null));
    }

    @Test
    void transferToUnknownAccountReturns404() throws Exception {
        when(transactionUseCase.transfer(any())).thenThrow(new ResourceNotFoundException("Cuenta", "3399999999"));

        mockMvc.perform(post("/api/transactions/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountNumber":"5300000001","destinationAccountNumber":"3399999999",
                                 "amount":100}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsTransactionsByAccount() throws Exception {
        when(transactionUseCase.findAll(1L)).thenReturn(List.of(
                tx(TransactionType.DEPOSIT, MovementType.CREDIT, 1L, "5300000001")));

        mockMvc.perform(get("/api/transactions").param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("5300000001"));
    }
}
