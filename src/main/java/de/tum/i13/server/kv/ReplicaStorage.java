package de.tum.i13.server.kv;

import java.util.HashMap;

public class ReplicaStorage {
    private HashMap<String, String> storage;
    private String startHash;
    private String endHash;
    private boolean inUse;
    public ReplicaStorage(HashMap<String, String> storage, String startHash, String endHash){
        this.storage = storage;
        this.startHash = startHash;
        this.endHash = endHash;
        this.inUse = true;
    }
    public ReplicaStorage(){
        this.storage = new HashMap<>();
        this.inUse = false;
    }
    public HashMap<String, String> getStorage() {
        return storage;
    }
    public void setStorage(HashMap<String, String> storage) {
        this.storage = storage;
    }
    public String getStartHash() {
        return startHash;
    }
    public void setStartHash(String startHash) {
        this.startHash = startHash;
    }
    public String getEndHash() {
        return endHash;
    }
    public void setEndHash(String endHash) {
        this.endHash = endHash;
    }
    public boolean isInUse() {
        return inUse;
    }
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
    
}
