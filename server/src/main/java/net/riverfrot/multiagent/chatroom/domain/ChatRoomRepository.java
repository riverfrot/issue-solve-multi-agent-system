package net.riverfrot.multiagent.chatroom.domain;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository {
    ChatRoom save(ChatRoom chatRoom);
    Optional<ChatRoom> findById(String chatRoomId);
    List<ChatRoom> findByUserId(String userId);
    List<ChatRoom> findByUserIdOrderByUpdatedAtDesc(String userId);
    void deleteById(String chatRoomId);
    boolean existsByIdAndUserId(String chatRoomId, String userId);
}