package net.riverfrot.multiagent.chatroom.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chatrooms")
public class ChatRoom {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;
    
    @Column(name = "title", length = 100)
    private String title;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    protected ChatRoom() {}
    
    private ChatRoom(String userId, String title) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public static ChatRoom createChatRoom(String userId, String title) {
        validateUserId(userId);
        return new ChatRoom(userId, title != null ? title : generateDefaultTitle());
    }
    
    public static ChatRoom createChatRoom(String userId) {
        return createChatRoom(userId, null);
    }
    
    private static void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID는 비어있을 수 없습니다.");
        }
    }
    
    private static String generateDefaultTitle() {
        return "새 채팅 " + LocalDateTime.now().getMonthValue() + "/" + LocalDateTime.now().getDayOfMonth();
    }
    
    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("채팅방 제목은 비어있을 수 없습니다.");
        }
        if (newTitle.length() > 100) {
            throw new IllegalArgumentException("채팅방 제목은 100자를 초과할 수 없습니다.");
        }
        this.title = newTitle.trim();
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChatRoom chatRoom = (ChatRoom) obj;
        return id != null && id.equals(chatRoom.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "ChatRoom{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}