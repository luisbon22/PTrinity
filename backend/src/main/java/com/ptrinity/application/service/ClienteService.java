package com.ptrinity.application.service;

import com.ptrinity.domain.exception.RecursoDuplicadoException;
import com.ptrinity.domain.exception.RecursoNoEncontradoException;
import com.ptrinity.domain.exception.ReglaNegocioException;
import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.DatosCliente;
import com.ptrinity.domain.port.in.GestionarClientesUseCase;
import com.ptrinity.domain.port.out.ClienteRepositoryPort;
import com.ptrinity.domain.port.out.CuentaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@Transactional
public class ClienteService implements GestionarClientesUseCase {

	private final ClienteRepositoryPort clientes;
	private final CuentaRepositoryPort cuentas;
	private final Clock reloj;

	public ClienteService(ClienteRepositoryPort clientes, CuentaRepositoryPort cuentas, Clock reloj) {
		this.clientes = clientes;
		this.cuentas = cuentas;
		this.reloj = reloj;
	}

	@Override
	public Cliente crear(DatosCliente datos) {
		Cliente cliente = Cliente.registrar(datos, reloj);
		validarIdentificacionLibre(cliente, null);
		return clientes.guardar(cliente);
	}

	@Override
	public Cliente actualizar(Long id, DatosCliente datos) {
		Cliente cliente = obtener(id);
		cliente.actualizar(datos, reloj);
		validarIdentificacionLibre(cliente, id);
		return clientes.guardar(cliente);
	}

	@Override
	@Transactional(readOnly = true)
	public Cliente obtener(Long id) {
		return clientes.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Cliente> listar() {
		return clientes.listar();
	}

	@Override
	public void eliminar(Long id) {
		obtener(id);
		if (cuentas.existePorCliente(id)) {
			throw new ReglaNegocioException("No se puede eliminar un cliente con productos vinculados");
		}
		clientes.eliminar(id);
	}

	private void validarIdentificacionLibre(Cliente cliente, Long idActual) {
		clientes.buscarPorIdentificacion(cliente.getTipoIdentificacion(), cliente.getNumeroIdentificacion())
				.filter(existente -> !existente.getId().equals(idActual))
				.ifPresent(existente -> {
					throw new RecursoDuplicadoException("Ya existe un cliente con identificacion "
							+ cliente.getTipoIdentificacion() + " " + cliente.getNumeroIdentificacion());
				});
	}
}
