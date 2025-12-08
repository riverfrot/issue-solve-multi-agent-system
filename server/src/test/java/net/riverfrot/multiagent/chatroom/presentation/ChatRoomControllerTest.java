package net.riverfrot.multiagent.chatroom.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.riverfrot.multiagent.chatroom.application.ChatRoomService;
import net.riverfrot.multiagent.chatroom.domain.ChatRoom;
import net.riverfrot.multiagent.chatroom.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class)
@DisplayName("ChatRoom Controller 테스트")
class ChatRoomControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ChatRoomService chatRoomService;
    
    @Test
    @DisplayName("채팅방 생성 - 성공")
    void createChatRoom_Success() throws Exception {
        // Given
        String userId = "user123";
        String title = "테스트 채팅방";
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(userId, title);
        
        ChatRoom mockChatRoom = ChatRoom.createChatRoom(userId, title);
        when(chatRoomService.createChatRoom(userId, title)).thenReturn(mockChatRoom);
        
        // When & Then
        mockMvc.perform(post("/api/v1/chatrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user_id").value(userId))
                .andExpect(jsonPath("$.title").value(title));
        
        verify(chatRoomService).createChatRoom(userId, title);
    }
    
    @Test
    @DisplayName("채팅방 생성 - 제목 없이 성공")
    void createChatRoom_WithoutTitle_Success() throws Exception {
        // Given
        String userId = "user123";
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(userId, null);
        
        ChatRoom mockChatRoom = ChatRoom.createChatRoom(userId);
        when(chatRoomService.createChatRoom(userId)).thenReturn(mockChatRoom);
        
        // When & Then
        mockMvc.perform(post("/api/v1/chatrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user_id").value(userId));
        
        verify(chatRoomService).createChatRoom(userId);
    }
    
    @Test
    @DisplayName("채팅방 생성 - 유효하지 않은 요청으로 실패")
    void createChatRoom_InvalidRequest_BadRequest() throws Exception {
        // Given - 유효한 요청을 만들고 서비스에서 예외 발생하도록 설정
        ChatRoomCreateRequest request = new ChatRoomCreateRequest("user123", "title");
        
        when(chatRoomService.createChatRoom(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("User ID는 비어있을 수 없습니다."));
        
        // When & Then
        mockMvc.perform(post("/api/v1/chatrooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("사용자별 채팅방 목록 조회 - 성공")
    void getChatRoomsByUser_Success() throws Exception {
        // Given
        String userId = "user123";
        ChatRoom chatRoom1 = ChatRoom.createChatRoom(userId, "채팅방 1");
        ChatRoom chatRoom2 = ChatRoom.createChatRoom(userId, "채팅방 2");
        List<ChatRoom> chatRooms = List.of(chatRoom1, chatRoom2);
        
        when(chatRoomService.getChatRoomsByUser(userId)).thenReturn(chatRooms);
        
        // When & Then
        mockMvc.perform(get("/api/v1/chatrooms")
                .param("user_id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
        
        verify(chatRoomService).getChatRoomsByUser(userId);
    }
    
    @Test
    @DisplayName("사용자별 채팅방 목록 조회 - 잘못된 사용자 ID로 실패")
    void getChatRoomsByUser_InvalidUserId_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/chatrooms")
                .param("user_id", ""))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("특정 채팅방 조회 - 성공")
    void getChatRoom_Success() throws Exception {
        // Given
        String chatRoomId = "chatroom123";
        ChatRoom mockChatRoom = ChatRoom.createChatRoom("user123", "테스트 채팅방");
        
        when(chatRoomService.getChatRoom(chatRoomId)).thenReturn(Optional.of(mockChatRoom));
        
        // When & Then
        mockMvc.perform(get("/api/v1/chatrooms/{chatRoomId}", chatRoomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("테스트 채팅방"));
        
        verify(chatRoomService).getChatRoom(chatRoomId);
    }
    
    @Test
    @DisplayName("특정 채팅방 조회 - 존재하지 않는 채팅방으로 실패")
    void getChatRoom_NotFound() throws Exception {
        // Given
        String chatRoomId = "nonexistent";
        
        when(chatRoomService.getChatRoom(chatRoomId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/v1/chatrooms/{chatRoomId}", chatRoomId))
                .andExpect(status().isNotFound());
        
        verify(chatRoomService).getChatRoom(chatRoomId);
    }
    
    @Test
    @DisplayName("채팅방 제목 수정 - 성공")
    void updateChatRoomTitle_Success() throws Exception {
        // Given
        String chatRoomId = "chatroom123";
        String userId = "user123";
        String newTitle = "수정된 제목";
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest(userId, newTitle);
        
        ChatRoom updatedChatRoom = ChatRoom.createChatRoom(userId, newTitle);
        when(chatRoomService.updateChatRoomTitle(chatRoomId, userId, newTitle))
                .thenReturn(updatedChatRoom);
        
        // When & Then
        mockMvc.perform(put("/api/v1/chatrooms/{chatRoomId}", chatRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(newTitle));
        
        verify(chatRoomService).updateChatRoomTitle(chatRoomId, userId, newTitle);
    }
    
    @Test
    @DisplayName("채팅방 제목 수정 - 권한 없음으로 실패")
    void updateChatRoomTitle_Unauthorized_BadRequest() throws Exception {
        // Given
        String chatRoomId = "chatroom123";
        String userId = "user123";
        String newTitle = "수정된 제목";
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest(userId, newTitle);
        
        when(chatRoomService.updateChatRoomTitle(chatRoomId, userId, newTitle))
                .thenThrow(new IllegalArgumentException("채팅방을 수정할 권한이 없습니다."));
        
        // When & Then
        mockMvc.perform(put("/api/v1/chatrooms/{chatRoomId}", chatRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("채팅방 삭제 - 성공")
    void deleteChatRoom_Success() throws Exception {
        // Given
        String chatRoomId = "chatroom123";
        String userId = "user123";
        ChatRoomDeleteRequest request = new ChatRoomDeleteRequest(userId);
        
        doNothing().when(chatRoomService).deleteChatRoom(chatRoomId, userId);
        
        // When & Then
        mockMvc.perform(delete("/api/v1/chatrooms/{chatRoomId}", chatRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
        
        verify(chatRoomService).deleteChatRoom(chatRoomId, userId);
    }
    
    @Test
    @DisplayName("채팅방 삭제 - 권한 없음으로 실패")
    void deleteChatRoom_Unauthorized_BadRequest() throws Exception {
        // Given
        String chatRoomId = "chatroom123";
        String userId = "user123";
        ChatRoomDeleteRequest request = new ChatRoomDeleteRequest(userId);
        
        doThrow(new IllegalArgumentException("채팅방을 삭제할 권한이 없습니다."))
                .when(chatRoomService).deleteChatRoom(chatRoomId, userId);
        
        // When & Then
        mockMvc.perform(delete("/api/v1/chatrooms/{chatRoomId}", chatRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}