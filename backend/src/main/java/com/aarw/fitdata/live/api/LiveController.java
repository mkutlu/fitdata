package com.aarw.fitdata.live.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/live")
public class LiveController {

    private final AtomicReference<LiveSample> last = new AtomicReference<>();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @PostMapping
    public void ingest(@RequestBody LiveSample sample) {
        last.set(sample);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(sample, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(0L); // timeout yok
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(_ -> emitters.remove(emitter));

        LiveSample existing = last.get();
        if (existing != null) {
            try {
                emitter.send(existing, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }

        return emitter;
    }
}
