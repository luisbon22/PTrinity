package com.ptrinity.bank.application.service;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import com.ptrinity.bank.domain.exception.ResourceNotFoundException;
import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.model.Transaction;
import com.ptrinity.bank.domain.model.TransactionType;
import com.ptrinity.bank.domain.port.in.DepositCommand;
import com.ptrinity.bank.domain.port.in.TransactionUseCase;
import com.ptrinity.bank.domain.port.in.TransferCommand;
import com.ptrinity.bank.domain.port.in.WithdrawalCommand;
import com.ptrinity.bank.domain.port.out.AccountRepositoryPort;
import com.ptrinity.bank.domain.port.out.TransactionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementa los movimientos financieros. Cada operación se ejecuta en una única
 * transacción de base de datos (atomicidad) y bloquea las cuentas involucradas
 * (aislamiento), de modo que el saldo y los movimientos siempre quedan consistentes.
 */
@Service
@Transactional
public class TransactionService implements TransactionUseCase {

    private final AccountRepositoryPort accountRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final Clock clock;

    public TransactionService(AccountRepositoryPort accountRepository,
                              TransactionRepositoryPort transactionRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Override
    public Transaction deposit(DepositCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Account account = lock(command.accountNumber());
        account.credit(command.amount(), now);
        accountRepository.save(account);
        return transactionRepository.save(Transaction.credit(newReference(), TransactionType.DEPOSIT, account,
                command.amount(), null, describe(command.description(), "Consignación"), now));
    }

    @Override
    public Transaction withdraw(WithdrawalCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Account account = lock(command.accountNumber());
        BigDecimal gmf = account.debit(command.amount(), now);
        accountRepository.save(account);
        return transactionRepository.save(Transaction.debit(newReference(), TransactionType.WITHDRAWAL, account,
                command.amount(), gmf, null, describe(command.description(), "Retiro"), now));
    }

    @Override
    public List<Transaction> transfer(TransferCommand command) {
        String sourceNumber = normalize(command.sourceAccountNumber());
        String destinationNumber = normalize(command.destinationAccountNumber());
        if (sourceNumber.equals(destinationNumber)) {
            throw new BusinessRuleException("La cuenta de origen y destino deben ser diferentes");
        }
        LocalDateTime now = LocalDateTime.now(clock);

        // Se bloquean siempre en el mismo orden para evitar interbloqueos entre transferencias cruzadas
        boolean sourceFirst = sourceNumber.compareTo(destinationNumber) < 0;
        Account first = lock(sourceFirst ? sourceNumber : destinationNumber);
        Account second = lock(sourceFirst ? destinationNumber : sourceNumber);
        Account source = sourceFirst ? first : second;
        Account destination = sourceFirst ? second : first;

        BigDecimal gmf = source.debit(command.amount(), now);
        destination.credit(command.amount(), now);
        accountRepository.save(source);
        accountRepository.save(destination);

        String reference = newReference();
        String description = describe(command.description(), "Transferencia");
        Transaction debit = transactionRepository.save(Transaction.debit(reference, TransactionType.TRANSFER, source,
                command.amount(), gmf, destination.getAccountNumber(), description, now));
        Transaction credit = transactionRepository.save(Transaction.credit(reference, TransactionType.TRANSFER,
                destination, command.amount(), source.getAccountNumber(), description, now));
        return List.of(debit, credit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findAll(Long accountId) {
        if (accountId == null) {
            return transactionRepository.findAll();
        }
        accountRepository.findById(accountId).orElseThrow(() -> new ResourceNotFoundException("Cuenta", accountId));
        return transactionRepository.findByAccountId(accountId);
    }

    private Account lock(String accountNumber) {
        String number = normalize(accountNumber);
        return accountRepository.findByAccountNumberForUpdate(number)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta", number));
    }

    private static String normalize(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BusinessRuleException("El número de cuenta es obligatorio");
        }
        return accountNumber.trim();
    }

    private static String describe(String description, String fallback) {
        return description == null || description.isBlank() ? fallback : description.trim();
    }

    private static String newReference() {
        return UUID.randomUUID().toString();
    }
}
