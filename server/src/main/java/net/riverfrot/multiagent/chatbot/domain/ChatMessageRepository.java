package net.riverfrot.multiagent.chatbot.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findTop3BySessionIdOrderByTimestampDesc(String sessionId);
    
    @Query("SELECT c FROM ChatMessage c WHERE c.sessionId = :sessionId ORDER BY c.timestamp ASC")
    List<ChatMessage> findBySessionIdOrderByTimestamp(@Param("sessionId") String sessionId);
}