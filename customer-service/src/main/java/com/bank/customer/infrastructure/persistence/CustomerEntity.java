package com.bank.customer.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
public class CustomerEntity extends PersonEntity {

    @Column(name = "customer_id", nullable = false, unique = true, updatable = false)
    private UUID customerId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private boolean active;
}
