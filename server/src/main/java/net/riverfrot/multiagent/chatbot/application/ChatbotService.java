package net.riverfrot.multiagent.chatbot.application;

import net.riverfrot.multiagent.chatbot.domain.AgentType;
import net.riverfrot.multiagent.chatbot.domain.ChatMessage;
import net.riverfrot.multiagent.chatbot.domain.ChatMessageRepository;
import net.riverfrot.multiagent.chatbot.domain.Conversation;
import net.riverfrot.multiagent.chatbot.domain.ConversationRepository;
import net.riverfrot.multiagent.chatbot.dto.ChatRequest;
import net.riverfrot.multiagent.chatbot.dto.ChatResponse;
import net.riverfrot.multiagent.chatbot.dto.StreamingResponse;
import net.riverfrot.multiagent.user.application.UserService;
import net.riverfrot.multiagent.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatbotService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserService userService;
    // python multi agent 붙이기전 테스트 서비스
    private final AIMockService aiMockService;
    private final ObjectMapper objectMapper;
    
    public ChatbotService(ChatMessageRepository chatMessageRepository, 
                         ConversationRepository conversationRepository,
                         UserService userService,
                         AIMockService aiMockService,
                         ObjectMapper objectMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
        this.userService = userService;
        this.aiMockService = aiMockService;
        this.objectMapper = objectMapper;
    }
    

    @Transactional
    public ChatResponse processChat(ChatRequest request) {
        User user = userService.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.userId()));
        
        
        Conversation conversation = getOrCreateConversation(request.sessionId(), user);
    
        ChatMessage userMessage = ChatMessage.createUserMessage(
            request.sessionId(), 
            request.message()
        );
    
        chatMessageRepository.save(userMessage);
        
        String aiResponse = aiMockService.generateResponse(request.message());
        AgentType agentType = AgentType.GENERAL;
        
        ChatMessage assistantMessage = ChatMessage.createAssistantMessage(
            request.sessionId(),
            aiResponse,
            agentType
        );
        chatMessageRepository.save(assistantMessage);
        
        return new ChatResponse(request.sessionId(), aiResponse, agentType.getCode());
    }
    
    /**
     * SSE 기반 스트리밍 채팅 처리
     * Virtual Thread에서 비동기적으로 응답을 분할하여 전송
     */
    public SseEmitter processStreamingChat(String message, String sessionId, String userId) {
        SseEmitter emitter = new SseEmitter(30000L); // 30초 타임아웃
        
        // 비동기 처리를 위한 CompletableFuture 사용 (Virtual Thread)
        CompletableFuture.runAsync(() -> {
            try {
                User user = userService.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                
                
                saveUserMessage(sessionId, message, user);

                // AI 메시지 사용 추후 kafka 이벤트 메시지 스트림 방식으로 변경 필요 
                // 실제 연동전에 
                String aiResponse = aiMockService.generateResponse(message);
                AgentType agentType = AgentType.GENERAL;
                
                List<String> chunks = splitTextForTypingEffect(aiResponse);
                
                for (int i = 0; i < chunks.size(); i++) {
                    StreamingResponse streamingResponse = new StreamingResponse(
                            sessionId,
                            chunks.get(i),
                            agentType.getCode(),
                            i == chunks.size() - 1 // 마지막 청크 여부
                    );
                    
                    emitter.send(SseEmitter.event()
                            .name("chunk")
                            .data(objectMapper.writeValueAsString(streamingResponse)));
                    
                    Thread.sleep(100);
                }
                
                saveAssistantMessage(sessionId, aiResponse, agentType, user);
                
                emitter.complete();
                
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    private List<String> splitTextForTypingEffect(String text) {
        String[] words = text.split(" ");
        List<String> chunks = new ArrayList<>();
        
        for (int i = 0; i < words.length; i++) {
            if (i == words.length - 1) {
                // 마지막 단어는 띄어쓰기 없이
                chunks.add(words[i]);
            } else {
                // 중간 단어들은 뒤에 띄어쓰기 추가
                chunks.add(words[i] + " ");
            }
        }
        
        return chunks;
    }
    
    @Transactional
    private void saveUserMessage(String sessionId, String message, User user) {
        getOrCreateConversation(sessionId, user);
        ChatMessage userMessage = ChatMessage.createUserMessage(sessionId, message);
        chatMessageRepository.save(userMessage);
    }
    
    @Transactional 
    private void saveAssistantMessage(String sessionId, String response, AgentType agentType, User user) {
        ChatMessage assistantMessage = ChatMessage.createAssistantMessage(
                sessionId, response, agentType);
        chatMessageRepository.save(assistantMessage);
    }
    
    private Conversation getOrCreateConversation(String sessionId, User user) {
        return conversationRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Conversation newConversation = Conversation.withSessionId(sessionId, user);
                    return conversationRepository.save(newConversation);
                });
    }
}