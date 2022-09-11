package de.tum.i13.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class DataLoader {
    private final static Logger LOGGER = Logger.getLogger(DataLoader.class.getName());
    String USER_DIR = System.getProperty("user.dir");
//    String MAIL_DIR = USER_DIR + File.separator+".."+File.separator +"data"+File.separator+"maildir"; // data dir is one adjacent to gr7/
    String MAIL_DIR = USER_DIR + File.separator+".."+File.separator +"data"+File.separator+"smaller_test_maildir"; // test_dataset
    Path MAIL_DIR_PATH;
    File MAIL_DIR_FILE;
    TreeMap<String, String> data; // should data be private here?
    private List<String> files;

    public DataLoader() throws IOException {
        this.files= new ArrayList<>();
        this.MAIL_DIR_PATH = Paths.get(MAIL_DIR);
        if (!Files.exists(this.MAIL_DIR_PATH))
            throw new IOException("Dataset was not found");
        this.MAIL_DIR_FILE = new File(this.MAIL_DIR);
        getFilesPaths(this.MAIL_DIR_FILE);          // this takes quite some time for the full data
}



    public void getFilesPaths(File maildir) {
        try {
            File[] files = maildir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
//                    System.out.println("directory:" + file.getCanonicalPath());  // for debugging remove later!!
                    getFilesPaths(file);
                } else {
                    String key = file.getCanonicalPath();
                    this.files.add(key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadAllData(){
        this.loadData(this.files.size());
    }

    public void loadData(int n){
        this.data = new TreeMap<>();
        Collections.shuffle(this.files);        // shuffle data
        this.files = this.files.subList(0,n);   // keep n subset
        for (String file : this.files)
        {
            String key = file;
            String value = readFile(key);
            this.data.put(key,value);
        }
        LOGGER.info("Numbers of files loaded " + data.size());

    }

    private static String readFile(String f)
    {
        StringBuilder lines = new StringBuilder();
        String lineText = null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.append(line);
            }
        }
        catch (IOException e)   {
            e.printStackTrace();
        }
        return lines.toString();
    }

    public Map.Entry<String, String> getSample(){ // for testing

        return this.data.firstEntry();

    }

//    public static void main(String[] args)throws Exception {   // testing and debugging
//
////        DataLoader enronDataset = new DataLoader();
////        enronDataset.loadData(200);
////        TreeMap<String, String> loadadData1 = enronDataset.data;
//
//        System.out.print("\n we stop here");
//
//
//    }

}
