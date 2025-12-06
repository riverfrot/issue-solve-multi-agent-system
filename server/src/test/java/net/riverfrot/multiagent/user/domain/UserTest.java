package net.riverfrot.multiagent.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User 도메인 테스트")
class UserTest {

    @Test
    @DisplayName("닉네임이 null인 경우 예외 발생")
    void user_shouldThrowExceptionForNullNickname() {
        // Given, When, Then
        assertThatThrownBy(() -> User.createWithNickname(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("닉네임은 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("닉네임이 빈 문자열인 경우 예외 발생")
    void user_shouldThrowExceptionForEmptyNickname() {
        // Given, When, Then
        assertThatThrownBy(() -> User.createWithNickname("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("닉네임은 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("닉네임이 50자 초과인 경우 예외 발생")
    void user_shouldThrowExceptionForTooLongNickname() {
        // Given
        String longNickname = "a".repeat(51);
        
        // When, Then
        assertThatThrownBy(() -> User.createWithNickname(longNickname))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("닉네임은 50자를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("유효한 닉네임으로 User 생성 성공")
    void user_shouldCreateSuccessfullyWithValidNickname() {
        // Given
        String validNickname = "testUser";
        
        // When
        User user = User.createWithNickname(validNickname);
        
        // Then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getNickname()).isEqualTo(validNickname);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("50자 닉네임은 허용")
    void user_shouldAllowMaxLengthNickname() {
        // Given
        String maxLengthNickname = "a".repeat(50);
        
        // When
        User user = User.createWithNickname(maxLengthNickname);
        
        // Then
        assertThat(user.getNickname()).isEqualTo(maxLengthNickname);
        assertThat(user.getNickname().length()).isEqualTo(50);
    }
}