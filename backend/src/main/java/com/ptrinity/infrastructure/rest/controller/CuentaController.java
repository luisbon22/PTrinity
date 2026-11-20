package com.ptrinity.infrastructure.rest.controller;

import com.ptrinity.domain.model.EstadoCuenta;
import com.ptrinity.domain.port.in.GestionarCuentasUseCase;
import com.ptrinity.infrastructure.rest.dto.ActualizarCuentaRequest;
import com.ptrinity.infrastructure.rest.dto.AperturaCuentaRequest;
import com.ptrinity.infrastructure.rest.dto.CambioEstadoRequest;
import com.ptrinity.infrastructure.rest.dto.CuentaResponse;
import com.ptrinity.infrastructure.rest.dto.EstadoDeCuentaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@Tag(name = "Productos (cuentas)", description = "Cuentas de ahorros y corrientes")
public class CuentaController {

	private final GestionarCuentasUseCase cuentas;

	public CuentaController(GestionarCuentasUseCase cuentas) {
		this.cuentas = cuentas;
	}

	@PostMapping
	@Operation(summary = "Abrir cuenta", description = "Numero unico de 10 digitos: ahorros inicia en 53, corriente en 33")
	public ResponseEntity<CuentaResponse> abrir(@Valid @RequestBody AperturaCuentaRequest request) {
		CuentaResponse creada = CuentaResponse.de(cuentas.abrir(request.clienteId(), request.tipoCuenta(), request.esExenta()));
		return ResponseEntity.created(URI.create("/api/cuentas/" + creada.id())).body(creada);
	}

	@GetMapping
	@Operation(summary = "Listar cuentas", description = "Opcionalmente filtradas por cliente")
	public List<CuentaResponse> listar(@RequestParam(required = false) Long clienteId) {
		return cuentas.listar(clienteId).stream().map(CuentaResponse::de).toList();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar cuenta")
	public CuentaResponse obtener(@PathVariable Long id) {
		return CuentaResponse.de(cuentas.obtener(id));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar exencion de GMF")
	public CuentaResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarCuentaRequest request) {
		return CuentaResponse.de(cuentas.actualizarExencionGmf(id, request.exentaGmf()));
	}

	@PatchMapping("/{id}/estado")
	@Operation(summary = "Activar, inactivar o cancelar", description = "Solo se cancela una cuenta con saldo $0")
	public CuentaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
		return CuentaResponse.de(cuentas.cambiarEstado(id, request.estado()));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Cancelar cuenta (borrado logico)",
			description = "Una cuenta con movimientos no se borra fisicamente: queda CANCELADA para conservar el historial")
	public CuentaResponse cancelar(@PathVariable Long id) {
		return CuentaResponse.de(cuentas.cambiarEstado(id, EstadoCuenta.CANCELADA));
	}

	@GetMapping("/{id}/estado-de-cuenta")
	@Operation(summary = "Estado de cuenta", description = "Por defecto, desde el primer dia del mes hasta hoy")
	public EstadoDeCuentaResponse estadoDeCuenta(@PathVariable Long id,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return EstadoDeCuentaResponse.de(cuentas.estadoDeCuenta(id, desde, hasta));
	}
}
