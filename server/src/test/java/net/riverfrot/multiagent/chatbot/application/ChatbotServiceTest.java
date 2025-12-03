package net.riverfrot.multiagent.chatbot.application;

import net.riverfrot.multiagent.chatbot.domain.ChatMessageRepository;
import net.riverfrot.multiagent.chatbot.domain.ConversationRepository;
import net.riverfrot.multiagent.chatbot.dto.ChatRequest;
import net.riverfrot.multiagent.chatbot.dto.ChatResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") 
@Transactional
@DisplayName("챗봇 서비스 테스트")
class ChatbotServiceTest {
    
    @Autowired
    private ChatbotService chatbotService;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Test
    @DisplayName("채팅 처리 - DB에 메시지 저장 확인")
    void processChat_shouldSaveMessagesToDatabase() {
        // Given
        String sessionId = "test-session-123";
        String userMessage = "안녕하세요!";
        ChatRequest request = new ChatRequest(sessionId, userMessage);
        
        // When
        ChatResponse response = chatbotService.processChat(request);
        
        // Then
        assertNotNull(response);
        assertEquals(sessionId, response.sessionId());
        assertNotNull(response.message());
        assertEquals("general", response.agentType());
        
        String messages = chatMessageRepository.findBySessionIdOrderByTimestamp(sessionId);
        assertEquals(2, messages.size());
        
        assertTrue(conversationRepository.findBySessionId(sessionId).isPresent());
    }
    
    @Test
    @DisplayName("대화 세션 재사용 - 기존 세션에 메시지 추가")
    void processChat_shouldReuseExistingConversation() {
        // Given
        String sessionId = "existing-session";
        
        // 첫 번째 메시지
        ChatRequest firstRequest = new ChatRequest(sessionId, "첫 번째 메시지");
        chatbotService.processChat(firstRequest);
        
        // When - 두 번째 메시지
        ChatRequest secondRequest = new ChatRequest(sessionId, "두 번째 메시지");
        chatbotService.processChat(secondRequest);
        
        // Then
        String messages = chatMessageRepository.findBySessionIdOrderByTimestamp(sessionId);
        assertEquals(4, messages.size());
        
        long conversationCount = conversationRepository.count();
        assertTrue(conversationCount >= 1);
    }
}