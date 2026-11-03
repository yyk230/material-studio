package com.example.studio.auth;

import java.time.Instant;

public record UserDto(long id, String email, String displayName, Instant createdAt) {

    static UserDto from(User user) {
        return new UserDto(user.id, user.email, user.displayName, user.createdAt);
    }
}
