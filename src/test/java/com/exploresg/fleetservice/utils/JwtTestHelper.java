package com.exploresg.fleetservice.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.*;

@Component
public class JwtTestHelper {
    private final SecretKeySpec secretKey;

    public JwtTestHelper(@Value("${application.security.jwt.secret-key}") String jwtSecret) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateToken(String username, String... roles) {
        Map<String, Object> claims = new HashMap<>();

        // Add roles as your production code does
        List<String> rolesList = Arrays.asList(roles);
        claims.put("roles", rolesList);

        // Add default user details
        claims.put("givenName", "Test");
        claims.put("familyName", "User");
        claims.put("picture", "https://example.com/avatar.jpg");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAdminToken(String username) {
        return generateToken(username, "ROLE_ADMIN");
    }

    public String generateTokenWithRoles(String subject, String... roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("scope", String.join(" ", roles));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(this.secretKey)
                .compact();
    }
}
