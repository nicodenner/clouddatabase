package de.tum.i13.server.ECSConnection;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.HashMap;

import de.tum.i13.shared.Metadata;

public class MessageObject implements Serializable {
    private String message;
    private Metadata metadata;
    private HashMap<String,String> transferedData;
    private HashMap<String, PublicKey> users;

    private HashMap<String, String> keyOwners;

    public MessageObject(String message, Metadata metadata, HashMap<String,String> transferedData, HashMap<String, String> keyOwners){
        this.message = message;
        this.metadata = metadata;
        this.transferedData = transferedData;
        this.keyOwners = keyOwners;
    }

    public MessageObject (String message, Metadata metadata, HashMap<String,String> transferedData ){
        this.message = message;
        this.metadata = metadata;
        this.transferedData = transferedData;
    }
    public MessageObject (String message, HashMap<String, PublicKey> users){
        this.message = message;
        this.users = users;
    }
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public HashMap<String, String> getTransferedData() {
        return transferedData;
    }

    public void setTransferedData(HashMap<String, String> transferedData) {
        this.transferedData = transferedData;
    }
    public HashMap<String, PublicKey> getUsers() {
        return users;
    }
    public void setUsers(HashMap<String, PublicKey> users) {
        this.users = users;
    }

    public HashMap<String, String> getKeyOwners() {return this.keyOwners;}
}
