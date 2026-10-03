package com.ecommerce.sportcenter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * JPA auditing (createdBy/updatedBy trên BaseEntity) + CORS cho frontend nội bộ.
 * Seeder/CLI (không có auth) ghi nhận là "system".
 */
@Configuration
@EnableJpaAuditing
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("system");
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) {
                return Optional.of(ud.getUsername());
            }
            String name = auth.getName();
            return Optional.ofNullable("anonymousUser".equals(name) ? "system" : name);
        };
    }
}
