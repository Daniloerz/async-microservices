package com.bank.customer.infrastructure.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.bank.customer.domain.model.Customer;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerEntityMapper {

    Customer toDomain(CustomerEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    CustomerEntity toNewEntity(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(Customer customer, @MappingTarget CustomerEntity entity);
}
