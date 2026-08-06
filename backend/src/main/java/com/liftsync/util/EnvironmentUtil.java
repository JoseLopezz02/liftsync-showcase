package com.liftsync.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentUtil {

    @Value("${app.domain}")
    private String appDomain;

    @Value("${env.frontend.allowed-origins}")
    private String frontendAllowedOrigins;

    @Value("${env.frontend.email-base-url}")
    private String frontendUrl;

    public boolean isProduction() {
        return appDomain != null && appDomain.contains("https://liftsync.es");
    }

    public boolean isDevelopment() {
        return appDomain != null && (appDomain.contains("localhost") || appDomain.contains("127.0.0.1"));
    }

    public String getCurrentEnvironment() {
        if (isProduction()) {
            return "production";
        } else if (isDevelopment()) {
            return "development";
        } else {
            return "unknown";
        }
    }

    public String getAppDomain() {
        return appDomain;
    }

    /**
     * Returns the base URL for email links (password recovery, invites, etc.).
     * This is the URL the user accesses in the browser, which may differ from
     * the CORS allowed origins (e.g., proxy URL vs Vite dev server).
     */
    public String getFrontendUrl() {
        return frontendUrl;
    }

    public String getFrontendAllowedOrigins() {
        return frontendAllowedOrigins;
    }
}

