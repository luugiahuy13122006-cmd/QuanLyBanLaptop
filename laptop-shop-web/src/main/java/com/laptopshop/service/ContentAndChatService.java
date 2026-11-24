package com.laptopshop.service;

import com.laptopshop.entity.Banner;
import com.laptopshop.entity.ChatMessage;
import com.laptopshop.entity.FaqItem;
import com.laptopshop.entity.TechArticle;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.BannerRepository;
import com.laptopshop.repository.ChatMessageRepository;
import com.laptopshop.repository.FaqItemRepository;
import com.laptopshop.repository.TechArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContentAndChatService {

    private final TechArticleRepository articleRepository;
    private final FaqItemRepository faqRepository;
    private final BannerRepository bannerRepository;
    private final ChatMessageRepository chatRepository;
    private final NotificationSseService sseService;

    public ContentAndChatService(TechArticleRepository articleRepository,
                                 FaqItemRepository faqRepository,
                                 BannerRepository bannerRepository,
                                 ChatMessageRepository chatRepository,
                                 NotificationSseService sseService) {
        this.articleRepository = articleRepository;
        this.faqRepository = faqRepository;
        this.bannerRepository = bannerRepository;
        this.chatRepository = chatRepository;
        this.sseService = sseService;
    }

    @Transactional(readOnly = true)
    public Page<TechArticle> listPublishedArticles(Pageable pageable) {
        return articleRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<TechArticle> listAllArticles() {
        return articleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TechArticle getArticleBySlug(String slug) {
        return articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết: " + slug));
    }

    @Transactional
    public TechArticle saveArticle(TechArticle form) {
        if (form.getSlug() == null || form.getSlug().isBlank()) {
            throw new BusinessException("Đường dẫn (slug) bài viết không được để trống.");
        }
        String slug = form.getSlug().trim().toLowerCase();
        if (form.getId() == null && articleRepository.existsBySlugIgnoreCase(slug)) {
            throw new BusinessException("Slug '" + slug + "' đã tồn tại.");
        }
        TechArticle target = form.getId() != null
                ? articleRepository.findById(form.getId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết"))
                : new TechArticle();
        target.setTitle(form.getTitle().trim());
        target.setSlug(slug);
        target.setSummary(form.getSummary());
        target.setContent(form.getContent());
        target.setCoverImage(form.getCoverImage());
        target.setAuthor(form.getAuthor() != null && !form.getAuthor().isBlank() ? form.getAuthor().trim() : "NEXORA Editorial");
        target.setPublished(form.getPublished() != null ? form.getPublished() : true);
        return articleRepository.save(target);
    }

    @Transactional(readOnly = true)
    public List<FaqItem> listPublishedFaqs() {
        return faqRepository.findByPublishedTrueOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<FaqItem> listAllFaqs() {
        return faqRepository.findAll();
    }

    @Transactional
    public FaqItem saveFaq(FaqItem form) {
        return faqRepository.save(form);
    }

    @Transactional(readOnly = true)
    public List<Banner> listActiveBanners() {
        return bannerRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<Banner> listAllBanners() {
        return bannerRepository.findAll();
    }

    @Transactional
    public Banner saveBanner(Banner form) {
        return bannerRepository.save(form);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getRoomMessages(String roomKey) {
        return chatRepository.findByRoomKeyOrderBySentAtAscIdAsc(roomKey);
    }

    @Transactional(readOnly = true)
    public List<String> getChatRooms() {
        return chatRepository.findDistinctRoomKeys();
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
            sseService.broadcast("NEW_CHAT", "Tin nhắn hỗ trợ mới",
                    senderName + ": " + (clean.length() > 60 ? clean.substring(0, 60) + "..." : clean),
                    "/admin/content?room=" + roomKey);
        }
        return saved;
    }
}
