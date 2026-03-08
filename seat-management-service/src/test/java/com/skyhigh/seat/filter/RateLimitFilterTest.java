package com.skyhigh.seat.filter;

import com.skyhigh.seat.service.RateLimitAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private RateLimitAuditService rateLimitAuditService;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(rateLimitAuditService);
    }

    @Test
    void doFilter_FirstRequest_AllowsThrough() throws Exception {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        rateLimitFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_WithinLimit_AllowsThrough() throws Exception {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act - Make 10 requests (well within 50 limit)
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_ExceedsLimit_Returns429() throws Exception {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act - Make 51 requests (exceeds 50 limit)
        for (int i = 0; i < 51; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(50)).doFilter(request, response);
        verify(response, atLeastOnce()).setStatus(429);
        verify(response, atLeastOnce()).setContentType("application/json");
        verify(rateLimitAuditService, atLeastOnce()).recordRateLimitExceeded(eq("127.0.0.1"), any());
    }

    @Test
    void doFilter_DifferentIPs_SeparateLimits() throws Exception {
        // Arrange
        when(request.getRemoteAddr())
                .thenReturn("127.0.0.1")
                .thenReturn("192.168.1.1");

        // Act
        rateLimitFilter.doFilter(request, response, filterChain);
        rateLimitFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, times(2)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_XForwardedForHeader_UsesFirstIP() throws Exception {
        // Arrange
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");

        // Act
        rateLimitFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(request).getHeader("X-Forwarded-For");
    }
}
