package com.ptrinity.domain.port.in;

import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.DatosCliente;

import java.util.List;

public interface GestionarClientesUseCase {

	Cliente crear(DatosCliente datos);

	Cliente actualizar(Long id, DatosCliente datos);

	Cliente obtener(Long id);

	List<Cliente> listar();

	void eliminar(Long id);
}
