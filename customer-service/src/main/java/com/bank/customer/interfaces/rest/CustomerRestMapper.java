package com.bank.customer.interfaces.rest;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.bank.customer.application.port.out.PageResult;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.domain.model.CustomerChanges;
import com.bank.customer.domain.model.CustomerDetails;
import com.bank.customer.interfaces.rest.dto.CreateCustomerRequest;
import com.bank.customer.interfaces.rest.dto.CustomerResponse;
import com.bank.customer.interfaces.rest.dto.PageResponse;
import com.bank.customer.interfaces.rest.dto.PatchCustomerRequest;
import com.bank.customer.interfaces.rest.dto.UpdateCustomerRequest;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerRestMapper {

    CustomerDetails toDetails(CreateCustomerRequest request);

    CustomerChanges toChanges(UpdateCustomerRequest request);

    CustomerChanges toChanges(PatchCustomerRequest request);

    CustomerResponse toResponse(Customer customer);

    default PageResponse<CustomerResponse> toPageResponse(PageResult<Customer> page) {
        PageResult<CustomerResponse> responses = page.map(this::toResponse);
        return new PageResponse<>(responses.content(), responses.page(), responses.size(),
                responses.totalElements(), responses.totalPages());
    }
}
