package com.laptopshop.repository;

import com.laptopshop.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomKeyOrderBySentAtAscIdAsc(String roomKey);

    @Query("SELECT DISTINCT c.roomKey FROM ChatMessage c ORDER BY c.roomKey")
    List<String> findDistinctRoomKeys();

    long countByIsReadFalseAndSenderRole(String senderRole);
}
