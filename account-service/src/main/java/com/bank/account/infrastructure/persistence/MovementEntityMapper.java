package com.bank.account.infrastructure.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.bank.account.domain.model.Movement;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MovementEntityMapper {

    Movement toDomain(MovementEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "movementDate", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MovementEntity toNewEntity(Movement movement);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "movementId", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "movementDate", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(Movement movement, @MappingTarget MovementEntity entity);
}
