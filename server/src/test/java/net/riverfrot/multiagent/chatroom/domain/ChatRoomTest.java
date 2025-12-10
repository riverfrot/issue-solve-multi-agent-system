package net.riverfrot.multiagent.chatroom.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("채팅방 도메인 테스트")
class ChatRoomTest {

    @Test
    @DisplayName("채팅방 생성시 유효하지 않은 User ID로 예외가 발생한다")
    void chatRoom_shouldThrowExceptionForInvalidUserId() {
        // Given & When & Then
        assertThatThrownBy(() -> ChatRoom.createChatRoom(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID는 비어있을 수 없습니다.");

        assertThatThrownBy(() -> ChatRoom.createChatRoom(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID는 비어있을 수 없습니다.");

        assertThatThrownBy(() -> ChatRoom.createChatRoom("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID는 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("채팅방 제목 변경시 유효하지 않은 제목으로 예외가 발생한다")
    void chatRoom_shouldThrowExceptionForInvalidTitle() {
        // Given
        ChatRoom chatRoom = ChatRoom.createChatRoom("user-123");
        String longTitle = "a".repeat(101); // 101자 제목
        
        // When & Then
        assertThatThrownBy(() -> chatRoom.updateTitle(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방 제목은 비어있을 수 없습니다.");

        assertThatThrownBy(() -> chatRoom.updateTitle(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방 제목은 비어있을 수 없습니다.");

        assertThatThrownBy(() -> chatRoom.updateTitle(longTitle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방 제목은 100자를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("채팅방 소유자 확인이 정상적으로 동작한다")
    void chatRoom_shouldCheckOwnershipCorrectly() {
        // Given
        String userId = "user-123";
        ChatRoom chatRoom = ChatRoom.createChatRoom(userId);
        
        // When & Then
        assertThat(chatRoom.isOwnedBy(userId)).isTrue();
        assertThat(chatRoom.isOwnedBy("other-user")).isFalse();
    }
}