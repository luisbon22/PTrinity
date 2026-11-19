package com.ptrinity.domain.port.out;

import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.TipoIdentificacion;

import java.util.List;
import java.util.Optional;

public interface ClienteRepositoryPort {

	Cliente guardar(Cliente cliente);

	Optional<Cliente> buscarPorId(Long id);

	Optional<Cliente> buscarPorIdentificacion(TipoIdentificacion tipo, String numero);

	List<Cliente> listar();

	void eliminar(Long id);
}
