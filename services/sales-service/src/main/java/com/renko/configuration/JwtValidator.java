package com.renko.configuration;

import com.renko.domain.UserRole;
import com.renko.payload.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

public class JwtValidator extends OncePerRequestFilter
{
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException
    {
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

        if(jwt != null && jwt.startsWith(BEARER_PREFIX))
        {
            jwt = jwt.substring(BEARER_PREFIX.length());
            try
            {
                SecretKey key = Keys.hmacShaKeyFor(JwtConstant.JWT_SECRET_KEY.getBytes());
                Claims claims = Jwts.parser()
                                .verifyWith(key)
                                .build()
                                .parseSignedClaims(jwt)
                                .getPayload();

                String email = String.valueOf(claims.get("email"));
                String authorities = String.valueOf(claims.get("authorities"));
                List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);

                UserDto userDto = new UserDto();
                Object userId = claims.get("userId");
                if(userId != null)
                {
                    userDto.setId(Long.valueOf(String.valueOf(userId)));
                }
                userDto.setEmail(email);
                Object storeId = claims.get("storeId");
                if(storeId != null && !"null".equals(String.valueOf(storeId)))
                {
                    userDto.setStoreId(Long.valueOf(String.valueOf(storeId)));
                }
                if(authorities != null && authorities.contains("ROLE_"))
                {
                    for(String part : authorities.split(","))
                    {
                        String p = part.trim();
                        if(p.startsWith("ROLE_"))
                        {
                            try
                            {
                                userDto.setRole(UserRole.valueOf(p.substring(5)));
                                break;
                            }
                            catch(IllegalArgumentException ignored)
                            {
                            }
                        }
                    }
                }

                Authentication auth = new UsernamePasswordAuthenticationToken(email, null, auths);
                ((UsernamePasswordAuthenticationToken) auth).setDetails(userDto);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            catch(Exception e)
            {
                System.out.println("JWT ERROR: " + e.getClass().getName());
                System.out.println("JWT MESSAGE: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
