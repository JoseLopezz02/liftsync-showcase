package com.liftsync.config.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsync.config.i18n.MessageResolver;
import com.liftsync.dto.response.chat.ChatMessageDTO;
import com.liftsync.model.Enum.MessageType;
import com.liftsync.model.chat.ChatRoom;
import com.liftsync.model.user.User;
import com.liftsync.service.chat.ChatService;
import com.liftsync.service.user.UserService;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    private final ChatService chatService;
    private final UserService userService;
    private final MessageResolver messageResolver;

    public ChatWebSocketHandler(ChatService chatService,
                                UserService userService,
                                MessageResolver messageResolver) {
        this.chatService = chatService;
        this.userService = userService;
        this.messageResolver = messageResolver;
    }


    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        try {

            Long userId = (Long) session.getAttributes().get("userId");
            Long roomId = (Long) session.getAttributes().get("roomId");
            if (userId == null || roomId == null) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason(
                        messageResolver.getMessage("error.websocket.auth.context")));
                return;
            }

            sessions.add(session);
        }catch (Exception e) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason(
                    messageResolver.getMessage("error.websocket.connection")));

        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws IOException {

        String payload = message.getPayload();
        ChatMessageDTO chatMessageDTO = new ObjectMapper()
                .readValue(payload, ChatMessageDTO.class);

        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason(
                    messageResolver.getMessage("error.websocket.auth.context")));
            return;
        }

        User user = userService.findUserById(userId);
        ChatRoom room = chatService.findRoomById(roomId);
        if (user == null || room == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason(
                    messageResolver.getMessage("error.websocket.invalid.room")));
            return;
        }

        if (chatMessageDTO.type().equals(MessageType.CHAT)) {
            chatService.saveMessage(chatMessageDTO, user, room);
        }

        String outJson = new ObjectMapper().writeValueAsString(chatMessageDTO);
        TextMessage outMsg = new TextMessage(outJson);

        for (Iterator<WebSocketSession> it = sessions.iterator(); it.hasNext(); ) {
            WebSocketSession ws = it.next();

            if (!ws.isOpen()) {
                it.remove();
                continue;
            }

            Long theirRoom = (Long) ws.getAttributes().get("roomId");
            if (roomId.equals(theirRoom)) {
                try {
                    ws.sendMessage(outMsg);
                } catch (IOException | IllegalStateException ex) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
                                      @NonNull CloseStatus status) {
        sessions.remove(session);
    }
}