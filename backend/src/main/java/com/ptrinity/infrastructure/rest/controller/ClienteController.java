package com.ptrinity.infrastructure.rest.controller;

import com.ptrinity.domain.port.in.GestionarClientesUseCase;
import com.ptrinity.infrastructure.rest.dto.ClienteRequest;
import com.ptrinity.infrastructure.rest.dto.ClienteResponse;
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

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Registro, consulta, actualizacion y eliminacion de clientes")
public class ClienteController {

	private final GestionarClientesUseCase clientes;

	public ClienteController(GestionarClientesUseCase clientes) {
		this.clientes = clientes;
	}

	@PostMapping
	@Operation(summary = "Registrar cliente", description = "Rechaza menores de edad e identificaciones duplicadas")
	public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
		ClienteResponse creado = ClienteResponse.de(clientes.crear(request.aDominio()));
		return ResponseEntity.created(URI.create("/api/clientes/" + creado.id())).body(creado);
	}

	@GetMapping
	@Operation(summary = "Listar clientes")
	public List<ClienteResponse> listar() {
		return clientes.listar().stream().map(ClienteResponse::de).toList();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar cliente")
	public ClienteResponse obtener(@PathVariable Long id) {
		return ClienteResponse.de(clientes.obtener(id));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar cliente", description = "La fecha de modificacion se recalcula automaticamente")
	public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
		return ClienteResponse.de(clientes.actualizar(id, request.aDominio()));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar cliente", description = "No se permite si el cliente tiene productos vinculados")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		clientes.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}
