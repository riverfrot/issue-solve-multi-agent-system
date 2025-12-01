package net.riverfrot.multiagent.chatbot.domain;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ChatMessage {

    private final String id;
    private final String sessionId;
    private final String role;
    private final String content;
    private final AgentType agentType;
    private final Map<String, Object> metadata;
    private final LocalDateTime timestamp;

    protected ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.sessionId = "";
        this.role = "";
        this.content = "";
        this.agentType = null;
        this.metadata = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }

    private ChatMessage(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.sessionId = builder.sessionId;
        this.role = builder.role;
        this.content = builder.content;
        this.agentType = builder.agentType;
        this.metadata = new HashMap<>(builder.metadata);
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        
        validateContent();
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    private void validateContent() {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }

        if (content.length() > 10000) {
            throw new IllegalArgumentException("Content too long (max 10000 chars)");
        }
    }

    public boolean isFromUser() {
        return "user".equals(role);
    }

    
    public boolean isFromAssistant() {
        return "assistant".equals(role);
    }

    public boolean isFromAgent() {
        return agentType != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ChatMessage{id='%s', role='%s', sessionId='%s', timestamp=%s}", 
                           id, role, sessionId, timestamp);
    }

    public static class Builder {
        private String id;
        private String sessionId;
        private String role;
        private String content;
        private AgentType agentType;
        private Map<String, Object> metadata = new HashMap<>();
        private LocalDateTime timestamp;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder agentType(AgentType agentType) {
            this.agentType = agentType;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public ChatMessage build() {
            return new ChatMessage(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ChatMessage createUserMessage(String sessionId, String content) {
        return builder()
                .sessionId(sessionId)
                .role("user")
                .content(content)
                .build();
    }

    public static ChatMessage createAssistantMessage(String sessionId, String content, AgentType agentType) {
        return builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(content)
                .agentType(agentType)
                .build();
    }
}