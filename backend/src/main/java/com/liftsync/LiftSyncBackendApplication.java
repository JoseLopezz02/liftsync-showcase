package com.liftsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import com.liftsync.config.interceptor.CoachingRelationshipInterceptor;
import com.liftsync.config.security.SecurityPaths;
import com.liftsync.config.interceptor.TokenInterceptor;
import com.liftsync.config.interceptor.UserOwnershipInterceptor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LiftSyncBackendApplication implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;
    private final UserOwnershipInterceptor userOwnershipInterceptor;
    private final CoachingRelationshipInterceptor coachingRelationshipInterceptor;

    public LiftSyncBackendApplication(
            TokenInterceptor tokenInterceptor,
            UserOwnershipInterceptor userOwnershipInterceptor,
            CoachingRelationshipInterceptor coachingRelationshipInterceptor) {

        this.tokenInterceptor = tokenInterceptor;
        this.userOwnershipInterceptor = userOwnershipInterceptor;
        this.coachingRelationshipInterceptor = coachingRelationshipInterceptor;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang"); // e.g. ?lang=es
        return interceptor;
    }

    public static void main(String[] args) {
        SpringApplication.run(LiftSyncBackendApplication.class, args);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns(SecurityPaths.API_PATTERN)
                .excludePathPatterns(SecurityPaths.publicApiPaths());

        registry.addInterceptor(userOwnershipInterceptor)
                .addPathPatterns(SecurityPaths.API_PATTERN)
                .excludePathPatterns(SecurityPaths.publicApiPaths());

        registry.addInterceptor(localeChangeInterceptor());

        // Handled by @RequireCoachingRelationship annotation
        registry.addInterceptor(coachingRelationshipInterceptor);

    }
}