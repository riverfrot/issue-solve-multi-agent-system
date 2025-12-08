package net.riverfrot.multiagent.chatroom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRoomUpdateRequest(
        @JsonProperty("user_id")
        String userId,
        
        @JsonProperty("title")
        String title
) {
    public ChatRoomUpdateRequest {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID는 필수입니다.");
        }
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("채팅방 제목은 필수입니다.");
        }
        
        if (title.length() > 100) {
            throw new IllegalArgumentException("채팅방 제목은 100자를 초과할 수 없습니다.");
        }
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getTitle() {
        return title;
    }
}