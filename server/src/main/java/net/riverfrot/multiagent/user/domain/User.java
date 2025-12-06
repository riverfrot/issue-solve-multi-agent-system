package net.riverfrot.multiagent.user.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "nickname", length = 50, nullable = false)
    private String nickname;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    protected User() {}
    
    private User(String nickname) {
        this.id = UUID.randomUUID().toString();
        this.nickname = nickname;
        this.createdAt = LocalDateTime.now();
    }
    
    public static User createWithNickname(String nickname) {
        validateNickname(nickname);
        return new User(nickname);
    }
    
    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }
        if (nickname.trim().length() > 50) {
            throw new IllegalArgumentException("닉네임은 50자를 초과할 수 없습니다.");
        }
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return id != null && id.equals(user.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", nickname='" + nickname + '\'' +
                '}';
    }
}