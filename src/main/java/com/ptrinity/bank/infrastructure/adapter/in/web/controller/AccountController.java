package com.ptrinity.bank.infrastructure.adapter.in.web.controller;

import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.port.in.AccountUseCase;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.AccountRequest;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.AccountResponse;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.AccountStatusRequest;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.StatementResponse;
import com.ptrinity.bank.infrastructure.adapter.in.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * Adaptador de entrada REST para los productos financieros (cuentas).
 */
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Cuentas")
public class AccountController {

    private final AccountUseCase accountUseCase;

    public AccountController(AccountUseCase accountUseCase) {
        this.accountUseCase = accountUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista las cuentas, opcionalmente filtradas por cliente")
    public List<AccountResponse> findAll(@RequestParam(required = false) Long customerId) {
        return accountUseCase.findAll(customerId).stream().map(WebMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una cuenta")
    public AccountResponse findById(@PathVariable Long id) {
        return WebMapper.toResponse(accountUseCase.findById(id));
    }

    @PostMapping
    @Operation(summary = "Abre una cuenta de ahorros o corriente para un cliente")
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody AccountRequest request) {
        Account account = accountUseCase.open(WebMapper.toCommand(request));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(account.getId()).toUri();
        return ResponseEntity.created(location).body(WebMapper.toResponse(account));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activa, inactiva o cancela una cuenta")
    public AccountResponse changeStatus(@PathVariable Long id, @Valid @RequestBody AccountStatusRequest request) {
        return WebMapper.toResponse(accountUseCase.changeStatus(id, request.status()));
    }

    @GetMapping("/{id}/statement")
    @Operation(summary = "Consulta el estado de cuenta")
    public StatementResponse statement(@PathVariable Long id) {
        return WebMapper.toResponse(accountUseCase.getStatement(id));
    }
}
