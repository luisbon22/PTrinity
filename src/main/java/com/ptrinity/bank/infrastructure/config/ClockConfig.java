package com.ptrinity.bank.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Expone un {@link Clock} inyectable para que las fechas de creación y
 * modificación se calculen en la zona horaria del negocio y sean testeables.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${bank.zone-id:America/Bogota}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
