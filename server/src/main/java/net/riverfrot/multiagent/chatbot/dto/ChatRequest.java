package net.riverfrot.multiagent.chatbot.dto;

/**
 * 채팅 요청 DTO
 */
public record ChatRequest(
    String sessionId,
    String message
) {
}