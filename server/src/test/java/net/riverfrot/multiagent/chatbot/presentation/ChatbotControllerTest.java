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
    
}