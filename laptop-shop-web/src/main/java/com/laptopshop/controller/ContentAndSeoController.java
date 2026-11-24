package com.laptopshop.controller;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.entity.Banner;
import com.laptopshop.entity.ChatMessage;
import com.laptopshop.entity.FaqItem;
import com.laptopshop.entity.TechArticle;
import com.laptopshop.service.ContentAndChatService;
import com.laptopshop.service.LaptopService;
import com.laptopshop.util.PaginationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class ContentAndSeoController {

    private final ContentAndChatService contentService;
    private final LaptopService laptopService;

    public ContentAndSeoController(ContentAndChatService contentService, LaptopService laptopService) {
        this.contentService = contentService;
        this.laptopService = laptopService;
    }

    @GetMapping("/tin-tuc")
    public String newsList(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("articlePage", contentService.listPublishedArticles(
                PageRequest.of(PaginationUtils.clampPage(page), 6)));
        model.addAttribute("faqs", contentService.listPublishedFaqs());
        return "store/news";
    }

    @GetMapping("/tin-tuc/{slug}")
    public String newsDetail(@PathVariable String slug, Model model) {
        model.addAttribute("article", contentService.getArticleBySlug(slug));
        return "store/news-detail";
    }

    @GetMapping("/ho-tro")
    public String supportChatPage(@AuthenticationPrincipal CustomUserDetails currentUser,
                                  HttpSession session,
                                  Model model) {
        String roomKey = resolveRoomKey(currentUser, session);
        model.addAttribute("roomKey", roomKey);
        model.addAttribute("messages", contentService.getRoomMessages(roomKey));
        model.addAttribute("faqs", contentService.listPublishedFaqs());
        return "store/support-chat";
    }

    @GetMapping("/api/chat/messages")
    @ResponseBody
    public List<Map<String, Object>> getChatMessages(@RequestParam(required = false) String roomKey,
                                                     @AuthenticationPrincipal CustomUserDetails currentUser,
                                                     HttpSession session) {
        String key = (roomKey != null && !roomKey.isBlank() && currentUser != null
                && ("ADMIN".equalsIgnoreCase(currentUser.getRoleName()) || "STAFF".equalsIgnoreCase(currentUser.getRoleName())))
                ? roomKey : resolveRoomKey(currentUser, session);
        return contentService.getRoomMessages(key).stream().map(m -> Map.<String, Object>of(
                "id", m.getId(),
                "senderName", m.getSenderName(),
                "senderRole", m.getSenderRole(),
                "content", m.getContent(),
                "sentAt", m.getSentAt().toString()
        )).toList();
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<?> sendChat(@RequestParam String content,
                                      @RequestParam(required = false) String roomKey,
                                      @AuthenticationPrincipal CustomUserDetails currentUser,
                                      HttpSession session) {
        boolean isStaff = currentUser != null
                && ("ADMIN".equalsIgnoreCase(currentUser.getRoleName()) || "STAFF".equalsIgnoreCase(currentUser.getRoleName()));
        String targetRoom = (isStaff && roomKey != null && !roomKey.isBlank())
                ? roomKey : resolveRoomKey(currentUser, session);
        String senderName = currentUser != null ? currentUser.getFullName() : "Khách hàng";
        String role = isStaff ? currentUser.getRoleName() : "CUSTOMER";
        ChatMessage saved = contentService.postChatMessage(targetRoom, senderName, role, content);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "roomKey", targetRoom));
    }

    private String resolveRoomKey(CustomUserDetails user, HttpSession session) {
        if (user != null) return "ACC-" + user.getAccountId();
        return "GUEST-" + session.getId().substring(0, Math.min(8, session.getId().length()));
    }

    @GetMapping("/admin/content")
    public String adminContent(@RequestParam(required = false) String room, Model model) {
        model.addAttribute("articles", contentService.listAllArticles());
        model.addAttribute("faqs", contentService.listAllFaqs());
        model.addAttribute("banners", contentService.listAllBanners());
        List<String> rooms = contentService.getChatRooms();
        String activeRoom = (room != null && !room.isBlank()) ? room : (rooms.isEmpty() ? null : rooms.get(0));
        model.addAttribute("chatRooms", rooms);
        model.addAttribute("activeRoom", activeRoom);
        model.addAttribute("roomMessages", activeRoom != null ? contentService.getRoomMessages(activeRoom) : List.of());
        return "admin/content/index";
    }

    @PostMapping("/admin/content/articles/save")
    public String saveArticle(@ModelAttribute TechArticle article, RedirectAttributes ra) {
        try {
            contentService.saveArticle(article);
            ra.addFlashAttribute("successMessage", "Đã lưu bài viết công nghệ.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/admin/content/faqs/save")
    public String saveFaq(@ModelAttribute FaqItem faq, RedirectAttributes ra) {
        contentService.saveFaq(faq);
        ra.addFlashAttribute("successMessage", "Đã lưu câu hỏi FAQ.");
        return "redirect:/admin/content";
    }

    @PostMapping("/admin/content/banners/save")
    public String saveBanner(@ModelAttribute Banner banner, RedirectAttributes ra) {
        contentService.saveBanner(banner);
        ra.addFlashAttribute("successMessage", "Đã lưu banner trang chủ.");
        return "redirect:/admin/content";
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robotsTxt() {
        return "User-agent: *\nAllow: /\nDisallow: /admin/\nDisallow: /api/\nSitemap: /sitemap.xml\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemapXml(HttpServletRequest req) {
        String base = req.getScheme() + "://" + req.getServerName()
                + ((req.getServerPort() == 80 || req.getServerPort() == 443) ? "" : ":" + req.getServerPort());
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String p : List.of("/", "/bao-hanh", "/tin-tuc", "/ho-tro", "/thong-tin/gioi-thieu")) {
            xml.append("  <url><loc>").append(base).append(p).append("</loc></url>\n");
        }
        laptopService.getAllLaptops().stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                .forEach(l -> xml.append("  <url><loc>").append(base).append("/san-pham/").append(l.getId()).append("</loc></url>\n"));
        xml.append("</urlset>\n");
        return xml.toString();
    }
}
