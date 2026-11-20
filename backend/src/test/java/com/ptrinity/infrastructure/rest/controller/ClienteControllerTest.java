package com.ptrinity.infrastructure.rest.controller;

import com.ptrinity.domain.exception.RecursoDuplicadoException;
import com.ptrinity.domain.exception.RecursoNoEncontradoException;
import com.ptrinity.domain.exception.ReglaNegocioException;
import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.DatosCliente;
import com.ptrinity.domain.model.TipoIdentificacion;
import com.ptrinity.domain.port.in.GestionarClientesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

	private static final String JSON_VALIDO = """
			{"tipoIdentificacion":"CC","numeroIdentificacion":"1020304050","nombres":"Ana","apellido":"Gomez",
			 "correo":"ana@correo.com","fechaNacimiento":"1990-05-14"}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private GestionarClientesUseCase clientes;

	private static Cliente cliente() {
		LocalDateTime f = LocalDateTime.of(2026, 1, 15, 10, 0);
		return Cliente.reconstruir(1L, new DatosCliente(TipoIdentificacion.CC, "1020304050", "Ana", "Gomez",
				"ana@correo.com", LocalDate.of(1990, 5, 14)), f, f);
	}

	@Test
	void crearDevuelve201ConUbicacion() throws Exception {
		when(clientes.crear(any())).thenReturn(cliente());

		mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/clientes/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.fechaCreacion").value("2026-01-15T10:00:00"));
	}

	@Test
	void crearConDatosInvalidosDevuelve400ConErroresPorCampo() throws Exception {
		String json = """
				{"tipoIdentificacion":"CC","numeroIdentificacion":"10-20","nombres":"A","apellido":"Gomez",
				 "correo":"correo-malo","fechaNacimiento":"2999-01-01"}
				""";

		mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.nombres").exists())
				.andExpect(jsonPath("$.errores.correo").value("debe tener el formato xxxx@xxxxx.xxx"))
				.andExpect(jsonPath("$.errores.numeroIdentificacion").exists())
				.andExpect(jsonPath("$.errores.fechaNacimiento").exists());
		verify(clientes, never()).crear(any());
	}

	@Test
	void tipoDeIdentificacionDesconocidoDevuelve400() throws Exception {
		mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(JSON_VALIDO.replace("\"CC\"", "\"XX\"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Solicitud mal formada"));
	}

	@Test
	void menorDeEdadDevuelve422() throws Exception {
		when(clientes.crear(any())).thenThrow(new ReglaNegocioException("El cliente debe ser mayor de edad"));

		mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("El cliente debe ser mayor de edad"));
	}

	@Test
	void identificacionDuplicadaDevuelve409() throws Exception {
		when(clientes.crear(any())).thenThrow(new RecursoDuplicadoException("Ya existe"));

		mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
				.andExpect(status().isConflict());
	}

	@Test
	void listarYConsultar() throws Exception {
		when(clientes.listar()).thenReturn(List.of(cliente()));
		when(clientes.obtener(1L)).thenReturn(cliente());

		mvc.perform(get("/api/clientes")).andExpect(status().isOk()).andExpect(jsonPath("$[0].nombres").value("Ana"));
		mvc.perform(get("/api/clientes/1")).andExpect(status().isOk()).andExpect(jsonPath("$.apellido").value("Gomez"));
	}

	@Test
	void consultarInexistenteDevuelve404() throws Exception {
		when(clientes.obtener(9L)).thenThrow(new RecursoNoEncontradoException("Cliente", 9L));

		mvc.perform(get("/api/clientes/9"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Cliente con id 9 no existe"));
	}

	@Test
	void idNoNumericoDevuelve400() throws Exception {
		mvc.perform(get("/api/clientes/abc")).andExpect(status().isBadRequest());
	}

	@Test
	void actualizar() throws Exception {
		when(clientes.actualizar(eq(1L), any())).thenReturn(cliente());

		mvc.perform(put("/api/clientes/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void eliminarDevuelve204() throws Exception {
		mvc.perform(delete("/api/clientes/1")).andExpect(status().isNoContent());
		verify(clientes).eliminar(1L);
	}

	@Test
	void eliminarConProductosDevuelve422() throws Exception {
		doThrow(new ReglaNegocioException("No se puede eliminar un cliente con productos vinculados"))
				.when(clientes).eliminar(1L);

		mvc.perform(delete("/api/clientes/1")).andExpect(status().isUnprocessableContent());
	}
}
