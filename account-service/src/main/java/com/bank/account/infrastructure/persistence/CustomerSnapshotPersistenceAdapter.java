package com.bank.account.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.bank.account.application.port.out.CustomerSnapshotRepository;
import com.bank.account.domain.model.CustomerSnapshot;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerSnapshotPersistenceAdapter implements CustomerSnapshotRepository {

    private final CustomerSnapshotJpaRepository customerSnapshotJpaRepository;
    private final CustomerSnapshotEntityMapper customerSnapshotEntityMapper;

    @Override
    public Optional<CustomerSnapshot> findById(UUID customerId) {
        return customerSnapshotJpaRepository.findById(customerId).map(customerSnapshotEntityMapper::toDomain);
    }

    @Override
    public Optional<CustomerSnapshot> findWithSharedLock(UUID customerId) {
        return customerSnapshotJpaRepository.findWithSharedLock(customerId).map(customerSnapshotEntityMapper::toDomain);
    }

    @Override
    public void save(CustomerSnapshot snapshot) {
        CustomerSnapshotEntity entity = customerSnapshotJpaRepository.findById(snapshot.customerId())
                .orElseGet(CustomerSnapshotEntity::new);
        customerSnapshotEntityMapper.updateEntity(snapshot, entity);
        customerSnapshotJpaRepository.saveAndFlush(entity);
    }
}
