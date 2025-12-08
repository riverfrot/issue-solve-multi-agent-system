package net.riverfrot.multiagent.chatroom.presentation;

import net.riverfrot.multiagent.chatroom.application.ChatRoomService;
import net.riverfrot.multiagent.chatroom.domain.ChatRoom;
import net.riverfrot.multiagent.chatroom.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/chatrooms")
public class ChatRoomController {
    
    private final ChatRoomService chatRoomService;
    
    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }
    
    /**
     * 채팅방 생성
     */
    @PostMapping
    public ResponseEntity<ChatRoomResponse> createChatRoom(@RequestBody ChatRoomCreateRequest request) {
        try {
            ChatRoom chatRoom;
            if (request.getTitle() != null) {
                chatRoom = chatRoomService.createChatRoom(request.getUserId(), request.getTitle());
            } else {
                chatRoom = chatRoomService.createChatRoom(request.getUserId());
            }
            
            ChatRoomResponse response = ChatRoomResponse.from(chatRoom);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 사용자별 채팅방 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getChatRoomsByUser(@RequestParam("user_id") String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<ChatRoom> chatRooms = chatRoomService.getChatRoomsByUser(userId);
            List<ChatRoomResponse> responses = chatRooms.stream()
                    .map(ChatRoomResponse::from)
                    .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 특정 채팅방 조회
     */
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ChatRoomResponse> getChatRoom(@PathVariable String chatRoomId) {
        try {
            Optional<ChatRoom> chatRoom = chatRoomService.getChatRoom(chatRoomId);
            
            if (chatRoom.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ChatRoomResponse response = ChatRoomResponse.from(chatRoom.get());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 채팅방 제목 수정
     */
    @PutMapping("/{chatRoomId}")
    public ResponseEntity<ChatRoomResponse> updateChatRoomTitle(
            @PathVariable String chatRoomId,
            @RequestBody ChatRoomUpdateRequest request) {
        try {
            ChatRoom updatedChatRoom = chatRoomService.updateChatRoomTitle(
                    chatRoomId, 
                    request.getUserId(), 
                    request.getTitle()
            );
            
            ChatRoomResponse response = ChatRoomResponse.from(updatedChatRoom);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 채팅방 삭제
     */
    @DeleteMapping("/{chatRoomId}")
    public ResponseEntity<Void> deleteChatRoom(
            @PathVariable String chatRoomId,
            @RequestBody ChatRoomDeleteRequest request) {
        try {
            chatRoomService.deleteChatRoom(chatRoomId, request.getUserId());
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}