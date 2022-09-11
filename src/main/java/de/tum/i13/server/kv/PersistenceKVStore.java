package de.tum.i13.server.kv;

import de.tum.i13.security.RSA;
import de.tum.i13.server.ECSConnection.ECSThread;
import de.tum.i13.server.filemanager.FileManager;
import de.tum.i13.server.filemanager.OwnerManager;

import de.tum.i13.server.filemanager.UsersManager;
import de.tum.i13.server.kvcache.KVCache;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.ServerEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Logger;

public class PersistenceKVStore implements KVStore {

    private String cacheStrategy;
    private KVCache cache;
    private FileManager fileManager;
    private UsersManager usersManager;
    private OwnerManager ownerManager;
    private int cacheCapacity;
    private Path path;
    private Metadata metadata;
    private String state;
    private ReplicaStorage replica_1;
    private ReplicaStorage replica_2;
    private ECSThread ecsthread;
    private Config config;
    private String generatedWord;
    public static Logger logger = Logger.getLogger(PersistenceKVStore.class.getName());

    public PersistenceKVStore(Config config) throws IllegalArgumentException, NullPointerException{
        
        this.cacheStrategy = config.cacheStrategy;
        this.cacheCapacity = config.cacheSize;
        this.config = config;
        this.path = config.dataDir;
        this.fileManager = new FileManager(this.path);
        this.ownerManager = new OwnerManager(this.path);
        this.cache= new KVCache(this.cacheCapacity, this.cacheStrategy);
        this.state = Constants.SERVER_AVAILABLE;
        this.ecsthread =new ECSThread(this, config.listenaddr, config.port, config.bootstrap);
        ecsthread.start();
        this.metadata = ecsthread.getMetadata();
        this.replica_1 = new ReplicaStorage();
        this.replica_2= new ReplicaStorage();
        this.usersManager = new UsersManager();
        this.generatedWord="";
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Closing the kv server");
            }
        });
    }
        /**
     * Inserts a key-value pair into the KVServer.
     *
     * @param key   the key that identifies the given value.
     * @param value the value that is indexed by the given key
     * @param user the user that entered that key and is allowed to alter it.
     * @return a message that confirms the insertion of the tuple or update of the tuple or the server state.
     * @throws Exception if put command cannot be executed (e.g. not connected to any
     *                   KV server).
     */

    @Override
    public String put(String key, String value, String user) throws Exception {
        if(this.state.equals(Constants.SERVER_AVAILABLE)){
            if(checkResponsibility(key)){
                if(this.ownerManager.isOwner(key, user)) {
                    String type = this.fileManager.put(key, value);
                    this.ownerManager.put(key, user);
                    // update cache even if it's an update command
                    this.cache.put(key, value);

                    return type==null?"insert":"update";
                } else {
                    return "put_error current user is not the owner of this key, no write requests allowed";
                }
            } else 
                return Constants.SERVER_NOTRESPONSIBLE;
        }
        // Either the server is stopped or in write_lock
        return this.state; 

    }

    /**
     * Retrieves the value for a given key from the KVServer.
     *
     * @param key the key that identifies the value.
     * @return the value, which is indexed by the given key, null if not found or the server state.
     * @throws Exception if get command cannot be executed (e.g. not connected to any
     *                   KV server).
     */
    @Override
    public String get(String key) throws Exception {
        if(this.state.equals(Constants.SERVER_AVAILABLE) || this.state.equals(Constants.SERVER_WRITELOCK)){
            if(checkResponsibility(key)){
                boolean isValueInCache = this.cache.isKeyInCache(key);
                // if value is in cache, return it
                if(isValueInCache){
                    String value = this.cache.get(key);
                    logger.info("Got value for the key: "+ key +" from cache");
                    return value;
                } else {
                    // else we check if it is in the file manager, if yes we add it to cache else we return null 
                    String value = this.fileManager.get(key);
                    if(value!= null){
                        logger.info("Got value for the key: "+ key +" from file storage");
                        this.cache.put(key, value);
                    }
                    return value;
                }
                // Check if replica_1 is responsible
            } else if (replica_1.isInUse() && Metadata.hashInRange(key, replica_1.getStartHash(), replica_1.getEndHash())){
                logger.info("Got value for the key: "+ key +" from replica_1");
                return replica_1.getStorage().get(key);
                // Check if replica_2 is responsible
            } else if (replica_2.isInUse() && Metadata.hashInRange(key, replica_2.getStartHash(), replica_2.getEndHash())){
                logger.info("Got value for the key: "+ key +" from replica_2");
                return replica_2.getStorage().get(key);
            } else
                return Constants.SERVER_NOTRESPONSIBLE;
        }
        return this.state;

    }
        /**
     * Deletes the value for a given key from the KVServer.
     *
     * @param key the key that identifies the value
     * @param user the user that entered that key and is allowed to alter it.
     * @return success message if deleted, error message/server state otherwise.
     * @throws Exception if delete command cannot be executed (e.g. not connected to any
     *                   KV server).
     */
    @Override
    public String delete(String key, String user) throws Exception {
        if(this.state.equals(Constants.SERVER_AVAILABLE)){
            if(checkResponsibility(key)){
                if (this.ownerManager.isOwner(key, user)) {
                    String value = this.fileManager.delete(key);
                    this.ownerManager.delete(key);
                    if(value==null){
                        // key is not in the storage so we do nothing
                        return Constants.NOT_FOUND;
                    } else {
                        // we check if the key is in cache to delete it from there too.
                        this.cache.delete(key);
                        return Constants.FOUND_DELETED;
                    }
                } else {
                    return "delete_error current user is not the owner of this key, no write requests allowed";
                }
            } else 
                return Constants.SERVER_NOTRESPONSIBLE;
        }
        return this.state;

    }

    public String register(String username, String publickey){
        if(this.state.equals(Constants.SERVER_AVAILABLE)){
            if(this.usersManager.getUserKey(username)!=null){
                return Constants.REGISTER_ERROR;
            } else {
                try {
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publickey));
                    PublicKey pubKey = kf.generatePublic(keySpecX509);
                    this.usersManager.addUser(username, pubKey);
                    this.ecsthread.triggerUsersBroadcast();
                    return Constants.REGISTER_SUCCESS;
                } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return this.state;

    }

    public String login(String username){
        if(this.state.equals(Constants.SERVER_AVAILABLE)){
            if(this.usersManager.getUserKey(username)==null){
                return Constants.LOGIN_ERROR + " :username not found!";
            } else {
                try {
                    this.generatedWord = generateRandomWord();
                    PublicKey pk = this.usersManager.getUserKey(username);    
                    String encryptedMsg = RSA.encrypt(this.generatedWord, pk);
                    return "decrypt_this "+ encryptedMsg;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return this.state;
    }
    
    public boolean check(String word) {
        return word.equals(this.generatedWord);
    }
    public String getKeyRangeRead() {
        if(this.state.equals(Constants.SERVER_STOPPED))
            return null;
        return this.metadata.keyrangeRead();
    }

    public String getKeyRange() {
        if(this.state.equals(Constants.SERVER_STOPPED))
            return null;
        return this.metadata.toClientString();
    }
    
    /**
     * Check if the current server is responsible for the given key.
     *
     * @param key  the key that identifies the given value.
     * @return a true if the server is responsible, false otherwise.
     */
    public boolean checkResponsibility(String key){
        String ipAndPort = this.config.listenaddr + ":" + this.config.port;
        ServerEntry se = this.metadata.getServerEntry(ipAndPort);
        if(se == null) {
            return false;
        }

        return Metadata.hashInRange(key, se.getStartIndex(), se.getEndIndex());
    }

    public void triggerShutdown(){
        logger.info("Shutdown triggered from the ECS thread");
        System.exit(0);
    }

    public String generateRandomWord(){
        Random random = new Random();
        char[] word = new char[random.nextInt(8)+3]; // words of length 3 through 10
        for(int j = 0; j < word.length; j++)
            word[j] = (char)('a' + random.nextInt(26));
        return new String(word);
}
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public FileManager getFileManager() {
        return fileManager;
    }
    public KVCache getCache() {
        return cache;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Metadata getMetadata() {
        return this.metadata;
    }
    public ReplicaStorage getReplica_1() {
        return replica_1;
    }
    public void setReplica_1(ReplicaStorage replica_1) {
        this.replica_1 = replica_1;
    }
    public ReplicaStorage getReplica_2() {
        return replica_2;
    }
    public void setReplica_2(ReplicaStorage replica_2) {
        this.replica_2 = replica_2;
    }
    public ECSThread getEcsthread() {
        return ecsthread;
    }
    public void setEcsthread(ECSThread ecsthread) {
        this.ecsthread = ecsthread;
    }
    public UsersManager getUsersManager() {
        return usersManager;
    }
    public void setUsersManager(UsersManager usersManager) {
        this.usersManager = usersManager;
    }

    public OwnerManager getOwnerManager() {
        return this.ownerManager;
    }
    
}
