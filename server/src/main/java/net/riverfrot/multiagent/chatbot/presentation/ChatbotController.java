package net.riverfrot.multiagent.chatbot.presentation;

import net.riverfrot.multiagent.chatbot.application.ChatbotService;
import net.riverfrot.multiagent.chatbot.dto.ChatRequest;
import net.riverfrot.multiagent.chatbot.dto.ChatResponse;
import net.riverfrot.multiagent.chatbot.dto.ChatMessageResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {
    
    private final ChatbotService chatbotService;
    
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }
    
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStream(@RequestParam(required = true) String message, 
                                @RequestParam(required = true) String sessionId,
                                @RequestParam(required = true) String userId) {
        return chatbotService.processStreamingChat(message, sessionId, userId);
    }
    
    /**
     * 채팅 기록 조회
     * 특정 세션의 모든 메시지를 시간순으로 조회
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(@PathVariable String sessionId) {
        List<ChatMessageResponse> chatHistory = chatbotService.getChatHistory(sessionId);
        return ResponseEntity.ok(chatHistory);
    }
}