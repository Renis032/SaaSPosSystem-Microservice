package com.renko.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
public class JwtProvider
{
    private static final long JWT_EXPIRATION_TIME = 86400000;
    private final SecretKey key = Keys.hmacShaKeyFor(JwtConstant.JWT_SECRET_KEY.getBytes());

    public String generateToken(Authentication authentication, Long userId, Long storeId)
    {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roles = populateAuthorities(authorities);

        var builder = Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_TIME))
                .claim("email", authentication.getName())
                .claim("authorities", roles)
                .claim("userId", userId);

        if(storeId != null)
        {
            builder.claim("storeId", storeId);
        }

        return builder.signWith(key).compact();
    }

    public String generateToken(Authentication authentication)
    {
        return generateToken(authentication, null, null);
    }

    public String getEmailFromToken(String jwt)
    {
        return String.valueOf(parseClaims(jwt).get("email"));
    }

    public Long getUserIdFromToken(String jwt)
    {
        Object v = parseClaims(jwt).get("userId");
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    public Long getStoreIdFromToken(String jwt)
    {
        Object v = parseClaims(jwt).get("storeId");
        if(v == null || "null".equals(String.valueOf(v)))
        {
            return null;
        }
        return Long.valueOf(String.valueOf(v));
    }

    public String getAuthoritiesFromToken(String jwt)
    {
        return String.valueOf(parseClaims(jwt).get("authorities"));
    }

    public Claims parseClaims(String jwt)
    {
        jwt = removeBearerPrefix(jwt);
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    private String populateAuthorities(Collection<? extends GrantedAuthority> authorities)
    {
        Set<String> auths = new HashSet<>();
        for(GrantedAuthority authority : authorities)
        {
            auths.add(authority.getAuthority());
        }
        return String.join(",", auths);
    }

    private String removeBearerPrefix(String jwt)
    {
        if(jwt != null && jwt.startsWith("Bearer "))
        {
            return jwt.substring(7);
        }
        return jwt;
    }
}
