package de.tum.i13.server.filemanager;

import de.tum.i13.shared.Constants;
import de.tum.i13.shared.Metadata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class OwnerManager {
    private Path path;

    public OwnerManager(Path path){
        this.path=path;
        createTheFiles(Constants.OWNER_FILENAMES, path);
    }
    /**
     * creates the fixed storage files if they don't exist.
     *
     * @param fileNames the name of the storage files fetched from the Constants class.
     * @param path the folder where the file will reside
     */
    private void createTheFiles(String [] fileNames, Path path) {
        for(String fn : fileNames){
            createAFile(fn, path);
        }
    }

    private void createAFile(String fileName, Path path) {
        Path p = Paths.get(path+"/"+fileName);
        if(!Files.exists(p)){
            File file = new File(p.toString());
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            writeToFile(new HashMap<String, String>(), fileName);
        }
    }

    public boolean isOwner(String key, String user) {
        String fileName = getTargetFile(key);
        HashMap <String, String> data = readFile(fileName);
        String valueOrNull = data.get(key);

        if (valueOrNull == null) {
            // there is no ower to this key, so we are good to go
            return true;
        } else {
            if (valueOrNull.equals(user)) {
                // we have the owner
                return true;
            } else {
                // there is an owner to this key, but a different one
                return false;
            }
        }
    }

    /**
     * reads the cache HashMap from the storage file.
     * @param fileName the file name where the HashMap will be loaded.
     * @return HashMap of the key-value pairs
     */
    private HashMap<String, String> readFile(String fileName){
        HashMap<String, String> data = null;
        fileName = this.path.toString() + "/" + fileName;
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            data = (HashMap<String, String>) ois.readObject();
            ois.close();
            fis.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    /**
     * writes the cache HashMap into the storage file.
     * @param data  the cache HashMap which will be stored in the file
     * @param fileName the file name where the HashMap will be stored.
     */
    private void writeToFile(HashMap<String, String> data, String fileName){
        fileName = this.path.toString() + "/" + fileName;
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(data);
            oos.close();
            fos.close();
        } catch (FileNotFoundException e){
            System.out.println("Data files not found!");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * gets the value for a key from the file storage.
     *
     * @param key   the key that identifies the given value.
     * @return the value, which is indexed by the given key.
     */
    public String get(String key){
        String fileName = getTargetFile(key);
        HashMap <String, String> data = readFile(fileName);
        if(data.size()==0)
            return null;
        return data.get(key);
    }

    /**
     * Inserts a key-value pair into the files.
     *
     * @param key   the key that identifies the given value.
     * @param value the value that is indexed by the given key.
     * @return the value associated with a key, null otherwise.
     */
    public synchronized String put(String key, String value){
        String fileName = getTargetFile(key);
        HashMap<String, String> data = readFile(fileName);
        String prev = data.put(key, value);
        writeToFile(data, fileName);
        return prev;
    }

    /**
     * Deletes the value for a given key from the files.
     * @param key the key that identifies the value.
     * @return the value deleted for the given key, null otherwise.
     */
    public synchronized String delete(String key){
        String fileName = getTargetFile(key);
        HashMap <String, String> data = readFile(fileName);
        if(data.size()==0)
            return null;
        String value = data.remove(key);
        if(value != null)
            writeToFile(data, fileName);
        return value;
    }

    /**
     * returns the list of the transfered key-value pairs.
     *
     * @param predHash the hash of the newly-added server.
     * @return string of the key-value pairs concatenated.
     */
    public synchronized HashMap<String,String> getTransferedKVPairs(String predHash, String predHashEnd){
        HashMap<String, String> res = new HashMap<>();
        for(String fName: Constants.FILENAMES){
            HashMap<String, String> data = readFile(fName);
            System.out.println(fName+ "::the data map size: " + data.size());
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if(predHash.equals("ALL")){
                    res.putIfAbsent(entry.getKey(), entry.getValue());
                }
                else if(Metadata.hashInRange(entry.getKey(), predHash, predHashEnd)){
                    res.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
        return res;
    }

    /**
     * returns the map of the all key-value pairs.
     *
     * @return map of the key-value pairs .
     */
    public synchronized HashMap<String,String> getAllPairs(){
        HashMap<String, String> res = new HashMap<>();
        for(String fName: Constants.FILENAMES){
            HashMap<String, String> data = readFile(fName);
            res.putAll(data);
        }
        return res;
    }

    /**
     * deletes the transfered key-value pairs.
     * @param data map of the transfered key-value pairs.
     */
    public synchronized void deleteTransferedKVPairs(HashMap<String,String> data){
        for (Map.Entry<String, String> entry : data.entrySet())
            this.delete(entry.getKey());
    }


    /**
     * determines the target file for a given key.
     *
     * @param key the key that identifies the value.
     * @return the file name where the key-value pair will be stored.
     */
    private String getTargetFile(String key){
        if(startsBetween(key, 'a', 'h'))
            return Constants.FILENAMES[0];
        else if(startsBetween(key, 'i', 'q'))
            return Constants.FILENAMES[1];
        else if(startsBetween(key, 'r', 'z'))
            return Constants.FILENAMES[2];
        else
            return Constants.FILENAMES[3];
    }

    private static boolean startsBetween(String key, char lowest, char highest) {
        char c=key.toLowerCase().charAt(0);
        return c >= lowest && c <= highest;
    }
}
