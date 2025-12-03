package net.riverfrot.multiagent.chatbot.application;

import org.springframework.stereotype.Service;

/**
 * AI 응답 Mock 서비스
 * 실제 AI 연동 전까지 사용할 Mock 구현체
 */
@Service
public class AIMockService {
    
    public String generateResponse(String userMessage) {
        // 단순 Mock 응답 생성
        if (userMessage.contains("안녕") || userMessage.contains("hello")) {
            return "안녕하세요! 어떻게 도와드릴까요?";
        }
        
        if (userMessage.contains("코드") || userMessage.contains("프로그래밍")) {
            return "코딩 관련 질문이군요! 어떤 언어나 기술에 대해 궁금하신가요?";
        }
        
        if (userMessage.contains("검색")) {
            return "정보를 검색해드리겠습니다. 잠시만 기다려주세요.";
        }
        
        return "죄송하지만 잘 이해하지 못했습니다. 다시 말씀해 주시겠어요?";
    }
}