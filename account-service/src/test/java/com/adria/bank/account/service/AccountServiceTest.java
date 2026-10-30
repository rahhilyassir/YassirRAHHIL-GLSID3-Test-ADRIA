package com.adria.bank.account.service;

import com.adria.bank.account.domain.Account;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createShouldPersistAccountWithDefaultActiveStatus() {
        Account account = Account.builder()
                .customerId("CUST-100")
                .currency("EUR")
                .balance(BigDecimal.ZERO)
                .build();

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account saved = accountService.create(account);

        assertNotNull(saved);
        assertEquals(AccountStatus.ACTIVE, saved.getStatus());
        assertEquals("CUST-100", saved.getCustomerId());
        verify(accountRepository).save(account);
    }

    @Test
    void debitShouldRejectInsufficientBalance() {
        Account account = Account.builder()
                .id(1L)
                .customerId("CUST-200")
                .currency("EUR")
                .balance(new BigDecimal("100.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> accountService.debit(1L, new BigDecimal("200.00")));

        assertEquals("Insufficient balance for debit.", exception.getMessage());
    }

    @Test
    void creditShouldRejectBlockedAccount() {
        Account account = Account.builder()
                .id(2L)
                .customerId("CUST-300")
                .currency("EUR")
                .balance(new BigDecimal("150.00"))
                .status(AccountStatus.BLOCKED)
                .build();

        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> accountService.credit(2L, new BigDecimal("50.00")));

        assertEquals("Blocked accounts cannot be credited.", exception.getMessage());
    }

    @Test
    void updateStatusShouldChangeStatus() {
        Account account = Account.builder()
                .id(3L)
                .customerId("CUST-400")
                .currency("EUR")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findById(3L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated = accountService.updateStatus(3L, AccountStatus.BLOCKED);

        assertEquals(AccountStatus.BLOCKED, updated.getStatus());
        verify(accountRepository).save(account);
    }
}
