package net.riverfrot.multiagent.user.dto;

import net.riverfrot.multiagent.user.domain.User;

import java.time.LocalDateTime;

/**
 * 사용자 응답 DTO
 */
public record UserResponse(
    String id,
    String nickname,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getNickname(),
            user.getCreatedAt()
        );
    }
}