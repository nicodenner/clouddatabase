package de.tum.i13.server.kv;

public interface KVStore {

    /**
     * Inserts a key-value pair into the KVServer.
     *
     * @param key   the key that identifies the given value.
     * @param value the value that is indexed by the given key.
     * @param user the user that entered that key and is allowed to alter it.
     * @return a message that confirms the insertion of the tuple, update of a the tuple or an error.
     * @throws Exception if put command cannot be executed (e.g. not connected to any
     *                   KV server).
     */
    public String put(String key, String value, String user) throws Exception;

    /**
     * Retrieves the value for a given key from the KVServer.
     *
     * @param key the key that identifies the value.
     * @return the value, which is indexed by the given key.
     * @throws Exception if get command cannot be executed (e.g. not connected to any
     *                   KV server).
     */
    public String get(String key) throws Exception;

    /**
     * Deletes the value for a given key from the KVServer.
     *
     * @param key the key that identifies the value.
     * @param user the user that entered that key and is allowed to alter it.
     * @return the value, which is indexed by the given key.
     * @throws Exception if delete command cannot be executed (e.g. not connected to any
     *                   KV server).
     */
    public String delete(String key, String user) throws Exception;



}
