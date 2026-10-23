package com.bank.account.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.bank.account.domain.model.CustomerSnapshot;

public interface CustomerSnapshotRepository {

    Optional<CustomerSnapshot> findById(UUID customerId);

    Optional<CustomerSnapshot> findForAccountOpening(UUID customerId);

    void save(CustomerSnapshot snapshot);
}
