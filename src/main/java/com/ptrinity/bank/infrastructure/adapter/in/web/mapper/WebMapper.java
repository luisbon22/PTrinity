package com.ptrinity.bank.infrastructure.adapter.in.web.mapper;

import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.model.AccountStatement;
import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.CustomerData;
import com.ptrinity.bank.domain.model.Transaction;
import com.ptrinity.bank.domain.port.in.DepositCommand;
import com.ptrinity.bank.domain.port.in.OpenAccountCommand;
import com.ptrinity.bank.domain.port.in.TransferCommand;
import com.ptrinity.bank.domain.port.in.WithdrawalCommand;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.AccountRequest;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.AccountResponse;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.CustomerRequest;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.MovementRequest;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.StatementResponse;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.TransferRequest;

/**
 * Traduce entre los DTOs de la API REST y los objetos del dominio, de modo que
 * el contrato HTTP pueda evolucionar sin afectar el núcleo de la aplicación.
 */
public final class WebMapper {

    private WebMapper() {
    }

    public static CustomerData toData(CustomerRequest request) {
        return new CustomerData(request.identificationType(), request.identificationNumber(), request.firstName(),
                request.lastName(), request.email(), request.birthDate());
    }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getIdentificationType(),
                customer.getIdentificationNumber(), customer.getFirstName(), customer.getLastName(),
                customer.getFirstName() + " " + customer.getLastName(), customer.getEmail(),
                customer.getBirthDate(), customer.getCreatedAt(), customer.getUpdatedAt());
    }

    public static OpenAccountCommand toCommand(AccountRequest request) {
        return new OpenAccountCommand(request.customerId(), request.accountType(), request.isGmfExempt());
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getAccountType(), account.getAccountNumber(),
                account.getStatus(), account.getBalance(), account.getAvailableBalance(),
                account.getOverdraftLimit(), account.isGmfExempt(), account.getCustomerId(),
                account.getCreatedAt(), account.getUpdatedAt());
    }

    public static DepositCommand toDeposit(MovementRequest request) {
        return new DepositCommand(request.accountNumber(), request.amount(), request.description());
    }

    public static WithdrawalCommand toWithdrawal(MovementRequest request) {
        return new WithdrawalCommand(request.accountNumber(), request.amount(), request.description());
    }

    public static TransferCommand toTransfer(TransferRequest request) {
        return new TransferCommand(request.sourceAccountNumber(), request.destinationAccountNumber(),
                request.amount(), request.description());
    }

    public static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(transaction.id(), transaction.reference(), transaction.type(),
                transaction.movement(), transaction.accountId(), transaction.accountNumber(), transaction.amount(),
                transaction.gmfAmount(), transaction.balanceAfter(), transaction.counterpartAccountNumber(),
                transaction.description(), transaction.createdAt());
    }

    public static StatementResponse toResponse(AccountStatement statement) {
        return new StatementResponse(toResponse(statement.account()), toResponse(statement.customer()),
                statement.transactions().stream().map(WebMapper::toResponse).toList(),
                statement.totalCredits(), statement.totalDebits(), statement.totalGmf(), statement.generatedAt());
    }
}
