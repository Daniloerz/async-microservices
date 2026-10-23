package com.bank.account.infrastructure.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bank.account.application.command.CustomerChangeCommand;
import com.bank.account.application.usecase.ApplyCustomerChangeUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEventListener {

    private final CustomerEventParser customerEventParser;
    private final ApplyCustomerChangeUseCase applyCustomerChangeUseCase;

    @KafkaListener(topics = "${app.kafka.customer-events.topic}")
    public void onCustomerEvent(String payload) {
        CustomerChangeCommand command = customerEventParser.parse(payload);
        applyCustomerChangeUseCase.apply(command);
        log.info("Customer event {} ({}) applied", command.eventId(), command.changeType());
    }
}
