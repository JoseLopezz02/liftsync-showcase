package com.liftsync.config;

import com.liftsync.util.query.QueryUtil;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.liftsync.util.debug.Debug;
import com.liftsync.util.EnvironmentUtil;

import jakarta.annotation.PostConstruct;

/**
 * Enables {@link Debug} dump-and-die only in development (per {@link EnvironmentUtil}). In any
 * other environment {@code Debug.dd()/d()} stay no-ops, so a forgotten debug call can never leak
 * data in production.
 */
@Configuration
public class DebugConfigurer {

    private final EnvironmentUtil environmentUtil;

    public DebugConfigurer(EnvironmentUtil environmentUtil) {
        this.environmentUtil = environmentUtil;
    }

    @PostConstruct
    void wireDebugFlag() {
        boolean development = environmentUtil.isDevelopment();
        Debug.setEnabled(development);
        QueryUtil.setEnabled(development);
    }

    @Bean
    HibernatePropertiesCustomizer queryTracer() {
        if (!environmentUtil.isDevelopment()) {
            return properties -> {/* no-op */};
        }

        StatementInspector inspector = sql -> {
            if (QueryUtil.isTracing()) {
                QueryUtil.recordQuery(sql);
            }
            return sql; // never rewrite the statement
        };
        return properties -> properties.put(AvailableSettings.STATEMENT_INSPECTOR, inspector);
    }
}
