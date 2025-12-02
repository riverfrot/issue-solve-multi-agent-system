package net.riverfrot.multiagent.chatbot.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    
    /**
     * 특정 세션의 최근 메시지 조회 (최대 3개)
     */
    List<ChatMessage> findTop3BySessionIdOrderByTimestampDesc(String sessionId);
    
    /**
     * 특정 세션의 모든 메시지를 시간순으로 조회
     */
    @Query("SELECT c FROM ChatMessage c WHERE c.sessionId = :sessionId ORDER BY c.timestamp ASC")
    List<ChatMessage> findBySessionIdOrderByTimestamp(@Param("sessionId") String sessionId);
}