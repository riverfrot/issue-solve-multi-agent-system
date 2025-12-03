package net.riverfrot.multiagent.chatbot.application;

import net.riverfrot.multiagent.chatbot.domain.AgentType;
import net.riverfrot.multiagent.chatbot.domain.ChatMessage;
import net.riverfrot.multiagent.chatbot.domain.ChatMessageRepository;
import net.riverfrot.multiagent.chatbot.domain.Conversation;
import net.riverfrot.multiagent.chatbot.domain.ConversationRepository;
import net.riverfrot.multiagent.chatbot.dto.ChatRequest;
import net.riverfrot.multiagent.chatbot.dto.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatbotService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    // python multi agent 붙이기전 테스트 서비스
    private final AIMockService aiMockService;
    
    public ChatbotService(ChatMessageRepository chatMessageRepository, 
                         ConversationRepository conversationRepository,
                         AIMockService aiMockService) {
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
        this.aiMockService = aiMockService;
    }
    

    @Transactional
    public ChatResponse processChat(ChatRequest request) {
        Conversation conversation = getOrCreateConversation(request.sessionId());
    
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
    
    private Conversation getOrCreateConversation(String sessionId) {
        return conversationRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Conversation newConversation = Conversation.withSessionId(sessionId, "default-user");
                    return conversationRepository.save(newConversation);
                });
    }
}