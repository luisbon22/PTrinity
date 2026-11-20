package com.ptrinity.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** El DML de demostracion carga y deja saldos coherentes con sus movimientos. */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:ptrinity_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
		"spring.flyway.locations=classpath:db/migration,classpath:db/demo" })
class DatosDemoTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void cargaLosDatosDeDemostracion() {
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM clientes", Integer.class)).isEqualTo(3);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas", Integer.class)).isEqualTo(3);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM movimientos", Integer.class)).isEqualTo(6);
	}

	@Test
	void elSaldoDeCadaCuentaCoincideConSusMovimientos() {
		Integer descuadradas = jdbc.queryForObject("""
				SELECT COUNT(*) FROM cuentas c
				WHERE c.saldo <> (SELECT COALESCE(SUM(CASE WHEN m.naturaleza = 'CREDITO' THEN m.monto
				                                         ELSE -(m.monto + m.gmf) END), 0)
				                  FROM movimientos m WHERE m.cuenta_id = c.id)
				""", Integer.class);
		assertThat(descuadradas).isZero();
		assertThat(jdbc.queryForObject("SELECT saldo FROM cuentas WHERE numero_cuenta = '5310000002'",
				BigDecimal.class)).isEqualByComparingTo("799200");
	}
}
