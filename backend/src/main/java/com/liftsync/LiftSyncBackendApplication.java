// Here we define the main application class for the LiftSync backend. 
// This class is responsible for bootstrapping the Spring Boot application and configuring the necessary interceptors 
// for handling authentication, user ownership, coaching relationships, 
// and locale changes.
package com.liftsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import com.liftsync.config.interceptor.CoachingRelationshipInterceptor;
import com.liftsync.config.interceptor.TokenInterceptor;
import com.liftsync.config.interceptor.UserOwnershipInterceptor;

@SpringBootApplication
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
        interceptor.setParamName("lang"); // ej: ?lang=es
        return interceptor;
    }

    public static void main(String[] args) {
        SpringApplication.run(LiftSyncBackendApplication.class, args);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/google",
                        "/api/auth/microsoft",
                        "/api/auth/oauth2/redirect",
                        "/api/auth/refresh",
                        "/api/login",
                        "/api/register",
                        "/api/verifyUser",
                        "/api/sendRegisterToken",
                        "/api/resendCode",
                        "/api/dev/**",
                        "/api/getRatings",
                        "/api/user/forgotPassword",
                        "/api/user/resetPassword");

        registry.addInterceptor(userOwnershipInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/google",
                        "/api/auth/microsoft",
                        "/api/auth/oauth2/redirect",
                        "/api/auth/refresh",
                        "/api/login",
                        "/api/sendRegisterToken",
                        "/api/register",
                        "/api/dev/**",
                        "/api/verifyUser",
                        "/api/resendCode",
                        "/api/getRatings",
                        "/api/user/forgotPassword",
                        "/api/user/resetPassword");

        registry.addInterceptor(localeChangeInterceptor());

        registry.addInterceptor(coachingRelationshipInterceptor)
                .addPathPatterns("/api/coaches/**")
                .excludePathPatterns(
                        "/api/auth/google",
                        "/api/auth/microsoft",
                        "/api/auth/oauth2/redirect",
                        "/api/login",
                        "/api/register",
                        "/api/verifyUser",
                        "/api/resendCode",
                        "/api/sendRegisterToken",
                        "/api/getRatings",
                        "/api/coaches/*/acceptAthlete/*",
                        "/api/coaches/*/deleteAthlete/*",
                        "/api/user/forgotPassword",
                        "/api/user/resetPassword");

    }
}