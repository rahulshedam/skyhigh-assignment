package com.skyhigh.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Service for rendering email templates using Thymeleaf.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Render an email template with the provided variables.
     *
     * @param templateName Name of the template (e.g., "email/seat-confirmation")
     * @param variables    Map of variables to be used in the template
     * @return Rendered HTML content as a string
     */
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            
            String renderedContent = templateEngine.process(templateName, context);
            log.debug("Successfully rendered template: {}", templateName);
            
            return renderedContent;
        } catch (Exception e) {
            log.error("Failed to render template: {}", templateName, e);
            throw new RuntimeException("Failed to render email template: " + templateName, e);
        }
    }

    /**
     * Render an email template with single variable.
     *
     * @param templateName Name of the template
     * @param variableName Name of the variable
     * @param value        Value of the variable
     * @return Rendered HTML content as a string
     */
    public String renderTemplate(String templateName, String variableName, Object value) {
        return renderTemplate(templateName, Map.of(variableName, value));
    }
}
