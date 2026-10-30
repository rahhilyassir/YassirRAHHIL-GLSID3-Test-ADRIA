package com.adria.bank.transaction.mcp;

import com.adria.bank.transaction.domain.Transaction;
import com.adria.bank.transaction.service.CreateTransactionRequest;
import com.adria.bank.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionMcpToolService {

    private final TransactionService transactionService;

    @Tool(description = "List all transactions recorded in the transaction service.")
    public List<Transaction> listTransactions() {
        return transactionService.findAll();
    }

    @Tool(description = "List all transactions for a specific account identifier.")
    public List<Transaction> getTransactionsByAccount(
            @ToolParam(description = "The account identifier whose transactions should be listed.") Long accountId) {
        return transactionService.findByAccountId(accountId);
    }

    @Tool(description = "Create a new debit transaction for an account after validating the account state and balance.")
    public Transaction createTransaction(
            @ToolParam(description = "The account identifier for the new transaction.") Long accountId,
            @ToolParam(description = "The currency code used for the transaction, such as EUR or USD.") String currency,
            @ToolParam(description = "The amount of the transaction to create.") BigDecimal amount) {
        return transactionService.create(new CreateTransactionRequest(accountId, currency, amount));
    }
}
