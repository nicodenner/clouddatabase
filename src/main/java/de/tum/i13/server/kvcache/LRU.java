package de.tum.i13.server.kvcache;

import java.util.concurrent.ConcurrentLinkedDeque;

public class LRU extends CacheStrategy {
    private ConcurrentLinkedDeque<String> keys;
    
    public LRU (int capacity){
        this.capacity=capacity;
        keys = new ConcurrentLinkedDeque<String>();
    }

    /**
     * adds the key to the cacheStrategy queue.
     * removes the last element of the queue if it is full.
     * @param key the key that identifies the value.
     * @return the key of the evicted pair if the queue is full, null otherwise.
     */
    @Override
    String addToCache(String key) {
        if(keys.size()<capacity){
            keys.addFirst(key);
            return null;
        } else {
            keys.addFirst(key);
            return keys.removeLast();
        }
    }

    /**
     * deletes the key from the cacheStrategy queue.
     * @param key the key that identifies the value.
     * @return true if the key is present in the queue, false otherwise.
     */
    @Override
    boolean deleteFromCache(String key) {
        return keys.remove(key);
    }

    /**
     * rearrange the queue incase some value was recently used.
     * the value is added to the the front of the queue
     * @param key the key that identifies the value.
     */
    @Override
    void rearrangeCache(String key){
        keys.remove(key);
        keys.addFirst(key);
    }
    
}
