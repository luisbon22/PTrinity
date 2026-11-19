package com.ptrinity.infrastructure.persistence.adapter;

import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.model.DatosCliente;
import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.domain.model.Naturaleza;
import com.ptrinity.domain.model.TipoCuenta;
import com.ptrinity.domain.model.TipoIdentificacion;
import com.ptrinity.domain.model.TipoTransaccion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba los adaptadores contra el esquema real de Flyway (H2 en modo PostgreSQL),
 * incluidas las restricciones del DDL.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ ClientePersistenceAdapter.class, CuentaPersistenceAdapter.class, MovimientoPersistenceAdapter.class,
		GeneradorNumeroCuentaAleatorio.class })
class PersistenciaAdapterTest {

	private final Clock reloj = Clock.systemDefaultZone();

	@Autowired
	private ClientePersistenceAdapter clientes;
	@Autowired
	private CuentaPersistenceAdapter cuentas;
	@Autowired
	private MovimientoPersistenceAdapter movimientos;
	@Autowired
	private GeneradorNumeroCuentaAleatorio generador;
	@Autowired
	private JdbcTemplate jdbc;

	private Cliente nuevoCliente(String numero) {
		return clientes.guardar(Cliente.registrar(new DatosCliente(TipoIdentificacion.CC, numero, "Ana", "Gomez",
				"ana@correo.com", LocalDate.of(1990, 1, 1)), reloj));
	}

	@Test
	void guardaYConsultaClientes() {
		Cliente guardado = nuevoCliente("111");

		assertThat(guardado.getId()).isNotNull();
		assertThat(clientes.buscarPorId(guardado.getId())).get()
				.extracting(Cliente::getNumeroIdentificacion).isEqualTo("111");
		assertThat(clientes.buscarPorIdentificacion(TipoIdentificacion.CC, "111")).isPresent();
		assertThat(clientes.buscarPorIdentificacion(TipoIdentificacion.CE, "111")).isEmpty();
		assertThat(clientes.listar()).extracting(Cliente::getId).contains(guardado.getId());

		clientes.eliminar(guardado.getId());
		assertThat(clientes.buscarPorId(guardado.getId())).isEmpty();
	}

	@Test
	void guardaCuentasYMovimientos() {
		Cliente cliente = nuevoCliente("222");
		Cuenta cuenta = cuentas.guardar(Cuenta.abrir(TipoCuenta.AHORROS, generador.generar(TipoCuenta.AHORROS), false,
				cliente.getId(), reloj));
		cuenta.acreditar(new BigDecimal("1000"), reloj);
		cuenta = cuentas.guardar(cuenta);
		movimientos.guardar(Movimiento.nuevo(cuenta, TipoTransaccion.CONSIGNACION, Naturaleza.CREDITO,
				new BigDecimal("1000"), BigDecimal.ZERO.setScale(2), "ref-1", null, "inicial"));

		assertThat(cuentas.buscarPorIdParaActualizar(cuenta.getId())).get()
				.extracting(Cuenta::getSaldo).isEqualTo(new BigDecimal("1000.00"));
		assertThat(cuentas.buscarPorId(cuenta.getId())).isPresent();
		assertThat(cuentas.listar()).isNotEmpty();
		assertThat(cuentas.listarPorCliente(cliente.getId())).hasSize(1);
		assertThat(cuentas.existePorNumero(cuenta.getNumero())).isTrue();
		assertThat(cuentas.existePorCliente(cliente.getId())).isTrue();

		var lista = movimientos.listarPorCuenta(cuenta.getId());
		assertThat(lista).hasSize(1);
		assertThat(movimientos.buscarPorId(lista.getFirst().id())).isPresent();
		LocalDateTime ahora = LocalDateTime.now(reloj);
		assertThat(movimientos.listarPorCuentaEntre(cuenta.getId(), ahora.minusDays(1), ahora.plusDays(1))).hasSize(1);
		assertThat(movimientos.listarPorCuentaEntre(cuenta.getId(), ahora.plusDays(1), ahora.plusDays(2))).isEmpty();
	}

	@Test
	void generadorRespetaPrefijoYLongitud() {
		assertThat(generador.generar(TipoCuenta.AHORROS)).matches("53\\d{8}");
		assertThat(generador.generar(TipoCuenta.CORRIENTE)).matches("33\\d{8}");
	}

	@Test
	void laBaseDeDatosRechazaClientesMenoresDeEdad() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO clientes (tipo_identificacion, numero_identificacion, nombres, apellido, correo,
				fecha_nacimiento, fecha_creacion, fecha_modificacion)
				VALUES ('CC', '999', 'Nino', 'Menor', 'n@m.co', CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""")).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void laBaseDeDatosRechazaAhorrosNegativosYCuentasSinCliente() {
		Cliente cliente = nuevoCliente("333");
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO cuentas (tipo_cuenta, numero_cuenta, estado, saldo, exenta_gmf, fecha_creacion,
				fecha_modificacion, cliente_id)
				VALUES ('AHORROS', '5399999999', 'ACTIVA', -1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
				""", cliente.getId())).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO cuentas (tipo_cuenta, numero_cuenta, estado, saldo, exenta_gmf, fecha_creacion,
				fecha_modificacion, cliente_id)
				VALUES ('CORRIENTE', '3399999999', 'ACTIVA', 0, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, -1)
				""")).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void laBaseDeDatosRechazaNumerosDeCuentaConPrefijoIncorrecto() {
		Cliente cliente = nuevoCliente("444");
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO cuentas (tipo_cuenta, numero_cuenta, estado, saldo, exenta_gmf, fecha_creacion,
				fecha_modificacion, cliente_id)
				VALUES ('AHORROS', '3312345678', 'ACTIVA', 0, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
				""", cliente.getId())).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void noSeBorraUnClienteConProductos() {
		Cliente cliente = nuevoCliente("555");
		cuentas.guardar(Cuenta.abrir(TipoCuenta.CORRIENTE, "3300000555", false, cliente.getId(), reloj));

		assertThatThrownBy(() -> jdbc.update("DELETE FROM clientes WHERE id = ?", cliente.getId()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
