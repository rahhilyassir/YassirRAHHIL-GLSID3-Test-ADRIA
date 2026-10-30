package com.adria.bank.transaction.service;

import com.adria.bank.transaction.client.AccountClient;
import com.adria.bank.transaction.client.AccountDto;
import com.adria.bank.transaction.config.BankTransactionProperties;
import com.adria.bank.transaction.domain.Transaction;
import com.adria.bank.transaction.exception.AccountNotFoundException;
import com.adria.bank.transaction.exception.DownstreamUnavailableException;
import com.adria.bank.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final String ACCOUNT_CLIENT = "accountClient";

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final BankTransactionProperties properties;

    @Transactional
    public Transaction create(CreateTransactionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required.");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero.");
        }
        if (request.amount().compareTo(properties.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("Transaction amount exceeds the allowed maximum.");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            throw new IllegalArgumentException("Currency is required.");
        }

        AccountDto account = fetchAccount(request.accountId());
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException("Account is not active.");
        }
        if (!request.currency().equalsIgnoreCase(account.getCurrency())) {
            throw new IllegalStateException("Transaction currency does not match account currency.");
        }
        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Insufficient balance for the requested transaction.");
        }

        Transaction transaction = Transaction.builder()
                .accountId(account.getId())
                .currency(request.currency())
                .amount(request.amount())
                .type("DEBIT")
                .build();

        Transaction saved = transactionRepository.save(transaction);

        try {
            debitAccount(request.accountId(), request.amount());
        } catch (Exception ex) {
            throw new IllegalStateException("Account debit failed, transaction rolled back.", ex);
        }

        return saved;
    }

    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with id: " + id));
    }

    public List<Transaction> findByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @CircuitBreaker(name = ACCOUNT_CLIENT, fallbackMethod = "fetchAccountFallback")
    @Retry(name = ACCOUNT_CLIENT)
    private AccountDto fetchAccount(Long accountId) {
        try {
            return accountClient.getAccount(accountId);
        } catch (Exception ex) {
            if (ex instanceof feign.FeignException.NotFound) {
                throw new AccountNotFoundException(accountId);
            }
            throw new DownstreamUnavailableException("Account service unavailable.");
        }
    }

    @CircuitBreaker(name = ACCOUNT_CLIENT, fallbackMethod = "debitAccountFallback")
    @Retry(name = ACCOUNT_CLIENT)
    private AccountDto debitAccount(Long accountId, BigDecimal amount) {
        return accountClient.debit(accountId, new AccountClient.AccountDebitRequest(amount));
    }

    private AccountDto fetchAccountFallback(Long accountId, Exception ex) {
        if (ex instanceof AccountNotFoundException) {
            throw (AccountNotFoundException) ex;
        }
        throw new DownstreamUnavailableException("Account service unavailable.");
    }

    private AccountDto debitAccountFallback(Long accountId, BigDecimal amount, Exception ex) {
        throw new DownstreamUnavailableException("Account service unavailable during debit.");
    }
}
