package com.ptrinity.bank.infrastructure.adapter.in.web.controller;

import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.port.in.CustomerUseCase;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.CustomerRequest;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.ptrinity.bank.infrastructure.adapter.in.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * Adaptador de entrada REST para la administración de clientes.
 */
@RestController
@RequestMapping("/api/customers")
@Tag(name = "Clientes")
public class CustomerController {

    private final CustomerUseCase customerUseCase;

    public CustomerController(CustomerUseCase customerUseCase) {
        this.customerUseCase = customerUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista los clientes")
    public List<CustomerResponse> findAll() {
        return customerUseCase.findAll().stream().map(WebMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un cliente")
    public CustomerResponse findById(@PathVariable Long id) {
        return WebMapper.toResponse(customerUseCase.findById(id));
    }

    @PostMapping
    @Operation(summary = "Registra un cliente (debe ser mayor de edad)")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        Customer customer = customerUseCase.create(WebMapper.toData(request));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(customer.getId()).toUri();
        return ResponseEntity.created(location).body(WebMapper.toResponse(customer));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza los datos de un cliente")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return WebMapper.toResponse(customerUseCase.update(id, WebMapper.toData(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un cliente sin productos vinculados")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
