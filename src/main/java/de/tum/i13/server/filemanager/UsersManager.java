package de.tum.i13.server.filemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class UsersManager {
    public UsersManager(){
        createUsersFile();
    }

    private void createUsersFile() {
        Path p = Paths.get("users/users.txt");
        if(!Files.exists(p)){
            try {
                Files.createDirectory(Paths.get("users/"));
                File file = new File(p.toString());    
                file.createNewFile();
                }
                catch (IOException e) {
                    e.printStackTrace();
			}
            writeToFile(new HashMap<String, PublicKey>());
        } 
    }

    private HashMap<String, PublicKey> readFile(){
        HashMap<String, PublicKey> users = null;
        String fileName = "users/users.txt";
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            users = (HashMap<String, PublicKey>) ois.readObject();
            ois.close();
            fis.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    private void writeToFile(HashMap<String, PublicKey> users){
        String fileName = "users/users.txt";
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(users);
            oos.close();
            fos.close();
        } catch (FileNotFoundException e){
            System.out.println("Users file not found!");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public PublicKey getUserKey(String username){
        HashMap <String, PublicKey> users = readFile();
        if(users.size()==0)
            return null;
        return users.get(username);   
    }

    public synchronized void addUser(String username, PublicKey pk){
        HashMap<String, PublicKey> users = readFile();
        users.put(username, pk);
        writeToFile(users);
    }

    public synchronized void addUsers(HashMap<String, PublicKey> newUsers){
        HashMap<String, PublicKey> users = readFile();
        for (Map.Entry<String, PublicKey> user : newUsers.entrySet()) {
            users.put(user.getKey(), user.getValue());
        }
        writeToFile(users);
    }
    public synchronized HashMap<String,PublicKey> getAllUsers(){
        HashMap<String, PublicKey> res = readFile();
        return res;
    }
}
