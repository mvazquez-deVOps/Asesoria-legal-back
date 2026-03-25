package com.juxa.legal_advice.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // Mapa en memoria para guardar el bucket de cada IP
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        // Límite: 50 peticiones
        // Recarga: 50 tokens cada 1 minuto
        Refill refill = Refill.intervally(100, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(100, refill);

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}