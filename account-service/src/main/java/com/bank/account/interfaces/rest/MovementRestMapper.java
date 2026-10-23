package com.bank.account.interfaces.rest;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.bank.account.application.port.out.PageResult;
import com.bank.account.domain.model.Movement;
import com.bank.account.interfaces.rest.dto.MovementResponse;
import com.bank.account.interfaces.rest.dto.PageResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MovementRestMapper {

    MovementResponse toResponse(Movement movement);

    default PageResponse<MovementResponse> toPageResponse(PageResult<Movement> page) {
        PageResult<MovementResponse> responses = page.map(this::toResponse);
        return new PageResponse<>(responses.content(), responses.page(), responses.size(),
                responses.totalElements(), responses.totalPages());
    }
}
