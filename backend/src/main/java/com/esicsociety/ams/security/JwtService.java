package com.esicsociety.ams.security;

import com.esicsociety.ams.config.AppProperties;
import com.esicsociety.ams.member.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Issues and validates stateless HS256 JWTs. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(AppProperties props) {
        byte[] secretBytes = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes. Set a longer JWT_SECRET.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiryMinutes = props.getJwt().getExpiryMinutes();
    }

    public String generateToken(Member member) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim("accountNo", member.getAccountNo())
                .claim("role", member.getRole().name())
                .claim("name", member.getName())
                .claim("mustChangePassword", member.isMustChangePassword())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiryMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
