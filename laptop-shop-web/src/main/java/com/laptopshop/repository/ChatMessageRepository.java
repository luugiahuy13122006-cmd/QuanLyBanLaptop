package com.laptopshop.repository;

import com.laptopshop.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomKeyOrderBySentAtAscIdAsc(String roomKey);

    @Query("SELECT DISTINCT c.roomKey FROM ChatMessage c ORDER BY c.roomKey")
    List<String> findDistinctRoomKeys();

    long countByIsReadFalseAndSenderRole(String senderRole);

    boolean existsByRoomKeyAndIsReadFalseAndSenderRole(String roomKey, String senderRole);

    @Modifying
    @Query("UPDATE ChatMessage c SET c.isRead = true WHERE c.roomKey = :roomKey AND c.senderRole = 'CUSTOMER' AND c.isRead = false")
    int markRoomAsRead(@Param("roomKey") String roomKey);
}
