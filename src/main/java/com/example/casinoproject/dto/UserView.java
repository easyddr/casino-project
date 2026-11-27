package com.example.casinoproject.dto;

import com.example.casinoproject.domain.Role;
import com.example.casinoproject.domain.UserAccount;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UserView(Long id, String username, String email, Role role, BigDecimal balance,
                       boolean blocked, OffsetDateTime createdAt) {

    public static UserView from(UserAccount u) {
        return new UserView(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getBalance(),
                u.isBlocked(), u.getCreatedAt());
    }
}
