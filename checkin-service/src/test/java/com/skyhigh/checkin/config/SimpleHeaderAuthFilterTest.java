package com.skyhigh.checkin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimpleHeaderAuthFilterTest {

    private SimpleHeaderAuthFilter authFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        authFilter = new SimpleHeaderAuthFilter();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithValidEmailHeader_SetsAuthentication() throws ServletException, IOException {
        String email = "user@example.com";
        when(request.getHeader("X-User-Email")).thenReturn(email);

        authFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(email, auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithMissingEmailHeader_DoesNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("X-User-Email")).thenReturn(null);

        authFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithBlankEmailHeader_DoesNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("X-User-Email")).thenReturn("  ");

        authFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);

        verify(filterChain).doFilter(request, response);
    }
}
