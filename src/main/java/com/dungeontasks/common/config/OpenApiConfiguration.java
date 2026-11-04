package com.dungeontasks.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI dungeonTasksOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DungeonTasks API")
                        .version("1.0")
                        .description("Authentication and current-user profile API"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    OpenApiCustomizer problemDetailSchemaCustomizer() {
        return openApi -> {
            Schema<?> problemSchema = openApi.getComponents().getSchemas().get("ProblemDetail");
            if (problemSchema == null || problemSchema.getProperties() == null) {
                return;
            }
            problemSchema.getProperties().remove("properties");
            ObjectSchema validationError = new ObjectSchema();
            validationError.addProperty("field", new StringSchema().description("Invalid JSON field name"));
            validationError.addProperty("message", new StringSchema().description("Validation constraint message"));
            validationError.setRequired(java.util.List.of("field", "message"));
            problemSchema.addProperty(
                    "errors",
                    new ArraySchema()
                            .description("Field validation errors; present only for Bean Validation failures")
                            .items(validationError)
            );
            problemSchema.setRequired(java.util.List.of("title", "status", "detail", "instance"));
        };
    }
}
