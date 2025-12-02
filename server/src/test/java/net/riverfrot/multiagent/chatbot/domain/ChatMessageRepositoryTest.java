package net.riverfrot.multiagent.chatbot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("채팅 메시지 Repository 테스트")
class ChatMessageRepositoryTest {
    
    @Autowired
    private ChatMessageRepository repository;
    
    @Test
    @DisplayName("메시지 저장 - 필수 필드 누락 시 예외 발생")
    void save_shouldThrowExceptionForInvalidMessage() {
        // Given - content가 빈 메시지 (ChatMessage 내부 검증 로직 트리거)
        
        // When & Then
        assertThrows(
            IllegalArgumentException.class,
            () -> ChatMessage.builder()
                    .sessionId("test-session")
                    .role("user")
                    .content("") // 빈 content로 검증 실패 유도
                    .build()
        );
    }
}