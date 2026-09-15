package com.renko.configuration;

public final class JwtConstant
{
    public static final String JWT_HEADER = "Authorization";

    /**
     * Prefer JWT_SECRET_KEY env var in non-local environments.
     * Fallback keeps local playground working without extra setup.
     */
    public static final String JWT_SECRET_KEY = System.getenv().getOrDefault(
            "JWT_SECRET_KEY",
            "a-very-long-random-secret-key-that-is-at-least-32-bytes-long"
    );

    private JwtConstant()
    {
    }
}
