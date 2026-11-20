package com.ptrinity.infrastructure.rest.controller;

import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.domain.port.in.RealizarTransaccionesUseCase;
import com.ptrinity.infrastructure.rest.dto.MovimientoResponse;
import com.ptrinity.infrastructure.rest.dto.OperacionRequest;
import com.ptrinity.infrastructure.rest.dto.TransferenciaRequest;
import com.ptrinity.infrastructure.rest.dto.TransferenciaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Los movimientos son inmutables: se crean y se consultan, nunca se editan ni se borran. */
@RestController
@RequestMapping("/api/transacciones")
@Tag(name = "Transacciones", description = "Consignaciones, retiros y transferencias")
public class TransaccionController {

	private final RealizarTransaccionesUseCase transacciones;

	public TransaccionController(RealizarTransaccionesUseCase transacciones) {
		this.transacciones = transacciones;
	}

	@PostMapping("/consignaciones")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Consignar")
	public MovimientoResponse consignar(@Valid @RequestBody OperacionRequest request) {
		return MovimientoResponse.de(transacciones.consignar(request.cuentaId(), request.monto(), request.descripcion()));
	}

	@PostMapping("/retiros")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Retirar", description = "Cobra GMF (4 x 1000) si la cuenta no es exenta")
	public MovimientoResponse retirar(@Valid @RequestBody OperacionRequest request) {
		return MovimientoResponse.de(transacciones.retirar(request.cuentaId(), request.monto(), request.descripcion()));
	}

	@PostMapping("/transferencias")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Transferir", description = "Genera el debito en la cuenta origen y el credito en la destino")
	public TransferenciaResponse transferir(@Valid @RequestBody TransferenciaRequest request) {
		List<Movimiento> movs = transacciones.transferir(request.cuentaOrigenId(), request.cuentaDestinoId(),
				request.monto(), request.descripcion());
		return new TransferenciaResponse(MovimientoResponse.de(movs.get(0)), MovimientoResponse.de(movs.get(1)));
	}

	@GetMapping
	@Operation(summary = "Movimientos de una cuenta", description = "Del mas reciente al mas antiguo")
	public List<MovimientoResponse> listar(@RequestParam Long cuentaId) {
		return transacciones.listarPorCuenta(cuentaId).stream().map(MovimientoResponse::de).toList();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar movimiento")
	public MovimientoResponse obtener(@PathVariable Long id) {
		return MovimientoResponse.de(transacciones.obtener(id));
	}
}
