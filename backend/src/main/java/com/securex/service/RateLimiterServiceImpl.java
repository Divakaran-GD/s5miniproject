package com.securex.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterServiceImpl implements RateLimiterService {

    private final Map<String, Deque<Long>> requestCounts = new ConcurrentHashMap<>();

    @Override
    public synchronized boolean isAllowed(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        requestCounts.putIfAbsent(key, new ArrayDeque<>());
        Deque<Long> timestamps = requestCounts.get(key);

        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMs) {
            timestamps.pollFirst();
        }

        if (timestamps.size() < maxRequests) {
            timestamps.addLast(now);
            return true;
        }

        return false;
    }
}
