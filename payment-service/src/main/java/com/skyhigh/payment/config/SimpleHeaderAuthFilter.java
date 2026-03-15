package com.skyhigh.payment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Simple authentication filter that reads the X-User-Email header.
 * If the header is present and non-blank, the request is treated as authenticated.
 */
@Slf4j
public class SimpleHeaderAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-User-Email";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userEmail = request.getHeader(HEADER_NAME);

        if (userEmail != null && !userEmail.isBlank()) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userEmail,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated request from: {}", userEmail);
        }

        filterChain.doFilter(request, response);
    }
}
