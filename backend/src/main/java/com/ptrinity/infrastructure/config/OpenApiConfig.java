package com.ptrinity.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI ptrinityOpenApi(@Value("${app.ambiente}") String ambiente) {
		return new OpenAPI().info(new Info()
				.title("PTrinity API")
				.version("1.0.0")
				.description("Gestion de clientes, productos financieros y transacciones. Ambiente: " + ambiente)
				.contact(new Contact().name("PTrinity").url("https://github.com/luisbon22/PTrinity")));
	}
}
