package net.riverfrot.multiagent.chatbot.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 챗봇 대화 세션을 관리하는 도메인 엔티티입니다.
 */
@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversation_user_started", columnList = "userId, startedAt"),
    @Index(name = "idx_conversation_started_at", columnList = "startedAt")
})
public class Conversation {

    private static final int MAX_MESSAGES_DEFAULT = 100;

    @Id
    @Column(name = "session_id", length = 36)
    private final String sessionId;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "session_id")
    @OrderBy("timestamp ASC")
    private final List<ChatMessage> messages;
    
    @Column(name = "user_id", nullable = false, length = 100)
    private final String userId;
    
    @Column(name = "started_at", nullable = false)
    private final LocalDateTime startedAt;

    // 기본 생성자 (JPA 호환)
    protected Conversation() {
        this.sessionId = UUID.randomUUID().toString();
        this.messages = new ArrayList<>();
        this.userId = null;
        this.startedAt = LocalDateTime.now();
    }

    private Conversation(Builder builder) {
        this.sessionId = builder.sessionId != null ? builder.sessionId : UUID.randomUUID().toString();
        this.messages = new ArrayList<>(); // 빈 리스트로 시작
        this.userId = builder.userId;
        this.startedAt = builder.startedAt != null ? builder.startedAt : LocalDateTime.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public String getUserId() {
        return userId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void addMessage(ChatMessage message) {
        Objects.requireNonNull(message, "Message cannot be null");
        
        if (!this.sessionId.equals(message.getSessionId())) {
            throw new IllegalArgumentException(
                String.format("Session ID mismatch: conversation=%s, message=%s", 
                            this.sessionId, message.getSessionId()));
        }

        messages.add(message);
        
        if (messages.size() > MAX_MESSAGES_DEFAULT) {
            messages.remove(0); 
        }
    }

    public List<ChatMessage> getRecentMessages(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        int startIndex = Math.max(0, messages.size() - limit);
        return Collections.unmodifiableList(
            new ArrayList<>(messages.subList(startIndex, messages.size()))
        );
    }


    public int getMessageCount() {
        return messages.size();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return String.format("Conversation{sessionId='%s', messageCount=%d, startedAt=%s, isActive=%s}", 
                           sessionId, messages.size(), startedAt);
    }

    public static class Builder {
        private String sessionId;
        private String userId;
        private LocalDateTime startedAt;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Conversation build() {
            return new Conversation(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // 대화시작
    public static Conversation startNewSession(String userId) {
        return builder()
                .userId(userId)
                .build();
    }

    public static Conversation withSessionId(String sessionId, String userId) {
        return builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();
    }
}