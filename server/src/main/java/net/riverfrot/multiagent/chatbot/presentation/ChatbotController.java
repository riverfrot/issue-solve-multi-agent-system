package net.riverfrot.multiagent.chatbot.presentation;

import net.riverfrot.multiagent.chatbot.application.ChatbotService;
import net.riverfrot.multiagent.chatbot.dto.ChatRequest;
import net.riverfrot.multiagent.chatbot.dto.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {
    
    private final ChatbotService chatbotService;
    
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }
    
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatbotService.processChat(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}