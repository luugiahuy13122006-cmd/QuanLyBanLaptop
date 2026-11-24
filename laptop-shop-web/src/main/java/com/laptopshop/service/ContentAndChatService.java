package com.laptopshop.service;

import com.laptopshop.entity.Banner;
import com.laptopshop.entity.ChatMessage;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.repository.BannerRepository;
import com.laptopshop.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContentAndChatService {

    private final BannerRepository bannerRepository;
    private final ChatMessageRepository chatRepository;
    private final NotificationSseService sseService;

    public ContentAndChatService(BannerRepository bannerRepository,
                                 ChatMessageRepository chatRepository,
                                 NotificationSseService sseService) {
        this.bannerRepository = bannerRepository;
        this.chatRepository = chatRepository;
        this.sseService = sseService;
    }

    @Transactional(readOnly = true)
    public List<Banner> listActiveBanners() {
        return bannerRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getRoomMessages(String roomKey) {
        return chatRepository.findByRoomKeyOrderBySentAtAscIdAsc(roomKey);
    }

    @Transactional(readOnly = true)
    public List<String> getChatRooms() {
        return chatRepository.findDistinctRoomKeys();
    }

    @Transactional(readOnly = true)
    public long getUnreadCustomerMessageCount() {
        return chatRepository.countByIsReadFalseAndSenderRole("CUSTOMER");
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> getRoomUnreadMap(List<String> rooms) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (String room : rooms) {
            map.put(room, chatRepository.existsByRoomKeyAndIsReadFalseAndSenderRole(room, "CUSTOMER"));
        }
        return map;
    }

    @Transactional
    public void markRoomAsRead(String roomKey) {
        if (roomKey != null && !roomKey.isBlank()) {
            chatRepository.markRoomAsRead(roomKey);
        }
    }

    @Transactional
    public ChatMessage postChatMessage(String roomKey, String senderName, String senderRole, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("Nội dung tin nhắn không được để trống.");
        }
        String clean = content.trim();
        ChatMessage saved = chatRepository.save(new ChatMessage(roomKey, senderName, senderRole,
                clean.length() > 1000 ? clean.substring(0, 1000) : clean));
        if ("CUSTOMER".equalsIgnoreCase(senderRole)) {
            sseService.broadcast("NEW_CHAT", "Tin nhắn hỗ trợ mới từ " + senderName,
                    clean.length() > 60 ? clean.substring(0, 60) + "..." : clean,
                    "/admin/content?room=" + roomKey);
        } else {
            chatRepository.markRoomAsRead(roomKey);
        }
        return saved;
    }
}
