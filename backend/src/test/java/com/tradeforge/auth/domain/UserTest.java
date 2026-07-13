package com.tradeforge.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User domain — email normalisation and role defaults")
class UserTest {

    @Test
    @DisplayName("email is lower-cased and trimmed on creation")
    void create_rawEmail_normalisedToLowerCase() {
        User user = User.create("Alice", "  ALICE@Example.COM  ", "hash", UserRole.TRADER);
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("default role is TRADER")
    void create_noRoleOverride_defaultIsTrader() {
        User user = User.create("Alice", "alice@example.com", "hash", UserRole.TRADER);
        assertThat(user.getRole()).isEqualTo(UserRole.TRADER);
    }

    @Test
    @DisplayName("admin role is correctly set")
    void create_adminRole_isAdmin() {
        User user = User.create("Admin", "admin@example.com", "hash", UserRole.ADMIN);
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("TRADER is not admin")
    void create_traderRole_isNotAdmin() {
        User user = User.create("Alice", "alice@example.com", "hash", UserRole.TRADER);
        assertThat(user.isAdmin()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Alice@Example.COM", " alice@example.com ", "ALICE@EXAMPLE.COM"})
    @DisplayName("normaliseEmail always returns lowercase trimmed")
    void normaliseEmail_variousInputs_alwaysLowerCased(String raw) {
        String normalised = User.normaliseEmail(raw);
        assertThat(normalised).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("normaliseEmail rejects null")
    void normaliseEmail_null_throwsIllegalArgument() {
        assertThatThrownBy(() -> User.normaliseEmail(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
