package com.bank.customer.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();

    @Test
    void hashIsBcryptAndMatchesTheRawPassword() {
        String hash = passwordHasher.hash("1234");

        assertThat(hash).startsWith("$2a$").doesNotContain("1234");
        assertThat(new BCryptPasswordEncoder().matches("1234", hash)).isTrue();
    }

    @Test
    void samePasswordGivesDifferentHashes() {
        assertThat(passwordHasher.hash("1234")).isNotEqualTo(passwordHasher.hash("1234"));
    }
}
