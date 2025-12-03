package net.riverfrot.multiagent.chatbot.presentation;

import net.riverfrot.multiagent.chatbot.application.ChatbotService;
import net.riverfrot.multiagent.chatbot.dto.ChatRequest;
import net.riverfrot.multiagent.chatbot.dto.ChatResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatbotController.class)
@DisplayName("챗봇 Controller 테스트")
class ChatbotControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ChatbotService chatbotService;
    
    @Test
    @DisplayName("채팅 API 호출 - 정상 처리")
    void chat_shouldReturnResponse() throws Exception {
        // Given
        ChatResponse mockResponse = new ChatResponse(
            "test-session", 
            "안녕하세요! 어떻게 도와드릴까요?", 
            "general"
        );
        when(chatbotService.processChat(any(ChatRequest.class))).thenReturn(mockResponse);
        
        String requestBody = """
            {
                "sessionId": "test-session",
                "message": "안녕하세요"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/chatbot/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").value("test-session"))
                .andExpect(jsonPath("$.message").value("안녕하세요! 어떻게 도와드릴까요?"))
                .andExpect(jsonPath("$.agentType").value("general"));
    }
    
    @Test
    @DisplayName("Health check API 호출")
    void health_shouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/chatbot/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
    
    @Test
    @DisplayName("잘못된 Content-Type 처리")
    void chat_shouldHandleInvalidContentType() throws Exception {
        // When & Then
        mockMvc.perform(post("/chatbot/chat")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid request"))
                .andExpect(status().isUnsupportedMediaType());
    }
}