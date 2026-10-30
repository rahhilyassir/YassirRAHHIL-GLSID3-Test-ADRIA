package com.adria.bank.account.service;

import com.adria.bank.account.domain.Account;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Account create(Account account) {
        if (account.getCustomerId() == null || account.getCustomerId().isBlank()) {
            throw new IllegalArgumentException("Customer id is required.");
        }
        if (account.getCurrency() == null || account.getCurrency().isBlank()) {
            throw new IllegalArgumentException("Currency is required.");
        }
        if (account.getStatus() == null) {
            account.setStatus(AccountStatus.ACTIVE);
        }
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        return accountRepository.save(account);
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Transactional
    public Account updateStatus(Long id, AccountStatus status) {
        Account account = findById(id);
        if (status == null) {
            throw new IllegalArgumentException("Status is required.");
        }
        account.setStatus(status);
        return accountRepository.save(account);
    }

    @Transactional
    public Account debit(Long id, BigDecimal amount) {
        Account account = findById(id);
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new IllegalStateException("Blocked accounts cannot be debited.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than zero.");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance for debit.");
        }
        account.setBalance(account.getBalance().subtract(amount));
        return accountRepository.save(account);
    }

    @Transactional
    public Account credit(Long id, BigDecimal amount) {
        Account account = findById(id);
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new IllegalStateException("Blocked accounts cannot be credited.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero.");
        }
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }
}
