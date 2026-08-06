package com.liftsync.config.security;

import com.liftsync.config.i18n.MessageResolver;
import com.liftsync.model.user.User;
import com.liftsync.service.auth.TokenService;
import com.liftsync.service.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final TokenService tokenService;
    private final UserService userService;
    private final MessageResolver messageResolver;

    public JwtAuthenticationFilter(TokenService tokenService, UserService userService, MessageResolver messageResolver) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.messageResolver = messageResolver;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String servletPath = request.getServletPath();

        return Arrays.stream(SecurityPaths.publicApiPaths())
                .anyMatch(publicPath -> PATH_MATCHER.match(publicPath, servletPath));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String jwtToken = tokenService.getTokenFromHeader(authHeader);

        if (jwtToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long userId = tokenService.verifyAndGetIdFromToken(jwtToken);
                User user = userService.findUserById(userId);

                if (user == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            messageResolver.getMessage("token.invalid"));

                    return;
                }

                UsernamePasswdAuthToken authToken =
                        new UsernamePasswdAuthToken(user, null, List.of());

                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, messageResolver.getMessage("token.invalid"));
                return;
            }
        }


        filterChain.doFilter(request, response);
    }

}
