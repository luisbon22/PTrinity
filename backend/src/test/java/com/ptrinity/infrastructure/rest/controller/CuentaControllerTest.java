package com.ptrinity.infrastructure.rest.controller;

import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.model.DatosCliente;
import com.ptrinity.domain.model.EstadoCuenta;
import com.ptrinity.domain.model.EstadoDeCuenta;
import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.domain.model.Naturaleza;
import com.ptrinity.domain.model.TipoCuenta;
import com.ptrinity.domain.model.TipoIdentificacion;
import com.ptrinity.domain.model.TipoTransaccion;
import com.ptrinity.domain.port.in.GestionarCuentasUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CuentaController.class)
class CuentaControllerTest {

	private final LocalDateTime f = LocalDateTime.of(2026, 1, 15, 10, 0);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private GestionarCuentasUseCase cuentas;

	private Cuenta cuenta(TipoCuenta tipo, EstadoCuenta estado) {
		return Cuenta.reconstruir(1L, tipo, tipo.prefijo() + "00000001", estado, new BigDecimal("100.00"), false, 1L, f, f);
	}

	@Test
	void abrirCuenta() throws Exception {
		when(cuentas.abrir(1L, TipoCuenta.AHORROS, false)).thenReturn(cuenta(TipoCuenta.AHORROS, EstadoCuenta.ACTIVA));

		mvc.perform(post("/api/cuentas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"clienteId\":1,\"tipoCuenta\":\"AHORROS\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.numeroCuenta").value("5300000001"))
				.andExpect(jsonPath("$.estado").value("ACTIVA"))
				.andExpect(jsonPath("$.saldoDisponible").value(100.00));
	}

	@Test
	void abrirSinTipoDevuelve400() throws Exception {
		mvc.perform(post("/api/cuentas").contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.tipoCuenta").exists());
	}

	@Test
	void corrienteMuestraSaldoDisponibleConSobregiro() throws Exception {
		when(cuentas.obtener(1L)).thenReturn(cuenta(TipoCuenta.CORRIENTE, EstadoCuenta.ACTIVA));

		mvc.perform(get("/api/cuentas/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saldoDisponible").value(1000100.00));
	}

	@Test
	void listarTodasOPorCliente() throws Exception {
		when(cuentas.listar(null)).thenReturn(List.of(cuenta(TipoCuenta.AHORROS, EstadoCuenta.ACTIVA)));
		when(cuentas.listar(1L)).thenReturn(List.of());

		mvc.perform(get("/api/cuentas")).andExpect(jsonPath("$.length()").value(1));
		mvc.perform(get("/api/cuentas").param("clienteId", "1")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void actualizarExencion() throws Exception {
		when(cuentas.actualizarExencionGmf(1L, true)).thenReturn(cuenta(TipoCuenta.AHORROS, EstadoCuenta.ACTIVA));

		mvc.perform(put("/api/cuentas/1").contentType(MediaType.APPLICATION_JSON).content("{\"exentaGmf\":true}"))
				.andExpect(status().isOk());
		verify(cuentas).actualizarExencionGmf(1L, true);
	}

	@Test
	void cambiarEstado() throws Exception {
		when(cuentas.cambiarEstado(1L, EstadoCuenta.INACTIVA)).thenReturn(cuenta(TipoCuenta.AHORROS, EstadoCuenta.INACTIVA));

		mvc.perform(patch("/api/cuentas/1/estado").contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"INACTIVA\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVA"));
	}

	@Test
	void deleteCancelaLaCuenta() throws Exception {
		when(cuentas.cambiarEstado(1L, EstadoCuenta.CANCELADA)).thenReturn(cuenta(TipoCuenta.AHORROS, EstadoCuenta.CANCELADA));

		mvc.perform(delete("/api/cuentas/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"));
	}

	@Test
	void estadoDeCuenta() throws Exception {
		Cliente cliente = Cliente.reconstruir(1L, new DatosCliente(TipoIdentificacion.CC, "123", "Ana", "Gomez", "a@b.co",
				LocalDate.of(1990, 1, 1)), f, f);
		Movimiento mov = new Movimiento(1L, 1L, TipoTransaccion.CONSIGNACION, Naturaleza.CREDITO, new BigDecimal("100.00"),
				BigDecimal.ZERO, new BigDecimal("100.00"), "r", null, "Deposito", f);
		LocalDate desde = LocalDate.of(2026, 1, 1);
		LocalDate hasta = LocalDate.of(2026, 1, 31);
		when(cuentas.estadoDeCuenta(1L, desde, hasta)).thenReturn(EstadoDeCuenta.de(
				cuenta(TipoCuenta.AHORROS, EstadoCuenta.ACTIVA), cliente, desde, hasta, List.of(mov)));

		mvc.perform(get("/api/cuentas/1/estado-de-cuenta").param("desde", "2026-01-01").param("hasta", "2026-01-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titular").value("Ana Gomez"))
				.andExpect(jsonPath("$.identificacionTitular").value("CC 123"))
				.andExpect(jsonPath("$.totalCreditos").value(100.00))
				.andExpect(jsonPath("$.movimientos[0].descripcion").value("Deposito"));
	}

	@Test
	void violacionDeIntegridadDevuelve409() throws Exception {
		when(cuentas.abrir(anyLong(), any(), anyBoolean())).thenThrow(new DataIntegrityViolationException("fk"));

		mvc.perform(post("/api/cuentas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"clienteId\":1,\"tipoCuenta\":\"CORRIENTE\",\"exentaGmf\":true}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Conflicto de integridad"));
	}
}
