package com.liftsync.config.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.ttls")
public class CacheTtlProperties {

    private Duration defaultTtl = Duration.ofMinutes(10);
    private Map<String, Duration> entries = new HashMap<>();

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Map<String, Duration> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, Duration> entries) {
        this.entries = entries;
    }
}

