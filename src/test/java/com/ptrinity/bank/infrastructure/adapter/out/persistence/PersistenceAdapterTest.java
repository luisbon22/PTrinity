package com.ptrinity.bank.infrastructure.adapter.out.persistence;

import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.model.AccountType;
import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.CustomerData;
import com.ptrinity.bank.domain.model.IdentificationType;
import com.ptrinity.bank.domain.model.Transaction;
import com.ptrinity.bank.domain.model.TransactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración de los adaptadores de persistencia contra el esquema real
 * creado por Flyway (H2 en modo PostgreSQL).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CustomerPersistenceAdapter.class, AccountPersistenceAdapter.class,
        TransactionPersistenceAdapter.class, SequenceAccountNumberGenerator.class})
class PersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);

    @Autowired
    private CustomerPersistenceAdapter customers;
    @Autowired
    private AccountPersistenceAdapter accounts;
    @Autowired
    private TransactionPersistenceAdapter transactions;
    @Autowired
    private SequenceAccountNumberGenerator numberGenerator;
    @Autowired
    private EntityManager entityManager;

    private Customer newCustomer(String id, String email) {
        return customers.save(Customer.create(new CustomerData(IdentificationType.CC, id, "Ana", "Pérez", email,
                LocalDate.of(1990, 1, 1)), NOW));
    }

    @Test
    void savesAndFindsCustomerAndDetectsDuplicates() {
        Customer saved = newCustomer("123", "ana@mail.com");

        assertThat(saved.getId()).isNotNull();
        assertThat(customers.findById(saved.getId())).isPresent();
        assertThat(customers.existsByIdentification(IdentificationType.CC, "123", null)).isTrue();
        assertThat(customers.existsByIdentification(IdentificationType.CC, "123", saved.getId())).isFalse();
        assertThat(customers.existsByEmail("ANA@mail.com", null)).isTrue();
    }

    @Test
    void generatesSequentialAccountNumbersWithPrefix() {
        String first = numberGenerator.nextAccountNumber(AccountType.SAVINGS);
        String second = numberGenerator.nextAccountNumber(AccountType.SAVINGS);
        String checking = numberGenerator.nextAccountNumber(AccountType.CHECKING);

        assertThat(first).hasSize(10).startsWith("53");
        assertThat(second).isNotEqualTo(first).startsWith("53");
        assertThat(checking).hasSize(10).startsWith("33");
    }

    @Test
    void savesAccountAndTransactionsAndUpdatesBalance() {
        Customer customer = newCustomer("456", "luis@mail.com");
        Account account = accounts.save(Account.open(AccountType.SAVINGS,
                numberGenerator.nextAccountNumber(AccountType.SAVINGS), customer.getId(), true, BigDecimal.ZERO, NOW));

        Account locked = accounts.findByAccountNumberForUpdate(account.getAccountNumber()).orElseThrow();
        locked.credit(new BigDecimal("5000"), NOW);
        accounts.save(locked);
        transactions.save(Transaction.credit("ref-1", TransactionType.DEPOSIT, locked, new BigDecimal("5000"),
                null, "Prueba", NOW));
        entityManager.flush();
        entityManager.clear();

        assertThat(accounts.findById(account.getId()).orElseThrow().getBalance()).isEqualByComparingTo("5000");
        assertThat(accounts.existsByCustomerId(customer.getId())).isTrue();
        assertThat(accounts.findByCustomerId(customer.getId())).hasSize(1);
        assertThat(transactions.findByAccountId(account.getId()))
                .singleElement()
                .satisfies(t -> assertThat(t.accountNumber()).isEqualTo(account.getAccountNumber()));
    }

    @Test
    void databaseRejectsMinorCustomer() {
        var insert = entityManager.createNativeQuery("""
                INSERT INTO customers (identification_type, identification_number, first_name, last_name, email,
                                       birth_date, created_at)
                VALUES ('CC', '999', 'Mateo', 'Menor', 'menor@mail.com', DATE '2015-01-01',
                        TIMESTAMP '2026-11-01 10:00:00')
                """);

        assertThatThrownBy(insert::executeUpdate).isInstanceOf(PersistenceException.class);
    }
}
