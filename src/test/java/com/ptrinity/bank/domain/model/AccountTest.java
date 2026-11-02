package com.ptrinity.bank.domain.model;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final BigDecimal OVERDRAFT = new BigDecimal("1000000");

    private Account savings(boolean gmfExempt) {
        return Account.open(AccountType.SAVINGS, "5300000001", 1L, gmfExempt, OVERDRAFT, NOW);
    }

    private Account checking() {
        return Account.open(AccountType.CHECKING, "3300000001", 1L, true, OVERDRAFT, NOW);
    }

    @Test
    void openSavingsAccountIsActiveWithZeroBalanceAndNoOverdraft() {
        Account account = savings(true);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getBalance()).isEqualByComparingTo("0");
        assertThat(account.getOverdraftLimit()).isEqualByComparingTo("0");
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void openRejectsNumberWithWrongPrefix() {
        assertThatThrownBy(() -> Account.open(AccountType.SAVINGS, "3300000001", 1L, true, OVERDRAFT, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void openRequiresCustomer() {
        assertThatThrownBy(() -> Account.open(AccountType.SAVINGS, "5300000001", null, true, OVERDRAFT, NOW))
                .hasMessageContaining("cliente");
    }

    @Test
    void formatAccountNumberUsesPrefixAndTenDigits() {
        assertThat(AccountType.SAVINGS.formatAccountNumber(42)).isEqualTo("5300000042");
        assertThat(AccountType.CHECKING.formatAccountNumber(7)).isEqualTo("3300000007");
        assertThatThrownBy(() -> AccountType.SAVINGS.formatAccountNumber(100_000_000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void savingsAccountCannotGoBelowZero() {
        Account account = savings(true);
        account.credit(new BigDecimal("100"), NOW);

        assertThatThrownBy(() -> account.debit(new BigDecimal("100.01"), NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Saldo insuficiente");
        assertThat(account.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    void checkingAccountCanUseOverdraft() {
        Account account = checking();

        account.debit(new BigDecimal("400000"), NOW);

        assertThat(account.getBalance()).isEqualByComparingTo("-400000");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("600000");
    }

    @Test
    void debitChargesGmfWhenNotExempt() {
        Account account = savings(false);
        account.credit(new BigDecimal("10000"), NOW);

        BigDecimal gmf = account.debit(new BigDecimal("1000"), NOW);

        assertThat(gmf).isEqualByComparingTo("4");
        assertThat(account.getBalance()).isEqualByComparingTo("8996");
    }

    @Test
    void inactiveAccountCannotTransact() {
        Account account = savings(true);
        account.inactivate(NOW);

        assertThatThrownBy(() -> account.credit(BigDecimal.TEN, NOW)).hasMessageContaining("no está activa");
    }

    @Test
    void cancelRequiresZeroBalance() {
        Account account = savings(true);
        account.credit(BigDecimal.TEN, NOW);

        assertThatThrownBy(() -> account.cancel(NOW)).hasMessageContaining("saldo igual a $0");

        account.debit(BigDecimal.TEN, NOW);
        account.cancel(NOW);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.CANCELLED);
    }

    @Test
    void cancelledAccountCannotBeReactivated() {
        Account account = savings(true);
        account.cancel(NOW);

        assertThatThrownBy(() -> account.changeStatus(AccountStatus.ACTIVE, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void statusChangesUpdateModificationDate() {
        Account account = savings(true);
        LocalDateTime later = NOW.plusHours(1);

        account.changeStatus(AccountStatus.INACTIVE, later);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(account.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void rejectsNonPositiveOrTooPreciseAmounts() {
        Account account = savings(true);

        assertThatThrownBy(() -> account.credit(BigDecimal.ZERO, NOW)).hasMessageContaining("mayor a cero");
        assertThatThrownBy(() -> account.credit(new BigDecimal("1.001"), NOW)).hasMessageContaining("decimales");
    }
}
