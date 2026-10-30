package com.adria.bank.transaction.service;

import com.adria.bank.transaction.client.AccountClient;
import com.adria.bank.transaction.client.AccountDto;
import com.adria.bank.transaction.config.BankTransactionProperties;
import com.adria.bank.transaction.domain.Transaction;
import com.adria.bank.transaction.repository.TransactionRepository;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private TransactionService transactionService;

    private final BankTransactionProperties properties = new BankTransactionProperties();

    @Test
    void createShouldRejectAmountAboveMax() {
        properties.setMaxAmount(new BigDecimal("1000"));

        TransactionService service = new TransactionService(transactionRepository, accountClient, properties);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateTransactionRequest(1L, "EUR", new BigDecimal("1001"))));

        assertEquals("Transaction amount exceeds the allowed maximum.", ex.getMessage());
    }

    @Test
    void createShouldRejectInactiveAccount() {
        properties.setMaxAmount(new BigDecimal("1000"));
        TransactionService service = new TransactionService(transactionRepository, accountClient, properties);
        when(accountClient.getAccount(1L)).thenReturn(AccountDto.builder().id(1L).currency("EUR").balance(new BigDecimal("500")).status("BLOCKED").build());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(new CreateTransactionRequest(1L, "EUR", new BigDecimal("100"))));

        assertEquals("Account is not active.", ex.getMessage());
    }

    @Test
    void createShouldRejectCurrencyMismatch() {
        properties.setMaxAmount(new BigDecimal("1000"));
        TransactionService service = new TransactionService(transactionRepository, accountClient, properties);
        when(accountClient.getAccount(1L)).thenReturn(AccountDto.builder().id(1L).currency("USD").balance(new BigDecimal("500")).status("ACTIVE").build());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(new CreateTransactionRequest(1L, "EUR", new BigDecimal("100"))));

        assertEquals("Transaction currency does not match account currency.", ex.getMessage());
    }

    @Test
    void createShouldRejectInsufficientBalance() {
        properties.setMaxAmount(new BigDecimal("1000"));
        TransactionService service = new TransactionService(transactionRepository, accountClient, properties);
        when(accountClient.getAccount(1L)).thenReturn(AccountDto.builder().id(1L).currency("EUR").balance(new BigDecimal("50")).status("ACTIVE").build());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(new CreateTransactionRequest(1L, "EUR", new BigDecimal("100"))));

        assertEquals("Insufficient balance for the requested transaction.", ex.getMessage());
    }

    @Test
    void createShouldSaveTransactionAndDebitAccount() {
        properties.setMaxAmount(new BigDecimal("1000"));
        TransactionService service = new TransactionService(transactionRepository, accountClient, properties);
        AccountDto account = AccountDto.builder().id(1L).currency("EUR").balance(new BigDecimal("500")).status("ACTIVE").build();
        when(accountClient.getAccount(1L)).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });
        when(accountClient.debit(eq(1L), any(AccountClient.AccountDebitRequest.class))).thenReturn(account);

        Transaction created = service.create(new CreateTransactionRequest(1L, "EUR", new BigDecimal("100")));

        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("DEBIT", created.getType());
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountClient).debit(eq(1L), any(AccountClient.AccountDebitRequest.class));
    }
}
