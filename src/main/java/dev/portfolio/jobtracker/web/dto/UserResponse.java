package dev.portfolio.jobtracker.web.dto;

import dev.portfolio.jobtracker.security.AuthUser;
import dev.portfolio.jobtracker.user.AppUser;

public record UserResponse(long id, String email, String displayName) {

    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName());
    }

    public static UserResponse from(AuthUser u) {
        return new UserResponse(u.id(), u.email(), u.displayName());
    }
}
