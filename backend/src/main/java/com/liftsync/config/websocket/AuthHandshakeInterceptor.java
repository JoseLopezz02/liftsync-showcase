// This class is responsible for intercepting WebSocket handshake requests 
// to ensure that only authenticated users can establish a connection to the chat service.
// I decide to get the token and roomId from the query parameters of the WebSocket URL instead of the headers because 
// WebSocket handshake requests do not support custom headers in the same way as regular HTTP requests.,
// which allows the client to pass the necessary information for authentication and room access control.
package com.liftsync.config.websocket;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.liftsync.model.ChatRoom;
import com.liftsync.model.User;
import com.liftsync.service.ChatService;
import com.liftsync.service.TokenService;
import com.liftsync.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
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
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws HandshakeFailureException {

        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();

        String token = params.getFirst("token");
        String roomIdS = params.getFirst("roomId");
        if (token == null || roomIdS == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {

            Long userId = tokenService.verifyAndGetIdFromToken(token);
            Long roomId = Long.valueOf(roomIdS);

            User user = userService.findUserById(userId);
            if (user == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            ChatRoom room = chatService.findRoomById(roomId);

            if (room == null) {
                response.setStatusCode(HttpStatus.NOT_FOUND);
                return false;
            }

            if (!chatService.isParticipantInRoom(user.getId(), room.getId())) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put("userId", userId);
            attributes.put("roomId", roomId);

            return true;

        } catch (JWTVerificationException | NumberFormatException ex) {
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
