package com.ptrinity.bank.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ptrinityOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PTrinity - API de entidad financiera")
                .description("Administración de clientes, productos financieros y movimientos")
                .version("1.0.0"));
    }
}
