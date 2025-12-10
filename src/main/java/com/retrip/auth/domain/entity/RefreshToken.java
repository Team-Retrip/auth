package com.retrip.auth.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @Column(columnDefinition = "varbinary(16)")
    private UUID id;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, unique = true)
    private String tokenValue;

    @Column(nullable = false)
    private long expiration;

    public static RefreshToken create(UUID memberId, String tokenValue, long expiration) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .memberId(memberId)
                .tokenValue(tokenValue)
                .expiration(expiration)
                .build();
    }

    public void rotate(String newTokenValue, long newExpiration) {
        this.tokenValue = newTokenValue;
        this.expiration = newExpiration;
    }
}