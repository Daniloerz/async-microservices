package com.bank.account.application.port.out;

import java.util.UUID;

public interface ProcessedEventRepository {

    boolean markAsProcessed(UUID eventId);
}
