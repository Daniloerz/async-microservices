package com.bank.account.application.exception;

import java.util.UUID;

import lombok.Getter;

@Getter
public class MovementNotFoundException extends RuntimeException {

    private final UUID movementId;

    public MovementNotFoundException(UUID movementId) {
        super("Movement not found");
        this.movementId = movementId;
    }
}
