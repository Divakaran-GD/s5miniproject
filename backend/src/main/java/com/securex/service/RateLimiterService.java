package com.securex.service;

public interface RateLimiterService {
    boolean isAllowed(String key, int maxRequests, long windowMs);
}
