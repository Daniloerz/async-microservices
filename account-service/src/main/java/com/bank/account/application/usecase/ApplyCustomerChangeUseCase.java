package com.bank.account.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.command.CustomerChangeCommand;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.CustomerSnapshotRepository;
import com.bank.account.application.port.out.ProcessedEventRepository;
import com.bank.account.domain.model.CustomerSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyCustomerChangeUseCase {

    private final ProcessedEventRepository processedEventRepository;
    private final CustomerSnapshotRepository customerSnapshotRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void apply(CustomerChangeCommand command) {
        if (!processedEventRepository.markAsProcessed(command.eventId())) {
            log.info("Customer event {} was already processed, skipping it", command.eventId());
            return;
        }

        boolean stale = customerSnapshotRepository.findById(command.customerId())
                .map(current -> !current.acceptsEventAt(command.occurredAt()))
                .orElse(false);
        if (stale) {
            log.info("Customer event {} is older than the current state, skipping it", command.eventId());
            return;
        }

        boolean active = !command.isDeletion() && command.active();
        customerSnapshotRepository.save(
                new CustomerSnapshot(command.customerId(), command.name(), active, command.occurredAt()));

        if (command.isDeletion()) {
            int deactivated = accountRepository.deactivateAllOfCustomer(command.customerId());
            log.info("Customer {} was deleted, {} account(s) deactivated", command.customerId(), deactivated);
        }
    }
}
