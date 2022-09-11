package de.tum.i13.server.kvcache;
import java.util.concurrent.ConcurrentLinkedDeque;

public class FIFO extends CacheStrategy{
    private ConcurrentLinkedDeque<String> keys;
    
    public FIFO (int capacity){
        this.capacity=capacity;
        keys = new ConcurrentLinkedDeque<String>();
    }

    /**
     * adds the key to the cacheStrategy queue.
     * @param key the key that identifies the value.
     * @return the key of the evicted pair if the queue is full, null otherwise.
     */
    @Override
    String addToCache(String key) {
        if(keys.size()<capacity){
            keys.add(key);
            return null;
        } else {
            keys.add(key);
            return keys.poll();
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
    @Override
    void rearrangeCache(String key) {        
    }
    
}
