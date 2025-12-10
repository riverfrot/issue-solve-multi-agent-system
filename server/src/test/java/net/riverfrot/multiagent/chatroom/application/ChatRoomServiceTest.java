package net.riverfrot.multiagent.chatroom.application;

import net.riverfrot.multiagent.chatroom.domain.ChatRoom;
import net.riverfrot.multiagent.chatroom.domain.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("채팅방 서비스 테스트")
class ChatRoomServiceTest {
    
    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    private ChatRoomService chatRoomService;
    
    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomService(chatRoomRepository);
    }
    
    @Test
    @DisplayName("채팅방 생성 성공")
    void createChatRoom_shouldCreateSuccessfully() {
        // Given
        String userId = "user-123";
        String title = "새 채팅방";
        ChatRoom mockChatRoom = ChatRoom.createChatRoom(userId, title);
        
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(mockChatRoom);
        
        // When
        ChatRoom result = chatRoomService.createChatRoom(userId, title);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTitle()).isEqualTo(title);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }
    
    @Test
    @DisplayName("사용자별 채팅방 목록 조회")
    void getChatRoomsByUser_shouldReturnUserChatRooms() {
        // Given
        String userId = "user-123";
        List<ChatRoom> mockChatRooms = List.of(
            ChatRoom.createChatRoom(userId, "채팅방 1"),
            ChatRoom.createChatRoom(userId, "채팅방 2")
        );
        
        when(chatRoomRepository.findByUserIdOrderByUpdatedAtDesc(userId))
                .thenReturn(mockChatRooms);
        
        // When
        List<ChatRoom> result = chatRoomService.getChatRoomsByUser(userId);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(1).getUserId()).isEqualTo(userId);
    }
    
    @Test
    @DisplayName("채팅방 제목 수정 성공")
    void updateChatRoomTitle_shouldUpdateSuccessfully() {
        // Given
        String chatRoomId = "room-123";
        String userId = "user-123";
        String newTitle = "수정된 제목";
        ChatRoom mockChatRoom = ChatRoom.createChatRoom(userId, "원래 제목");
        
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(mockChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(mockChatRoom);
        
        // When
        ChatRoom result = chatRoomService.updateChatRoomTitle(chatRoomId, userId, newTitle);
        
        // Then
        assertThat(result).isNotNull();
        verify(chatRoomRepository).findById(chatRoomId);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }
    
    @Test
    @DisplayName("권한 없는 사용자의 채팅방 수정 시도시 예외 발생")
    void updateChatRoomTitle_shouldThrowExceptionWhenUnauthorized() {
        // Given
        String chatRoomId = "room-123";
        String ownerId = "owner-123";
        String unauthorizedUserId = "other-user";
        String newTitle = "수정된 제목";
        ChatRoom mockChatRoom = ChatRoom.createChatRoom(ownerId, "원래 제목");
        
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(mockChatRoom));
        
        // When & Then
        assertThatThrownBy(() -> 
            chatRoomService.updateChatRoomTitle(chatRoomId, unauthorizedUserId, newTitle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방을 수정할 권한이 없습니다.");
    }
    
    @Test
    @DisplayName("존재하지 않는 채팅방 수정 시도시 예외 발생")
    void updateChatRoomTitle_shouldThrowExceptionWhenNotFound() {
        // Given
        String nonExistentChatRoomId = "non-existent";
        String userId = "user-123";
        String newTitle = "수정된 제목";
        
        when(chatRoomRepository.findById(nonExistentChatRoomId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> 
            chatRoomService.updateChatRoomTitle(nonExistentChatRoomId, userId, newTitle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방을 찾을 수 없습니다: " + nonExistentChatRoomId);
    }
}