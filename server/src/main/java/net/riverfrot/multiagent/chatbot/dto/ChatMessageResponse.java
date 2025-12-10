package net.riverfrot.multiagent.chatbot.dto;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 응답 DTO
 * 채팅 기록 조회 시 사용
 */
public record ChatMessageResponse(
    String id,
    String sessionId,
    String content,
    String role, // user, assistant
    String agentType, // general, supervisor 등
    LocalDateTime timestamp
) {
    public static ChatMessageResponse from(net.riverfrot.multiagent.chatbot.domain.ChatMessage chatMessage) {
        return new ChatMessageResponse(
            chatMessage.getId(),
            chatMessage.getSessionId(),
            chatMessage.getContent(),
            chatMessage.getRole(), // role은 이미 String
            chatMessage.getAgentType() != null ? chatMessage.getAgentType().getCode() : null,
            chatMessage.getTimestamp() // getTimestamp() 메서드 사용
        );
    }
}