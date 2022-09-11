package de.tum.i13.server.kvcache;

import java.util.concurrent.ConcurrentHashMap;
public class LFU extends CacheStrategy {

    private ConcurrentHashMap<String, Integer> frequencies;
    public LFU (int capacity){
        this.capacity=capacity;
        frequencies = new ConcurrentHashMap<String, Integer>();
    }

    /**
     * adds the key to the cacheStrategy map.
     * removes the element with the least frquency of usage.
     * @param key the key that identifies the value.
     * @return the key of the evicted pair if the map is full, null otherwise.
     */
    @Override
    String addToCache(String key) {
        if(frequencies.size()<capacity){
            frequencies.put(key,1);
            return null;
        } else {
            // this returns the key associated with the least frequency 
            String keyToBeRemoved = frequencies.reduceKeys(5, (k1, k2) -> frequencies.get(k1).compareTo(frequencies.get(k2)) < 0 ? k1 : k2);
            frequencies.remove(keyToBeRemoved);
            frequencies.put(key,1);
            return keyToBeRemoved;
        }

    }

    /**
     * deletes the key from the cacheStrategy queue.
     * @param key the key that identifies the value.
     * @return true if the key is present in the map, false otherwise.
     */
    @Override
    boolean deleteFromCache(String key) {
        // map.remove returns the value when removing the key, null if key not found
        return frequencies.remove(key)!=null;
    }

    /**
     * rearrange the cache map to update the frquency of somne used key.
     * the frequency of the given key is incremented by 1 if it's found in the frequency Map
     * @param key the key that identifies the value.
     */
    @Override
    void rearrangeCache(String key) {
        Integer oldFrequency = frequencies.remove(key);
        if(oldFrequency!= null)
            frequencies.put(key, oldFrequency+1);
    }
    
}
