package com.bank.account.infrastructure.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.bank.account.domain.model.CustomerSnapshot;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerSnapshotEntityMapper {

    CustomerSnapshot toDomain(CustomerSnapshotEntity entity);

    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CustomerSnapshot snapshot, @MappingTarget CustomerSnapshotEntity entity);
}
