package net.riverfrot.multiagent.chatroom.infrastructure;

import net.riverfrot.multiagent.chatroom.domain.ChatRoom;
import net.riverfrot.multiagent.chatroom.domain.ChatRoomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaChatRoomRepository extends JpaRepository<ChatRoom, String>, ChatRoomRepository {
    
    @Override
    List<ChatRoom> findByUserId(String userId);
    
    @Override
    List<ChatRoom> findByUserIdOrderByUpdatedAtDesc(String userId);
    
    @Override
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ChatRoom c WHERE c.id = :chatRoomId AND c.userId = :userId")
    boolean existsByIdAndUserId(@Param("chatRoomId") String chatRoomId, @Param("userId") String userId);
}