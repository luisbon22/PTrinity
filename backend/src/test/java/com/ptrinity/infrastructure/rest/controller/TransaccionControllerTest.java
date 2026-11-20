package com.ptrinity.infrastructure.rest.controller;

import com.ptrinity.domain.exception.ReglaNegocioException;
import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.domain.model.Naturaleza;
import com.ptrinity.domain.model.TipoTransaccion;
import com.ptrinity.domain.port.in.RealizarTransaccionesUseCase;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransaccionController.class)
class TransaccionControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RealizarTransaccionesUseCase transacciones;

	private static Movimiento mov(Long cuentaId, TipoTransaccion tipo, Naturaleza naturaleza) {
		return new Movimiento(10L, cuentaId, tipo, naturaleza, new BigDecimal("100.00"), new BigDecimal("0.40"),
				new BigDecimal("899.60"), "ref", null, null, LocalDateTime.of(2026, 1, 15, 10, 0));
	}

	@Test
	void consignar() throws Exception {
		when(transacciones.consignar(1L, new BigDecimal("100.00"), "Deposito"))
				.thenReturn(mov(1L, TipoTransaccion.CONSIGNACION, Naturaleza.CREDITO));

		mvc.perform(post("/api/transacciones/consignaciones").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaId\":1,\"monto\":100.00,\"descripcion\":\"Deposito\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("CONSIGNACION"))
				.andExpect(jsonPath("$.naturaleza").value("CREDITO"));
	}

	@Test
	void retirar() throws Exception {
		when(transacciones.retirar(eq(1L), any(), any())).thenReturn(mov(1L, TipoTransaccion.RETIRO, Naturaleza.DEBITO));

		mvc.perform(post("/api/transacciones/retiros").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaId\":1,\"monto\":100}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.gmf").value(0.40));
	}

	@Test
	void montoInvalidoDevuelve400() throws Exception {
		mvc.perform(post("/api/transacciones/retiros").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaId\":1,\"monto\":-5}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.monto").exists());
		mvc.perform(post("/api/transacciones/consignaciones").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaId\":1,\"monto\":1.234}"))
				.andExpect(status().isBadRequest());
		verify(transacciones, never()).retirar(any(), any(), any());
	}

	@Test
	void saldoInsuficienteDevuelve422() throws Exception {
		when(transacciones.retirar(eq(1L), any(), any())).thenThrow(new ReglaNegocioException("Saldo insuficiente"));

		mvc.perform(post("/api/transacciones/retiros").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaId\":1,\"monto\":100}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.title").value("Regla de negocio"));
	}

	@Test
	void transferir() throws Exception {
		when(transacciones.transferir(eq(1L), eq(2L), any(), any())).thenReturn(List.of(
				mov(1L, TipoTransaccion.TRANSFERENCIA, Naturaleza.DEBITO),
				mov(2L, TipoTransaccion.TRANSFERENCIA, Naturaleza.CREDITO)));

		mvc.perform(post("/api/transacciones/transferencias").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaOrigenId\":1,\"cuentaDestinoId\":2,\"monto\":100}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.debito.cuentaId").value(1))
				.andExpect(jsonPath("$.debito.naturaleza").value("DEBITO"))
				.andExpect(jsonPath("$.credito.cuentaId").value(2))
				.andExpect(jsonPath("$.credito.naturaleza").value("CREDITO"));
	}

	@Test
	void transferenciaSinDestinoDevuelve400() throws Exception {
		mvc.perform(post("/api/transacciones/transferencias").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaOrigenId\":1,\"monto\":100}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.cuentaDestinoId").exists());
	}

	@Test
	void listarYConsultar() throws Exception {
		when(transacciones.listarPorCuenta(1L)).thenReturn(List.of(mov(1L, TipoTransaccion.RETIRO, Naturaleza.DEBITO)));
		when(transacciones.obtener(10L)).thenReturn(mov(1L, TipoTransaccion.RETIRO, Naturaleza.DEBITO));

		mvc.perform(get("/api/transacciones").param("cuentaId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(10));
		mvc.perform(get("/api/transacciones/10")).andExpect(status().isOk());
	}

	@Test
	void listarSinCuentaDevuelve400() throws Exception {
		mvc.perform(get("/api/transacciones")).andExpect(status().isBadRequest());
	}
}
