package net.riverfrot.multiagent.chatbot.dto;

/**
 * 채팅 응답 DTO
 * 
 */

// 스트리밍 채팅 메시지 DTO
public record StreamingResponse(
    String sessionId,
    String chunk,
    String agentType,
    boolean isLast
) {}