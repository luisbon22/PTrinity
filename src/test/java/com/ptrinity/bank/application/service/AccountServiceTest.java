package com.ptrinity.bank.application.service;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import com.ptrinity.bank.domain.exception.ResourceNotFoundException;
import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.model.AccountStatement;
import com.ptrinity.bank.domain.model.AccountStatus;
import com.ptrinity.bank.domain.model.AccountType;
import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.IdentificationType;
import com.ptrinity.bank.domain.model.MovementType;
import com.ptrinity.bank.domain.model.Transaction;
import com.ptrinity.bank.domain.model.TransactionType;
import com.ptrinity.bank.domain.port.in.OpenAccountCommand;
import com.ptrinity.bank.domain.port.out.AccountNumberGeneratorPort;
import com.ptrinity.bank.domain.port.out.AccountRepositoryPort;
import com.ptrinity.bank.domain.port.out.CustomerRepositoryPort;
import com.ptrinity.bank.domain.port.out.TransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final BigDecimal OVERDRAFT = new BigDecimal("1000000");

    @Mock
    private AccountRepositoryPort accountRepository;
    @Mock
    private CustomerRepositoryPort customerRepository;
    @Mock
    private TransactionRepositoryPort transactionRepository;
    @Mock
    private AccountNumberGeneratorPort numberGenerator;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accountRepository, customerRepository, transactionRepository, numberGenerator,
                CLOCK, OVERDRAFT);
    }

    private Customer customer() {
        return new Customer(1L, IdentificationType.CC, "1020", "Ana", "Pérez", "ana@mail.com",
                LocalDate.of(1990, 1, 1), NOW, null);
    }

    private Account account(String balance) {
        return new Account(10L, AccountType.SAVINGS, "5300000001", AccountStatus.ACTIVE, new BigDecimal(balance),
                BigDecimal.ZERO, true, 1L, NOW.minusDays(1), null);
    }

    @Test
    void openSavingsAccountIsActiveAndUsesGeneratedNumber() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(numberGenerator.nextAccountNumber(AccountType.SAVINGS)).thenReturn("5300000007");
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account account = service.open(new OpenAccountCommand(1L, AccountType.SAVINGS, true));

        assertThat(account.getAccountNumber()).isEqualTo("5300000007");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
        assertThat(account.getOverdraftLimit()).isEqualByComparingTo("0");
    }

    @Test
    void openCheckingAccountGetsConfiguredOverdraft() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(numberGenerator.nextAccountNumber(AccountType.CHECKING)).thenReturn("3300000001");
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account account = service.open(new OpenAccountCommand(1L, AccountType.CHECKING, false));

        assertThat(account.getOverdraftLimit()).isEqualByComparingTo(OVERDRAFT);
        assertThat(account.getAvailableBalance()).isEqualByComparingTo(OVERDRAFT);
    }

    @Test
    void openRequiresExistingCustomer() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.open(new OpenAccountCommand(99L, AccountType.SAVINGS, true)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void changeStatusInactivatesAccount() {
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account("0")));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account account = service.changeStatus(10L, AccountStatus.INACTIVE);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(account.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void cancelFailsWhenBalanceIsNotZero() {
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account("50")));

        assertThatThrownBy(() -> service.changeStatus(10L, AccountStatus.CANCELLED))
                .isInstanceOf(BusinessRuleException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void findAllFiltersByCustomer() {
        when(accountRepository.findByCustomerId(1L)).thenReturn(List.of(account("0")));

        assertThat(service.findAll(1L)).hasSize(1);
        verify(accountRepository, never()).findAll();
    }

    @Test
    void findByIdFailsWhenMissing() {
        when(accountRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void statementSummarizesMovements() {
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account("700")));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(transactionRepository.findByAccountId(10L)).thenReturn(List.of(
                new Transaction(1L, "r1", TransactionType.DEPOSIT, MovementType.CREDIT, 10L, "5300000001",
                        new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("1000"), null, "d", NOW),
                new Transaction(2L, "r2", TransactionType.WITHDRAWAL, MovementType.DEBIT, 10L, "5300000001",
                        new BigDecimal("300"), BigDecimal.ZERO, new BigDecimal("700"), null, "r", NOW)));

        AccountStatement statement = service.getStatement(10L);

        assertThat(statement.totalCredits()).isEqualByComparingTo("1000");
        assertThat(statement.totalDebits()).isEqualByComparingTo("300");
        assertThat(statement.transactions()).hasSize(2);
        assertThat(statement.customer().getId()).isEqualTo(1L);
    }
}
