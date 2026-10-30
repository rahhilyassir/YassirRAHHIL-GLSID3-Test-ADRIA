package com.adria.bank.transaction.service;

import com.adria.bank.transaction.client.AccountClient;
import com.adria.bank.transaction.client.AccountDto;
import com.adria.bank.transaction.config.BankTransactionProperties;
import com.adria.bank.transaction.domain.MerchantCategory;
import com.adria.bank.transaction.domain.Transaction;
import com.adria.bank.transaction.event.TransactionCreatedEvent;
import com.adria.bank.transaction.exception.AccountNotFoundException;
import com.adria.bank.transaction.exception.DownstreamUnavailableException;
import com.adria.bank.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class TransactionService {

    private static final String ACCOUNT_CLIENT = "accountClient";

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final BankTransactionProperties properties;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountClient accountClient,
                              BankTransactionProperties properties) {
        this(transactionRepository, accountClient, properties, event -> {});
    }

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              AccountClient accountClient,
                              BankTransactionProperties properties,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
        this.properties = properties;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public Transaction create(CreateTransactionRequest request) {
        validateRequest(request);

        AccountDto account = fetchAccount(request.accountId());
        validateSourceAccount(account, request.amount(), request.currency());

        Transaction transaction = Transaction.builder()
                .accountId(account.getId())
                .currency(request.currency())
                .amount(request.amount())
                .type("DEBIT")
                .merchantCategory(MerchantCategory.DEBIT)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        try {
            debitAccount(request.accountId(), request.amount());
        } catch (Exception ex) {
            throw new IllegalStateException("Account debit failed, transaction rolled back.", ex);
        }

        applicationEventPublisher.publishEvent(new TransactionCreatedEvent(saved.getId(), saved.getAccountId(), saved.getAmount(), saved.getCurrency(), toInstant(saved)));
        return saved;
    }

    @Transactional
    public List<Transaction> transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String currency) {
        if (fromAccountId == null || toAccountId == null) {
            throw new IllegalArgumentException("Source and target account ids are required.");
        }
        validateAmount(amount, currency);

        AccountDto sourceAccount = fetchAccount(fromAccountId);
        validateSourceAccount(sourceAccount, amount, currency);

        AccountDto targetAccount = fetchAccount(toAccountId);
        if (!"ACTIVE".equalsIgnoreCase(targetAccount.getStatus())) {
            throw new IllegalStateException("Target account is not active.");
        }
        if (!currency.equalsIgnoreCase(targetAccount.getCurrency())) {
            throw new IllegalStateException("Transfer currency does not match target account currency.");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalStateException("Source and target account cannot be the same.");
        }

        Transaction out = Transaction.builder()
                .accountId(fromAccountId)
                .currency(currency)
                .amount(amount)
                .type("TRANSFER_OUT")
                .merchantCategory(MerchantCategory.TRANSFER_OUT)
                .build();

        Transaction in = Transaction.builder()
                .accountId(toAccountId)
                .currency(currency)
                .amount(amount)
                .type("TRANSFER_IN")
                .merchantCategory(MerchantCategory.TRANSFER_IN)
                .build();

        Transaction savedOut = transactionRepository.save(out);
        Transaction savedIn = transactionRepository.save(in);

        try {
            debitAccount(fromAccountId, amount);
            creditAccount(toAccountId, amount);
        } catch (Exception ex) {
            throw new IllegalStateException("Transfer debit/credit failed, transaction rolled back.", ex);
        }

        applicationEventPublisher.publishEvent(new TransactionCreatedEvent(savedOut.getId(), savedOut.getAccountId(), savedOut.getAmount(), savedOut.getCurrency(), toInstant(savedOut)));
        applicationEventPublisher.publishEvent(new TransactionCreatedEvent(savedIn.getId(), savedIn.getAccountId(), savedIn.getAmount(), savedIn.getCurrency(), toInstant(savedIn)));

        return List.of(savedOut, savedIn);
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

    private java.time.Instant toInstant(Transaction transaction) {
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(LocalDateTime.now());
        }
        return transaction.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
    }

    private void validateRequest(CreateTransactionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required.");
        }
        validateAmount(request.amount(), request.currency());
    }

    private void validateAmount(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero.");
        }
        if (amount.compareTo(properties.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("Transaction amount exceeds the allowed maximum.");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required.");
        }
    }

    private void validateSourceAccount(AccountDto account, BigDecimal amount, String currency) {
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException("Account is not active.");
        }
        if (!currency.equalsIgnoreCase(account.getCurrency())) {
            throw new IllegalStateException("Transaction currency does not match account currency.");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance for the requested transaction.");
        }
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

    @CircuitBreaker(name = ACCOUNT_CLIENT, fallbackMethod = "creditAccountFallback")
    @Retry(name = ACCOUNT_CLIENT)
    private AccountDto creditAccount(Long accountId, BigDecimal amount) {
        return accountClient.credit(accountId, new AccountClient.AccountDebitRequest(amount));
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

    private AccountDto creditAccountFallback(Long accountId, BigDecimal amount, Exception ex) {
        throw new DownstreamUnavailableException("Account service unavailable during credit.");
    }
}
