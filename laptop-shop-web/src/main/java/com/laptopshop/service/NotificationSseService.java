package com.laptopshop.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationSseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of(
                    "message", "Đã kết nối thông báo thời gian thực",
                    "at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            )));
        } catch (IOException ignored) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void broadcast(String type, String title, String message, String url) {
        Map<String, Object> payload = Map.of(
                "type", type,
                "title", title,
                "message", message,
                "url", url != null ? url : "/admin/orders",
                "at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        }
    }
}
