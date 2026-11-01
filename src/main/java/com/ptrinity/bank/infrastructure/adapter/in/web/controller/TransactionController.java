package com.ptrinity.bank.infrastructure.adapter.in.web.controller;

import com.ptrinity.bank.domain.port.in.TransactionUseCase;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.MovementRequest;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.TransferRequest;
import com.ptrinity.bank.infrastructure.adapter.in.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST para los movimientos financieros.
 * Los movimientos son registros contables inmutables: se crean y se consultan,
 * pero no se modifican ni se eliminan.
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transacciones")
public class TransactionController {

    private final TransactionUseCase transactionUseCase;

    public TransactionController(TransactionUseCase transactionUseCase) {
        this.transactionUseCase = transactionUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista los movimientos, opcionalmente filtrados por cuenta")
    public List<TransactionResponse> findAll(@RequestParam(required = false) Long accountId) {
        return transactionUseCase.findAll(accountId).stream().map(WebMapper::toResponse).toList();
    }

    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Realiza una consignación")
    public TransactionResponse deposit(@Valid @RequestBody MovementRequest request) {
        return WebMapper.toResponse(transactionUseCase.deposit(WebMapper.toDeposit(request)));
    }

    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Realiza un retiro")
    public TransactionResponse withdraw(@Valid @RequestBody MovementRequest request) {
        return WebMapper.toResponse(transactionUseCase.withdraw(WebMapper.toWithdrawal(request)));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfiere entre cuentas: genera un débito en origen y un crédito en destino")
    public List<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return transactionUseCase.transfer(WebMapper.toTransfer(request)).stream()
                .map(WebMapper::toResponse)
                .toList();
    }
}
