package com.ecommerce.sportcenter.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    @Value("${app.url}")
    private String URL;

    @Value("${app.version}")
    private String version;

    private final String bearerFormat = "JWT";
    private final String scheme = "bearer";

    private String NAME = "TOANDC";
    private String EMAIL = "test@gmail.com";
    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl(URL);
        server.setDescription("Production");

        Contact contact = new Contact();
        contact.setName(NAME);
        contact.setEmail(EMAIL);

        Info info = new Info()
                .title("Sport Center API")
                .version(version)
                .description("Sport Center API Documentation")
                .contact(contact);
        return new OpenAPI()
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("Bearer Authentication")
                ).components(
                        new Components()
                                .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()   )
                )
                .info(info)
                .servers(List.of(server));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat(bearerFormat)
                .scheme(scheme);
    }
}
