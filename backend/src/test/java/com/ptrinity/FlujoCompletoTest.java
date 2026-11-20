package com.ptrinity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de extremo a extremo: API real, servicios reales y base de datos H2 con el esquema de Flyway. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlujoCompletoTest {

	@Autowired
	private MockMvc mvc;

	private long crear(String url, String json) throws Exception {
		String cuerpo = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(cuerpo);
		m.find();
		return Long.parseLong(m.group(1));
	}

	private long crearCliente(String numero) throws Exception {
		return crear("/api/clientes", """
				{"tipoIdentificacion":"CC","numeroIdentificacion":"%s","nombres":"Ana","apellido":"Gomez",
				 "correo":"ana@correo.com","fechaNacimiento":"1990-05-14"}
				""".formatted(numero));
	}

	@Test
	void flujoDeClienteCuentasYTransacciones() throws Exception {
		long cliente = crearCliente("777001");
		long ahorros = crear("/api/cuentas", "{\"clienteId\":%d,\"tipoCuenta\":\"AHORROS\"}".formatted(cliente));
		long corriente = crear("/api/cuentas",
				"{\"clienteId\":%d,\"tipoCuenta\":\"CORRIENTE\",\"exentaGmf\":true}".formatted(cliente));

		mvc.perform(post("/api/transacciones/consignaciones").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaId\":%d,\"monto\":1000000}".formatted(ahorros)))
				.andExpect(status().isCreated());

		// 300.000 + GMF 1.200 sale de ahorros; entran 300.000 a corriente
		mvc.perform(post("/api/transacciones/transferencias").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaOrigenId\":%d,\"cuentaDestinoId\":%d,\"monto\":300000}".formatted(ahorros, corriente)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.debito.gmf").value(1200.00));

		mvc.perform(get("/api/cuentas/" + ahorros))
				.andExpect(jsonPath("$.saldo").value(698800.00))
				.andExpect(jsonPath("$.numeroCuenta").value(org.hamcrest.Matchers.matchesPattern("53\\d{8}")));
		mvc.perform(get("/api/cuentas/" + corriente))
				.andExpect(jsonPath("$.saldo").value(300000.00))
				.andExpect(jsonPath("$.numeroCuenta").value(org.hamcrest.Matchers.matchesPattern("33\\d{8}")));

		// Ahorros no puede quedar negativa
		mvc.perform(post("/api/transacciones/retiros").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaId\":%d,\"monto\":698800}".formatted(ahorros)))
				.andExpect(status().isUnprocessableContent());

		mvc.perform(get("/api/transacciones").param("cuentaId", String.valueOf(ahorros)))
				.andExpect(jsonPath("$.length()").value(2));
		mvc.perform(get("/api/cuentas/%d/estado-de-cuenta".formatted(ahorros)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCreditos").value(1000000.00))
				.andExpect(jsonPath("$.totalDebitos").value(301200.00));

		// Cliente con productos no se elimina; cuenta con saldo no se cancela
		mvc.perform(delete("/api/clientes/" + cliente)).andExpect(status().isUnprocessableContent());
		mvc.perform(delete("/api/cuentas/" + corriente)).andExpect(status().isUnprocessableContent());

		// Cuenta inactiva no recibe dinero
		mvc.perform(patch("/api/cuentas/%d/estado".formatted(corriente)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"INACTIVA\"}")).andExpect(status().isOk());
		mvc.perform(post("/api/transacciones/consignaciones").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cuentaId\":%d,\"monto\":10}".formatted(corriente)))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void clienteDuplicadoYEliminacionSinProductos() throws Exception {
		long cliente = crearCliente("777002");

		mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("""
				{"tipoIdentificacion":"CC","numeroIdentificacion":"777002","nombres":"Otro","apellido":"Cliente",
				 "correo":"otro@correo.com","fechaNacimiento":"1980-01-01"}
				""")).andExpect(status().isConflict());

		mvc.perform(delete("/api/clientes/" + cliente)).andExpect(status().isNoContent());
		mvc.perform(get("/api/clientes/" + cliente)).andExpect(status().isNotFound());
	}

	@Test
	void documentacionOpenApiYSalud() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("PTrinity API"))
				.andExpect(jsonPath("$.paths['/api/transacciones/transferencias']").exists());
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}
