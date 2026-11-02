package com.ptrinity.bank.application.service;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import com.ptrinity.bank.domain.exception.DuplicateResourceException;
import com.ptrinity.bank.domain.exception.ResourceNotFoundException;
import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.CustomerData;
import com.ptrinity.bank.domain.model.IdentificationType;
import com.ptrinity.bank.domain.port.out.AccountRepositoryPort;
import com.ptrinity.bank.domain.port.out.CustomerRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private CustomerRepositoryPort customerRepository;
    @Mock
    private AccountRepositoryPort accountRepository;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, accountRepository, CLOCK);
    }

    private CustomerData data() {
        return new CustomerData(IdentificationType.CC, "1020", "Ana", "Pérez", "ana@mail.com",
                LocalDate.of(1990, 1, 1));
    }

    private Customer existing() {
        return new Customer(1L, IdentificationType.CC, "1020", "Ana", "Pérez", "ana@mail.com",
                LocalDate.of(1990, 1, 1), NOW.minusDays(10), null);
    }

    @Test
    void createSavesCustomerWithCreationDate() {
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer created = service.create(data());

        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        assertThat(created.getUpdatedAt()).isNull();
        verify(customerRepository).existsByIdentification(IdentificationType.CC, "1020", null);
    }

    @Test
    void createRejectsDuplicateIdentification() {
        when(customerRepository.existsByIdentification(eq(IdentificationType.CC), eq("1020"), isNull()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(data())).isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(customerRepository.existsByEmail(anyString(), isNull())).thenReturn(true);

        assertThatThrownBy(() -> service.create(data()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("correo");
    }

    @Test
    void createRejectsMinor() {
        CustomerData minor = new CustomerData(IdentificationType.CC, "1", "Ana", "Pérez", "a@b.co",
                NOW.toLocalDate().minusYears(17));

        assertThatThrownBy(() -> service.create(minor)).isInstanceOf(BusinessRuleException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateSetsModificationDate() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer updated = service.update(1L, new CustomerData(IdentificationType.CC, "1020", "Ana María",
                "Pérez", "ana.maria@mail.com", LocalDate.of(1990, 1, 1)));

        assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
        assertThat(updated.getFirstName()).isEqualTo("Ana María");
        verify(customerRepository).existsByEmail("ana.maria@mail.com", 1L);
    }

    @Test
    void updateFailsWhenCustomerDoesNotExist() {
        when(customerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9L, data())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesCustomerWithoutProducts() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(accountRepository.existsByCustomerId(1L)).thenReturn(false);

        service.delete(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deleteRejectsCustomerWithProducts() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(accountRepository.existsByCustomerId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("productos vinculados");
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    void findAllDelegatesToRepository() {
        when(customerRepository.findAll()).thenReturn(List.of(existing()));

        assertThat(service.findAll()).hasSize(1);
    }
}
