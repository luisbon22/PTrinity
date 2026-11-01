package com.ptrinity.bank.application.service;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import com.ptrinity.bank.domain.exception.ResourceNotFoundException;
import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.model.AccountStatus;
import com.ptrinity.bank.domain.model.AccountType;
import com.ptrinity.bank.domain.model.MovementType;
import com.ptrinity.bank.domain.model.Transaction;
import com.ptrinity.bank.domain.model.TransactionType;
import com.ptrinity.bank.domain.port.in.DepositCommand;
import com.ptrinity.bank.domain.port.in.TransferCommand;
import com.ptrinity.bank.domain.port.in.WithdrawalCommand;
import com.ptrinity.bank.domain.port.out.AccountRepositoryPort;
import com.ptrinity.bank.domain.port.out.TransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private AccountRepositoryPort accountRepository;
    @Mock
    private TransactionRepositoryPort transactionRepository;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(accountRepository, transactionRepository, CLOCK);
        lenient().when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Account savings(long id, String number, String balance, boolean gmfExempt) {
        return new Account(id, AccountType.SAVINGS, number, AccountStatus.ACTIVE, new BigDecimal(balance),
                BigDecimal.ZERO, gmfExempt, 1L, NOW, null);
    }

    private void stubLock(Account account) {
        when(accountRepository.findByAccountNumberForUpdate(account.getAccountNumber()))
                .thenReturn(Optional.of(account));
    }

    @Test
    void depositIncreasesBalanceAndRegistersCredit() {
        Account account = savings(1L, "5300000001", "100", true);
        stubLock(account);

        Transaction tx = service.deposit(new DepositCommand("5300000001", new BigDecimal("50"), null));

        assertThat(account.getBalance()).isEqualByComparingTo("150");
        assertThat(tx.movement()).isEqualTo(MovementType.CREDIT);
        assertThat(tx.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.balanceAfter()).isEqualByComparingTo("150");
        assertThat(tx.description()).isEqualTo("Consignación");
        verify(accountRepository).save(account);
    }

    @Test
    void depositFailsForUnknownAccount() {
        when(accountRepository.findByAccountNumberForUpdate("5399999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deposit(new DepositCommand("5399999999", BigDecimal.TEN, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void withdrawalChargesGmfWhenNotExempt() {
        Account account = savings(1L, "5300000001", "10000", false);
        stubLock(account);

        Transaction tx = service.withdraw(new WithdrawalCommand("5300000001", new BigDecimal("1000"), "Cajero"));

        assertThat(tx.gmfAmount()).isEqualByComparingTo("4");
        assertThat(account.getBalance()).isEqualByComparingTo("8996");
        assertThat(tx.movement()).isEqualTo(MovementType.DEBIT);
    }

    @Test
    void withdrawalFailsWithInsufficientFundsAndDoesNotPersist() {
        Account account = savings(1L, "5300000001", "100", true);
        stubLock(account);

        assertThatThrownBy(() -> service.withdraw(new WithdrawalCommand("5300000001", new BigDecimal("200"), null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferGeneratesDebitAndCreditWithSameReference() {
        Account source = savings(1L, "5300000002", "1000", true);
        Account destination = savings(2L, "5300000001", "0", true);
        stubLock(source);
        stubLock(destination);

        List<Transaction> result = service.transfer(
                new TransferCommand("5300000002", "5300000001", new BigDecimal("400"), null));

        assertThat(result).hasSize(2);
        Transaction debit = result.get(0);
        Transaction credit = result.get(1);
        assertThat(debit.movement()).isEqualTo(MovementType.DEBIT);
        assertThat(debit.accountId()).isEqualTo(1L);
        assertThat(debit.counterpartAccountNumber()).isEqualTo("5300000001");
        assertThat(credit.movement()).isEqualTo(MovementType.CREDIT);
        assertThat(credit.accountId()).isEqualTo(2L);
        assertThat(debit.reference()).isEqualTo(credit.reference());
        assertThat(source.getBalance()).isEqualByComparingTo("600");
        assertThat(destination.getBalance()).isEqualByComparingTo("400");
    }

    @Test
    void transferLocksAccountsInDeterministicOrder() {
        Account source = savings(1L, "5300000002", "1000", true);
        Account destination = savings(2L, "5300000001", "0", true);
        stubLock(source);
        stubLock(destination);

        service.transfer(new TransferCommand("5300000002", "5300000001", BigDecimal.TEN, null));

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByAccountNumberForUpdate("5300000001");
        order.verify(accountRepository).findByAccountNumberForUpdate("5300000002");
    }

    @Test
    void transferToSameAccountIsRejected() {
        assertThatThrownBy(() -> service.transfer(
                new TransferCommand("5300000001", "5300000001", BigDecimal.TEN, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void transferToUnknownAccountIsRejected() {
        when(accountRepository.findByAccountNumberForUpdate("5300000001"))
                .thenReturn(Optional.of(savings(1L, "5300000001", "100", true)));
        when(accountRepository.findByAccountNumberForUpdate("5399999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(
                new TransferCommand("5300000001", "5399999999", BigDecimal.TEN, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void findAllByAccountRequiresExistingAccount() {
        when(accountRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAll(7L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllWithoutFilterReturnsEverything() {
        when(transactionRepository.findAll()).thenReturn(List.of());

        assertThat(service.findAll(null)).isEmpty();
    }
}
