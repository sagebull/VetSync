package com.vetsync.api.dto;

import java.time.Instant;

public record AuthenticationResponse(String accessToken, String tokenType, Instant expiresAt) {
}
