package com.adria.bank.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class TransactionCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionCreatedEventListener.class);

    @KafkaListener(topics = "bank.transactions.created", groupId = "transaction-audit")
    public void onTransactionCreated(
            @Payload TransactionCreatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {

        log.info("Kafka event received | key={} | eventId={} | eventType={} | transactionId={} | accountId={} | amount={} | currency={} | occurredAt={}",
                key,
                event.eventId(),
                event.eventType(),
                event.transactionId(),
                event.accountId(),
                event.amount(),
                event.currency(),
                event.occurredAt());
    }
}
