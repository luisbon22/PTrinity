package com.ptrinity.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Un solo reloj inyectable: las fechas automaticas son deterministas en las pruebas. */
@Configuration
public class RelojConfig {

	@Bean
	Clock reloj(@Value("${app.zona-horaria}") String zona) {
		return Clock.system(ZoneId.of(zona));
	}
}
