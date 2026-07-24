package com.vehicle.server.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserDetailsCache {

    private static final long TTL_MS = 60_000L;

    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    public UserDetails get(String username) {
        Entry entry = cache.get(username);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            cache.remove(username, entry);
            return null;
        }
        return entry.details;
    }

    public void put(String username, UserDetails details) {
        cache.put(username, new Entry(details, System.currentTimeMillis() + TTL_MS));
    }

    public void evict(String username) {
        if (username != null) {
            cache.remove(username);
        }
    }

    public void clear() {
        cache.clear();
    }

    private record Entry(UserDetails details, long expireAt) {
    }
}
