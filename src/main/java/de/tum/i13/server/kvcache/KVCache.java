package de.tum.i13.server.kvcache;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.tum.i13.shared.Constants;

public class KVCache {
    private int maxSize;
    private ConcurrentHashMap<String, String> cache;
    private String strategy;
    private CacheStrategy cacheStrategy;
    public KVCache(int maxSize, String strategy) {
        this.maxSize=maxSize;
        this.strategy=strategy;
        this.cache = new ConcurrentHashMap<>();
        switch(strategy){
            case Constants.FIFO: cacheStrategy = new FIFO(maxSize); break;
            case Constants.LRU: cacheStrategy = new LRU(maxSize); break;
            case Constants.LFU: cacheStrategy = new LFU(maxSize); break;
            default: System.out.println("Incorrect cache strategy!"); 
            throw new IllegalArgumentException();
        }
        if(maxSize<=0)
            throw new IllegalArgumentException();
    }

    /**
     * Gets the value for a given key from the cache.
     * @param key the key that identifies the value.
     * @return the value which is indexed by the given key and rearranges the cache, null otherwise.
     */
    public String get(String key) {
        if(key==null)
            throw new NullPointerException();
        String value = null;
        if (cache.containsKey(key)){
            value = cache.get(key);
            cacheStrategy.rearrangeCache(key);
        }
        return value;
    }

    /**
     * Inserts a key-value pair into the cache.
     * Checks if the cache doesn't contain the key and it's full then it applies the eviction strategy
     * Inserts/update the key-value pair otherwise
     * @param key   the key that identifies the given value.
     * @param value the value that is indexed by the given key.
     * @return the key of the toBeDeleted pair, null if not needed.
     */
    public String put(String key, String value) {
        if(key==null || value==null)
            throw new NullPointerException();
        if(!cache.containsKey(key)){
            if(cache.size() < maxSize){
                cache.put(key, value);
                cacheStrategy.addToCache(key);
            }
            else {
                cache.put(key, value);
                String keyToDelete = cacheStrategy.addToCache(key);
                if(keyToDelete!=null)
                    cache.remove(keyToDelete);
            }
        } else {
            cache.put(key, value);
        }
        return key;
    }
    /**
     * Deletes the value for a given key from the cache if available.
     *
     * @param key the key that identifies the value.
     */
    public void delete(String key) {
        if(key==null)
            throw new NullPointerException();
        if (cache.containsKey(key)){
            cache.remove(key);
            cacheStrategy.deleteFromCache(key);
        }
    }
    public boolean isKeyInCache(String key){
        return this.cache.containsKey(key);
    }


    public void deleteTransferedKVPairs(HashMap<String,String> data){
        for (Map.Entry<String, String> entry : data.entrySet()) 
            this.delete(entry.getKey());

    }

}
