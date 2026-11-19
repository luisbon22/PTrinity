package com.ptrinity.infrastructure.persistence.adapter;

import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.TipoIdentificacion;
import com.ptrinity.domain.port.out.ClienteRepositoryPort;
import com.ptrinity.infrastructure.persistence.repository.ClienteJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class ClientePersistenceAdapter implements ClienteRepositoryPort {

	private final ClienteJpaRepository repository;

	ClientePersistenceAdapter(ClienteJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Cliente guardar(Cliente cliente) {
		return PersistenciaMapper.aDominio(repository.save(PersistenciaMapper.aEntidad(cliente)));
	}

	@Override
	public Optional<Cliente> buscarPorId(Long id) {
		return repository.findById(id).map(PersistenciaMapper::aDominio);
	}

	@Override
	public Optional<Cliente> buscarPorIdentificacion(TipoIdentificacion tipo, String numero) {
		return repository.findByTipoIdentificacionAndNumeroIdentificacion(tipo, numero).map(PersistenciaMapper::aDominio);
	}

	@Override
	public List<Cliente> listar() {
		return repository.findAll(Sort.by("id")).stream().map(PersistenciaMapper::aDominio).toList();
	}

	@Override
	public void eliminar(Long id) {
		repository.deleteById(id);
	}
}
