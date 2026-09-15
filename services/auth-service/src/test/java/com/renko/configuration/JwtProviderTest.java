package com.renko.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtProviderTest
{
    private final JwtProvider jwtProvider = new JwtProvider();

    @Test
    void roundTripsEmailUserIdStoreIdAndRole()
    {
        var auth = new UsernamePasswordAuthenticationToken(
                "cashier@renko.demo",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));

        String token = jwtProvider.generateToken(auth, 2L, 1L);

        assertEquals("cashier@renko.demo", jwtProvider.getEmailFromToken(token));
        assertEquals(2L, jwtProvider.getUserIdFromToken(token));
        assertEquals(1L, jwtProvider.getStoreIdFromToken(token));
        assertTrue(jwtProvider.getAuthoritiesFromToken(token).contains("ROLE_CASHIER"));
    }

    @Test
    void readsClaimsFromBearerPrefixedToken()
    {
        var auth = new UsernamePasswordAuthenticationToken(
                "owner@renko.demo",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
        String token = jwtProvider.generateToken(auth, 1L, 9L);

        assertEquals("owner@renko.demo", jwtProvider.getEmailFromToken("Bearer " + token));
        assertEquals(9L, jwtProvider.getStoreIdFromToken("Bearer " + token));
    }
}
