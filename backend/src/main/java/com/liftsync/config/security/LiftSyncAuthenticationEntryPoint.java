package com.liftsync.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsync.config.i18n.MessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Component
public class LiftSyncAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MessageResolver messageResolver;
    private final ObjectMapper objectMapper;

    public LiftSyncAuthenticationEntryPoint(MessageResolver messageResolver, ObjectMapper objectMapper) {
        this.messageResolver = messageResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), messageResolver.getMessage("base.unauthorized"));
    }
}

