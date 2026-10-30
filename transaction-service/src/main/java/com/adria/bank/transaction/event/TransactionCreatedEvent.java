package com.adria.bank.transaction.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID eventId,
        String eventType,
        Long transactionId,
        Long accountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
    public TransactionCreatedEvent(Long transactionId, Long accountId, BigDecimal amount, String currency, Instant occurredAt) {
        this(UUID.randomUUID(), "TransactionCreated", transactionId, accountId, amount, currency, occurredAt);
    }
}
