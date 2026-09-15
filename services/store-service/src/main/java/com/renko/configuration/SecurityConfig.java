package com.renko.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig
{
    private final Environment environment;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            Environment environment,
            @Value("${app.cors.allowed-origins:http://localhost:8080,http://localhost:5173}") String origins)
    {
        this.environment = environment;
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> false == s.isBlank())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");

        return http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth ->
                {
                    auth.requestMatchers("/auth/**").permitAll();
                    auth.requestMatchers("/actuator/health").permitAll();
                    auth.requestMatchers("/api/billing/webhooks/stripe").permitAll();

                    if(isDev)
                    {
                        auth.requestMatchers("/api/dev/reset-demo", "/api/dev/demo-info").permitAll();
                        auth.requestMatchers("/api/dev/clear-db").permitAll();
                        auth.requestMatchers("/api/dev/**").hasAnyRole("ADMIN", "OWNER");
                    }
                    else
                    {
                        auth.requestMatchers("/api/dev/**").denyAll();
                    }

                    auth.requestMatchers("/api/super-admin/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/users/internal").hasAnyRole("OWNER", "ADMIN", "STORE_MANAGER");
                    auth.requestMatchers(HttpMethod.PATCH, "/api/users/*/store").hasAnyRole("OWNER", "ADMIN", "STORE_MANAGER");

                    auth.requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole(
                            "CASHIER", "OWNER", "STORE_MANAGER", "BRANCH_MANAGER", "ADMIN");
                    auth.requestMatchers("/api/reports/**").hasAnyRole(
                            "OWNER", "STORE_MANAGER", "BRANCH_MANAGER", "ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/employees/**").hasAnyRole(
                            "OWNER", "STORE_MANAGER", "ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/products/**", "/api/categories/**",
                                    "/api/inventories/**")
                            .hasAnyRole("OWNER", "STORE_MANAGER", "BRANCH_MANAGER", "ADMIN", "CASHIER");

                    auth.requestMatchers(HttpMethod.DELETE, "/api/stores", "/api/users",
                                    "/api/orders", "/api/products", "/api/categories", "/api/customers",
                                    "/api/branches", "/api/inventories", "/api/refunds", "/api/employees",
                                    "/api/shift-report")
                            .hasAnyRole("ADMIN", "OWNER");

                    auth.requestMatchers("/api/**").authenticated();
                    auth.anyRequest().permitAll();
                })
                .addFilterBefore(
                        new JwtValidator(),
                        BasicAuthenticationFilter.class
                )
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource()
    {
        return request ->
        {
            CorsConfiguration config = new CorsConfiguration();
            java.util.ArrayList<String> patterns = new java.util.ArrayList<>();
            patterns.add("http://localhost:*");
            patterns.add("http://127.0.0.1:*");
            patterns.addAll(allowedOrigins);
            config.setAllowedOriginPatterns(patterns);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
            config.setAllowCredentials(true);
            config.setAllowedHeaders(List.of("*"));
            config.setExposedHeaders(List.of("Authorization"));
            config.setMaxAge(3600L);
            return config;
        };
    }
}
