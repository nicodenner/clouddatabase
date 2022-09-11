package de.tum.i13.server.kvcache;

public abstract class CacheStrategy {
    int capacity;

    abstract String addToCache (String key);

    abstract boolean deleteFromCache(String key);

    abstract void rearrangeCache(String key);
}
