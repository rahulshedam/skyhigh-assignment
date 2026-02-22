package com.skyhigh.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TemplateService.
 */
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private TemplateService templateService;

    @Test
    void testRenderTemplate_Success() {
        // Given
        String templateName = "email/seat-confirmation";
        Map<String, Object> variables = Map.of(
                "passengerId", "P12345",
                "seatNumber", "12A"
        );
        String expectedHtml = "<html>Rendered Content</html>";

        when(templateEngine.process(eq(templateName), any(Context.class)))
                .thenReturn(expectedHtml);

        // When
        String result = templateService.renderTemplate(templateName, variables);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine, times(1)).process(eq(templateName), any(Context.class));
    }

    @Test
    void testRenderTemplate_WithSingleVariable() {
        // Given
        String templateName = "email/test";
        String variableName = "name";
        String value = "John";
        String expectedHtml = "<html>Hello John</html>";

        when(templateEngine.process(eq(templateName), any(Context.class)))
                .thenReturn(expectedHtml);

        // When
        String result = templateService.renderTemplate(templateName, variableName, value);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine, times(1)).process(eq(templateName), any(Context.class));
    }

    @Test
    void testRenderTemplate_ThrowsException() {
        // Given
        String templateName = "email/invalid";
        Map<String, Object> variables = Map.of("key", "value");

        when(templateEngine.process(eq(templateName), any(Context.class)))
                .thenThrow(new RuntimeException("Template not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
                templateService.renderTemplate(templateName, variables));
    }

    @Test
    void testRenderTemplate_WithEmptyVariables() {
        // Given
        String templateName = "email/simple";
        Map<String, Object> variables = Map.of();
        String expectedHtml = "<html>Simple Content</html>";

        when(templateEngine.process(eq(templateName), any(Context.class)))
                .thenReturn(expectedHtml);

        // When
        String result = templateService.renderTemplate(templateName, variables);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
    }
}
