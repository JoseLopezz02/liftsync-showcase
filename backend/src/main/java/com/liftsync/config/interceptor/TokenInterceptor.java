package com.liftsync.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.liftsync.dto.response.auth.AuthUserProjection;
import com.liftsync.service.auth.TokenService;
import com.liftsync.service.user.UserService;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final UserService userService;

    public TokenInterceptor(TokenService tokenService,
                            UserService userService) {

        this.tokenService = tokenService;
        this.userService = userService;
        
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (request.getMethod().equals("OPTIONS")) {
            return true; // Allow preflight requests
        }

        String strHeader = request.getHeader("Authorization");

        String token = tokenService.getTokenFromHeader(strHeader);
        if (token == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
            return false;
        }

        try {
            Long id = tokenService.verifyAndGetIdFromToken(token);
            AuthUserProjection user = userService.findAuthUserById(id);

            request.setAttribute("userId", id);                    // internal Long id (for services)
            request.setAttribute("userPublicId", user.publicId()); // String publicId (for URL ownership checks)
            request.setAttribute("userRole", user.role());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return false;
        }

        return true;
    }
}