package com.renko.service.impl;

import com.renko.entities.UserEntity;
import com.renko.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService
{
    @Autowired // Injects the UserRepository bean managed by Spring
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException
    {
        UserEntity userEntity = userRepository.findByEmail(email);
        if(userEntity == null)
        {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        // Spring hasRole("X") expects authority "ROLE_X"
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userEntity.getRole().name());

        Collection<GrantedAuthority> authorities = Collections.singletonList(authority);

        // Return Spring Security's built-in UserDetails implementation
        // Spring uses this object to authenticate the user and check permissions
        return new org.springframework.security.core.userdetails.User(
                userEntity.getEmail(),
                userEntity.getPassword(),
                authorities);
    }
}
