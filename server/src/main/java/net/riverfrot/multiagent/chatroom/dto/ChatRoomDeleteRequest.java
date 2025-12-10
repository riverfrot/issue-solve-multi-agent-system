package net.riverfrot.multiagent.chatroom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRoomDeleteRequest(
        @JsonProperty("user_id")
        String userId
) {
    public ChatRoomDeleteRequest {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID는 필수입니다.");
        }
    }
    
    public String getUserId() {
        return userId;
    }
}