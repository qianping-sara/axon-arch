package com.atam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) Configuration
 * 
 * Provides API documentation at /swagger-ui.html
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI atamCopilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ATAM Copilot API")
                        .description("AI-assisted Architecture Tradeoff Analysis Method (ATAM) evaluation platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ATAM Copilot Team")
                                .email("team@atam-copilot.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

}

