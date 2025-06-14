package com.retrip.auth.application.in.request;

import org.springframework.security.crypto.password.PasswordEncoder;

public record LoginRequest (
        String email,
        String password
) {
}
