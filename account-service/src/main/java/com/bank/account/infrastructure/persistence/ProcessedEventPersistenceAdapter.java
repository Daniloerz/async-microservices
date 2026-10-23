package com.bank.account.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.bank.account.application.port.out.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcessedEventPersistenceAdapter implements ProcessedEventRepository {

    private final ProcessedEventJpaRepository processedEventJpaRepository;

    @Override
    public boolean markAsProcessed(UUID eventId) {
        return processedEventJpaRepository.insertIfAbsent(eventId) == 1;
    }
}
