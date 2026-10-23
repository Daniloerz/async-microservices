package com.bank.account.interfaces.rest;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.bank.account.application.port.out.PageResult;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountChanges;
import com.bank.account.interfaces.rest.dto.AccountResponse;
import com.bank.account.interfaces.rest.dto.PageResponse;
import com.bank.account.interfaces.rest.dto.PatchAccountRequest;
import com.bank.account.interfaces.rest.dto.UpdateAccountRequest;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountRestMapper {

    AccountChanges toChanges(UpdateAccountRequest request);

    AccountChanges toChanges(PatchAccountRequest request);

    AccountResponse toResponse(Account account);

    default PageResponse<AccountResponse> toPageResponse(PageResult<Account> page) {
        PageResult<AccountResponse> responses = page.map(this::toResponse);
        return new PageResponse<>(responses.content(), responses.page(), responses.size(),
                responses.totalElements(), responses.totalPages());
    }
}
