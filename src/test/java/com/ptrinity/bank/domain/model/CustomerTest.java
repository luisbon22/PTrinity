package com.ptrinity.bank.domain.model;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);

    private CustomerData data(LocalDate birthDate) {
        return new CustomerData(IdentificationType.CC, " 1020304050 ", "Ana", "Pérez",
                "Ana.Perez@Mail.com", birthDate);
    }

    @Test
    void createSetsCreationDateAndNormalizesData() {
        Customer customer = Customer.create(data(LocalDate.of(1990, 5, 20)), NOW);

        assertThat(customer.getCreatedAt()).isEqualTo(NOW);
        assertThat(customer.getUpdatedAt()).isNull();
        assertThat(customer.getIdentificationNumber()).isEqualTo("1020304050");
        assertThat(customer.getEmail()).isEqualTo("ana.perez@mail.com");
    }

    @Test
    void createRejectsMinor() {
        LocalDate seventeen = NOW.toLocalDate().minusYears(18).plusDays(1);

        assertThatThrownBy(() -> Customer.create(data(seventeen), NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mayor de edad");
    }

    @Test
    void createAcceptsCustomerTurningEighteenToday() {
        LocalDate eighteen = NOW.toLocalDate().minusYears(18);

        assertThat(Customer.create(data(eighteen), NOW)).isNotNull();
    }

    @Test
    void createRejectsInvalidEmail() {
        CustomerData invalid = new CustomerData(IdentificationType.CC, "1", "Ana", "Pérez", "ana@mail",
                LocalDate.of(1990, 1, 1));

        assertThatThrownBy(() -> Customer.create(invalid, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("correo");
    }

    @Test
    void createRejectsShortNames() {
        CustomerData shortName = new CustomerData(IdentificationType.CC, "1", "A", "Pérez", "a@b.co",
                LocalDate.of(1990, 1, 1));
        CustomerData shortLastName = new CustomerData(IdentificationType.CC, "1", "Ana", "P", "a@b.co",
                LocalDate.of(1990, 1, 1));

        assertThatThrownBy(() -> Customer.create(shortName, NOW)).hasMessageContaining("nombre");
        assertThatThrownBy(() -> Customer.create(shortLastName, NOW)).hasMessageContaining("apellido");
    }

    @Test
    void updateRecalculatesModificationDate() {
        Customer customer = Customer.create(data(LocalDate.of(1990, 5, 20)), NOW);
        LocalDateTime later = NOW.plusDays(3);

        customer.update(new CustomerData(IdentificationType.CE, "999", "Laura", "Gómez", "laura@mail.com",
                LocalDate.of(1985, 1, 1)), later);

        assertThat(customer.getUpdatedAt()).isEqualTo(later);
        assertThat(customer.getCreatedAt()).isEqualTo(NOW);
        assertThat(customer.getFirstName()).isEqualTo("Laura");
        assertThat(customer.getIdentificationType()).isEqualTo(IdentificationType.CE);
    }

    @Test
    void updateRejectsMinorBirthDate() {
        Customer customer = Customer.create(data(LocalDate.of(1990, 5, 20)), NOW);

        assertThatThrownBy(() -> customer.update(data(NOW.toLocalDate().minusYears(10)), NOW))
                .isInstanceOf(BusinessRuleException.class);
    }
}
