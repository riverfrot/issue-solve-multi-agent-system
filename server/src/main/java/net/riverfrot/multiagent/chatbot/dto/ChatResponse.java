package net.riverfrot.multiagent.chatbot.dto;

/**
 * 채팅 응답 DTO
 * 
 */
public record ChatResponse(
    String sessionId,
    String message,
    String agentType
) {
}