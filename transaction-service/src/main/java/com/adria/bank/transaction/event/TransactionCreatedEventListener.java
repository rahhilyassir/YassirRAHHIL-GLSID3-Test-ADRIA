package com.adria.bank.transaction.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TransactionCreatedEventListener {

    private static final String TOPIC = "bank.transactions.created";

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.accountId().toString(), event);
    }
}
