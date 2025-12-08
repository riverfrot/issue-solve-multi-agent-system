package net.riverfrot.multiagent.chatroom.application;

import net.riverfrot.multiagent.chatroom.domain.ChatRoom;
import net.riverfrot.multiagent.chatroom.domain.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatRoomService {
    
    private final ChatRoomRepository chatRoomRepository;
    
    public ChatRoomService(ChatRoomRepository chatRoomRepository) {
        this.chatRoomRepository = chatRoomRepository;
    }
    
    public ChatRoom createChatRoom(String userId, String title) {
        ChatRoom chatRoom = ChatRoom.createChatRoom(userId, title);
        return chatRoomRepository.save(chatRoom);
    }
    
    public ChatRoom createChatRoom(String userId) {
        ChatRoom chatRoom = ChatRoom.createChatRoom(userId);
        return chatRoomRepository.save(chatRoom);
    }
    
    public List<ChatRoom> getChatRoomsByUser(String userId) {
        return chatRoomRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
    
    public Optional<ChatRoom> getChatRoom(String chatRoomId) {
        return chatRoomRepository.findById(chatRoomId);
    }
    
    public ChatRoom updateChatRoomTitle(String chatRoomId, String userId, String newTitle) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
        
        if (!chatRoom.isOwnedBy(userId)) {
            throw new IllegalArgumentException("채팅방을 수정할 권한이 없습니다.");
        }
        
        chatRoom.updateTitle(newTitle);
        return chatRoomRepository.save(chatRoom);
    }
    
    public void deleteChatRoom(String chatRoomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
        
        if (!chatRoom.isOwnedBy(userId)) {
            throw new IllegalArgumentException("채팅방을 삭제할 권한이 없습니다.");
        }
        
        chatRoomRepository.deleteById(chatRoomId);
    }

}