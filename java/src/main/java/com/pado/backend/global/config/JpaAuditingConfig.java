package com.pado.backend.global.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // TODO : 현재 작성자, 수정자 : pado-dev
    @Bean
    public AuditorAware<String> auditorProvider(){
        return () -> Optional.of("pado-dev");
    }
}