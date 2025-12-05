package net.riverfrot.multiagent.chatbot.presentation;

import net.riverfrot.multiagent.chatbot.application.ChatbotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatbotController.class)
@DisplayName("ChatBot SSE Streaming Controller 테스트")
class ChatbotStreamingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatbotService chatbotService;

    @Test
    @DisplayName("SSE 스트리밍 엔드포인트 정상 호출")
    void chatStream_shouldReturnSseEmitter() throws Exception {
        // Given
        String message = "안녕하세요";
        String sessionId = "test-session-123";
        SseEmitter mockEmitter = new SseEmitter();

        when(chatbotService.processStreamingChat(anyString(), anyString()))
                .thenReturn(mockEmitter);

        // When & Then
        mockMvc.perform(get("/chatbot/chat/stream")
                        .param("message", message)
                        .param("sessionId", sessionId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("필수 파라미터 누락시 400 에러")
    void chatStream_shouldReturnBadRequestWhenMissingParameters() throws Exception {
        // When & Then - message 파라미터 누락
        mockMvc.perform(get("/chatbot/chat/stream")
                        .param("sessionId", "test-session"))
                .andExpect(status().isBadRequest());

        // When & Then - sessionId 파라미터 누락
        mockMvc.perform(get("/chatbot/chat/stream")
                        .param("message", "안녕하세요"))
                .andExpect(status().isBadRequest());
    }
}