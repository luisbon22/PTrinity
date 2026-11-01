package com.ptrinity.bank.application.service;

import com.ptrinity.bank.domain.exception.ResourceNotFoundException;
import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.model.AccountStatement;
import com.ptrinity.bank.domain.model.AccountStatus;
import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.port.in.AccountUseCase;
import com.ptrinity.bank.domain.port.in.OpenAccountCommand;
import com.ptrinity.bank.domain.port.out.AccountNumberGeneratorPort;
import com.ptrinity.bank.domain.port.out.AccountRepositoryPort;
import com.ptrinity.bank.domain.port.out.CustomerRepositoryPort;
import com.ptrinity.bank.domain.port.out.TransactionRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementa los casos de uso de productos financieros.
 */
@Service
@Transactional
public class AccountService implements AccountUseCase {

    private final AccountRepositoryPort accountRepository;
    private final CustomerRepositoryPort customerRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final AccountNumberGeneratorPort accountNumberGenerator;
    private final Clock clock;
    private final BigDecimal checkingOverdraftLimit;

    public AccountService(AccountRepositoryPort accountRepository, CustomerRepositoryPort customerRepository,
                          TransactionRepositoryPort transactionRepository,
                          AccountNumberGeneratorPort accountNumberGenerator, Clock clock,
                          @Value("${bank.checking.overdraft-limit:0}") BigDecimal checkingOverdraftLimit) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.clock = clock;
        this.checkingOverdraftLimit = checkingOverdraftLimit;
    }

    @Override
    public Account open(OpenAccountCommand command) {
        Customer customer = customerRepository.findById(command.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", command.customerId()));
        String number = accountNumberGenerator.nextAccountNumber(command.accountType());
        Account account = Account.open(command.accountType(), number, customer.getId(), command.gmfExempt(),
                checkingOverdraftLimit, LocalDateTime.now(clock));
        return accountRepository.save(account);
    }

    @Override
    public Account changeStatus(Long id, AccountStatus status) {
        Account account = accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta", id));
        account.changeStatus(status, LocalDateTime.now(clock));
        return accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Account findById(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cuenta", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> findAll(Long customerId) {
        return customerId == null ? accountRepository.findAll() : accountRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountStatement getStatement(Long id) {
        Account account = findById(id);
        Customer customer = customerRepository.findById(account.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", account.getCustomerId()));
        return AccountStatement.of(account, customer, transactionRepository.findByAccountId(id),
                LocalDateTime.now(clock));
    }
}
