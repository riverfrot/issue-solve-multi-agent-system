package net.riverfrot.multiagent.chatbot.application;

import net.riverfrot.multiagent.chatbot.dto.ChatMessageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") 
@Transactional
@DisplayName("ChatbotService getChatHistory 테스트")
class ChatbotServiceTest {
    
    @Autowired
    private ChatbotService chatbotService;
    
    @Test
    @DisplayName("채팅 기록 조회 - 존재하지 않는 세션은 빈 목록 반환")
    void getChatHistory_shouldReturnEmptyListForNonExistentSession() {
        // Given
        String nonExistentSessionId = "non-existent-session-123";
        
        // When
        List<ChatMessageResponse> chatHistory = chatbotService.getChatHistory(nonExistentSessionId);
        
        // Then
        assertNotNull(chatHistory);
        assertTrue(chatHistory.isEmpty());
    }
    
    @Test
    @DisplayName("getChatHistory 메서드 존재 확인")
    void getChatHistory_shouldExistAndReturnList() {
        // Given
        String sessionId = "any-session-id";
        
        // When
        List<ChatMessageResponse> result = chatbotService.getChatHistory(sessionId);
        
        // Then - 메서드가 존재하고 리스트를 반환함
        assertNotNull(result);
        assertTrue(result instanceof List);
    }
}