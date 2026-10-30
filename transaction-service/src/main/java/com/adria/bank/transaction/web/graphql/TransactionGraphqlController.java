package com.adria.bank.transaction.web.graphql;

import com.adria.bank.transaction.domain.Transaction;
import com.adria.bank.transaction.service.CreateTransactionRequest;
import com.adria.bank.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TransactionGraphqlController {

    private final TransactionService transactionService;

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.findAll();
    }

    @QueryMapping
    public List<Transaction> transactionsByAccount(@Argument("accountId") Long accountId) {
        return transactionService.findByAccountId(accountId);
    }

    @MutationMapping
    public Transaction createTransaction(@Argument("accountId") Long accountId,
                                        @Argument("currency") String currency,
                                        @Argument("amount") BigDecimal amount) {
        return transactionService.create(new CreateTransactionRequest(accountId, currency, amount));
    }

    @MutationMapping
    public List<Transaction> transfer(@Argument("fromAccountId") Long fromAccountId,
                                     @Argument("toAccountId") Long toAccountId,
                                     @Argument("amount") BigDecimal amount,
                                     @Argument("currency") String currency) {
        return transactionService.transfer(fromAccountId, toAccountId, amount, currency);
    }
}
