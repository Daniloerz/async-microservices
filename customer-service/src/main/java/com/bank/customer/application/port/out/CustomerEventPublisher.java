package com.bank.customer.application.port.out;

import com.bank.customer.domain.model.Customer;

public interface CustomerEventPublisher {

    void publish(CustomerEventType eventType, Customer customer);
}
