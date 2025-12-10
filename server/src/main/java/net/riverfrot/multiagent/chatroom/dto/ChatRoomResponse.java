package net.riverfrot.multiagent.chatroom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.riverfrot.multiagent.chatroom.domain.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        @JsonProperty("id")
        String id,
        
        @JsonProperty("user_id")
        String userId,
        
        @JsonProperty("title")
        String title,
        
        @JsonProperty("created_at")
        LocalDateTime createdAt,
        
        @JsonProperty("updated_at")
        LocalDateTime updatedAt,
        
        @JsonProperty("last_message_at")
        LocalDateTime lastMessageAt
) {
    
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getUserId(),
                chatRoom.getTitle(),
                chatRoom.getCreatedAt(),
                chatRoom.getUpdatedAt(),
                chatRoom.getLastMessageAt()
        );
    }
}