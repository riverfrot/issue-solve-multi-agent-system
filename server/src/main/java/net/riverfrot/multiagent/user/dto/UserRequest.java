package net.riverfrot.multiagent.user.dto;

/**
 * 사용자 관련 요청 DTO
 */
public record UserRequest(
    String nickname
) {
    public UserRequest {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
    }
}