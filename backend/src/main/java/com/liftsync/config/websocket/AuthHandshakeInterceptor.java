package com.liftsync.config.websocket;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.liftsync.model.chat.ChatRoom;
import com.liftsync.model.user.User;
import com.liftsync.service.chat.ChatService;
import com.liftsync.service.auth.TokenService;
import com.liftsync.service.user.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenService tokenService;
    private final UserService userService;
    private final ChatService chatService;

    public AuthHandshakeInterceptor(TokenService tokenService,
                                    UserService userService,
                                    ChatService chatService) {
        this.chatService = chatService;
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) throws HandshakeFailureException {

        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();

        String token = params.getFirst("token");
        String roomId = params.getFirst("roomId");
        if (token == null || roomId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {

            Long userId = tokenService.verifyAndGetIdFromToken(token);

            User user = userService.findUserById(userId);
            if (user == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            ChatRoom room = chatService.findRoomByPublicId(roomId);

            if (room == null) {
                response.setStatusCode(HttpStatus.NOT_FOUND);
                return false;
            }

            if (!chatService.isParticipantInRoom(user.getId(), room.getId())) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put("userId", userId);
            attributes.put("roomId", room.getId());

            return true;

        } catch (JWTVerificationException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // nothing to do
    }

}
