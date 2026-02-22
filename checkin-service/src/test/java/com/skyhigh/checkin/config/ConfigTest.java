package com.skyhigh.checkin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void appConfig_shouldLoad() {
        AppConfig appConfig = context.getBean(AppConfig.class);
        assertNotNull(appConfig);

        RestTemplate restTemplate = context.getBean(RestTemplate.class);
        assertNotNull(restTemplate);
    }

    @Test
    void securityConfig_shouldLoad() {
        SecurityConfig securityConfig = context.getBean(SecurityConfig.class);
        assertNotNull(securityConfig);

        SecurityFilterChain filterChain = context.getBean(SecurityFilterChain.class);
        assertNotNull(filterChain);
    }
}
