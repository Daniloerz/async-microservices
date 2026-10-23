package com.bank.account.interfaces.rest;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.bank.account.application.report.AccountStatement;
import com.bank.account.application.report.AccountStatementAccount;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.Movement;
import com.bank.account.interfaces.rest.dto.AccountStatementResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReportRestMapper {

    @Mapping(target = "startDate", source = "period.startDate")
    @Mapping(target = "endDate", source = "period.endDate")
    @Mapping(target = "timeZone", expression = "java(statement.period().zone().getId())")
    AccountStatementResponse toResponse(AccountStatement statement);

    AccountStatementResponse.CustomerSummary toCustomerSummary(CustomerSnapshot customer);

    AccountStatementResponse.StatementAccount toStatementAccount(AccountStatementAccount account);

    AccountStatementResponse.StatementMovement toStatementMovement(Movement movement);
}
