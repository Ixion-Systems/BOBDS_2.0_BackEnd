package com.bobds.server;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    // Mapa de email a lista de emitters (un usuario puede tener varias pestañas abiertas)
    private final Map<String, List<SseEmitter>> emittersMap = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String email) {
        // Timeout de 1 hora o sin timeout (0L no soportado en todos los tomcats, usamos 1hr)
        SseEmitter emitter = new SseEmitter(3600000L);
        
        emittersMap.computeIfAbsent(email, k -> new ArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(email, emitter));
        emitter.onTimeout(() -> removeEmitter(email, emitter));
        emitter.onError((e) -> removeEmitter(email, emitter));

        return emitter;
    }

    private void removeEmitter(String email, SseEmitter emitter) {
        List<SseEmitter> list = emittersMap.get(email);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersMap.remove(email);
            }
        }
    }

    public void sendEventToEmail(String email, String eventName, Object data) {
        List<SseEmitter> list = emittersMap.get(email);
        if (list != null) {
            // Copia para iterar de forma segura sin ConcurrentModificationException
            List<SseEmitter> copy = new ArrayList<>(list);
            for (SseEmitter emitter : copy) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (IOException e) {
                    removeEmitter(email, emitter);
                }
            }
        }
    }
}
