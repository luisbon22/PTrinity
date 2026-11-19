package com.ptrinity.infrastructure.persistence.adapter;

import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.port.out.CuentaRepositoryPort;
import com.ptrinity.infrastructure.persistence.repository.CuentaJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class CuentaPersistenceAdapter implements CuentaRepositoryPort {

	private final CuentaJpaRepository repository;

	CuentaPersistenceAdapter(CuentaJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Cuenta guardar(Cuenta cuenta) {
		return PersistenciaMapper.aDominio(repository.save(PersistenciaMapper.aEntidad(cuenta)));
	}

	@Override
	public Optional<Cuenta> buscarPorId(Long id) {
		return repository.findById(id).map(PersistenciaMapper::aDominio);
	}

	@Override
	public Optional<Cuenta> buscarPorIdParaActualizar(Long id) {
		return repository.findByIdForUpdate(id).map(PersistenciaMapper::aDominio);
	}

	@Override
	public List<Cuenta> listar() {
		return repository.findAll(Sort.by("id")).stream().map(PersistenciaMapper::aDominio).toList();
	}

	@Override
	public List<Cuenta> listarPorCliente(Long clienteId) {
		return repository.findByClienteIdOrderByIdAsc(clienteId).stream().map(PersistenciaMapper::aDominio).toList();
	}

	@Override
	public boolean existePorNumero(String numero) {
		return repository.existsByNumero(numero);
	}

	@Override
	public boolean existePorCliente(Long clienteId) {
		return repository.existsByClienteId(clienteId);
	}
}
