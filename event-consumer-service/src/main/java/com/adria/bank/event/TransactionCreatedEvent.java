package com.adria.bank.event;

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
}
