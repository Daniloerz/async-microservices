package com.bank.account.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Money {

    static final int SCALE = 2;

    private Money() {
    }

    static BigDecimal of(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
