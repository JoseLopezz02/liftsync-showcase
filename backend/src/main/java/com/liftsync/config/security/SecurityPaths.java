package com.liftsync.config.security;

public final class SecurityPaths {

    public static final String API_PATTERN = "/api/**";
    public static final String WS_PATTERN = "/api/ws/**";

    private static final String[] PUBLIC_API_PATHS = {
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
            "/api/user/resetPassword"
    };

    private SecurityPaths() {}

    public static String[] publicApiPaths() {
        return PUBLIC_API_PATHS.clone();
    }
}