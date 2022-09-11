package de.tum.i13.evaluation;

import de.tum.i13.client.ClientKVStore;
import de.tum.i13.ecs.StartECSServer;
import de.tum.i13.server.nio.StartSimpleNioServer;
import org.apache.commons.lang3.time.StopWatch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

// one client -> many servers
// many clients -> one server
// many -> many
// cashe size
// cashing strategy

public class Performance {
    private final static Logger LOGGER = Logger.getLogger(Performance.class.getName());
    private String ADDRESS;
    private String LOG_LEVEL;

    private Integer CACHE_SIZE; // [10,100,1000]
    private String CACHE_STRATEGY; // [FIFO,LRU,LFU]
    StopWatch stopWatch;
    int number_of_files;
    DataLoader dataLoader;
    public Performance () throws IOException {
        stopWatch= new StopWatch();
        dataLoader = new DataLoader();
        number_of_files = 0;
        ADDRESS = "127.0.0.1";
        LOG_LEVEL= "FINE";
        CACHE_SIZE = 1;
        CACHE_STRATEGY = "FIFO";
    }

    public static void  main(String[] args)throws Exception {   // testing and debugging


        Performance test = new Performance();
        test.number_of_files=16;

//        test.manyClientKV_manyServer(1,1); //1:1
//        test.manyClientKV_manyServer(5,1); //1:1
//        test.manyClientKV_manyServer(10,1); //1:1
//        test.manyClientKV_manyServer(1,5); //1:1
//        test.manyClientKV_manyServer(1,10); //1:1
//        test.manyClientKV_manyServer(5,5); //1:1
//        test.manyClientKV_manyServer(5,10); //1:1
//        test.manyClientKV_manyServer(10,5); //1:1
//        test.manyClientKV_manyServer(10,10); //1:1
//        test.manyClientKV_manyServer(20,10); //1:1
//        test.manyClientKV_manyServer(20,1); //1:1

//        test.CACHE_SIZE = 1000; // 100 ->500->1000
//        test.CACHE_STRATEGY = "FIFO"; // try each with a stable case size {"FIFO", "LFU", "LRU"};


//        test.manyClientKV_manyServer_putonly(1,5); //1:1
        test.manyClientKV_manyServer_getonly(1,2); //1:1

        System.out.print("\n Application end");
    }


    //    @Test
    public void manyClientKV_manyServer_putonly(int number_of_clients,int number_of_servers) throws InterruptedException, IOException { // make this executable
        this.dataLoader.loadData(this.number_of_files);           //max message size of 128 kbyte?!
        TreeMap<String, String> enron_data = this.dataLoader.data;

//
        launchECS();
        launchServer(number_of_servers);
        Thread.sleep(5000); // halt 5 seconds
        ClientKVStore[] clients = launchClientsKV(number_of_clients, number_of_servers);

        this.stopWatch.start();
        int number_clients_idx=0;
        for (int i =0 ;i<this.number_of_files;) {
            String Start_Key = enron_data.keySet().toArray()[i].toString();
            String End_Key = enron_data.keySet().toArray()[i+(int)(this.number_of_files/number_of_clients)-1].toString(); //this is "x2" as key string
            ClientKVStore client = clients[number_clients_idx];
            NavigableMap<String, String> temp_enron_data = enron_data.subMap(Start_Key, true, End_Key, true);
            for (String key: temp_enron_data.keySet()) {
                String req_key = key;
                String req_val = enron_data.get(key);
                client.put(req_key, req_val); // send request via client
            }
            i=i+(int)(this.number_of_files/number_of_clients);
            number_clients_idx++;

        }
        Thread.sleep(500);
        this.stopWatch.stop();

        // get benchmark info
        long elapsedTime= this.stopWatch.getTime(); //in mSec
        long throughput= ((long) this.number_of_files) /  elapsedTime;
        long latency = elapsedTime / ((long) this.number_of_files);

        saveReport("report",
                number_of_clients,number_of_servers,CACHE_STRATEGY,CACHE_SIZE
                ,elapsedTime,this.number_of_files,throughput,latency);

        this.stopWatch.reset();

    }

    public void manyClientKV_manyServer_getonly(int number_of_clients,int number_of_servers) throws InterruptedException, IOException { // make this executable
        this.dataLoader.loadData(this.number_of_files);           //max message size of 128 kbyte?!
        TreeMap<String, String> enron_data = this.dataLoader.data;

//
        launchECS();
        launchServer(number_of_servers);
        Thread.sleep(5000); // halt 5 seconds
        ClientKVStore[] clients = launchClientsKV(number_of_clients, number_of_servers);

        this.stopWatch.start();
        int number_clients_idx=0;
        for (int i =0 ;i<this.number_of_files;) {
            String Start_Key = enron_data.keySet().toArray()[i].toString();
            String End_Key = enron_data.keySet().toArray()[i+(int)(this.number_of_files/number_of_clients)-1].toString(); //this is "x2" as key string
            ClientKVStore client = clients[number_clients_idx];
            NavigableMap<String, String> temp_enron_data = enron_data.subMap(Start_Key, true, End_Key, true);
            for (String key: temp_enron_data.keySet()) {
                String req_key = key;
                String req_val = enron_data.get(key);
                client.get(req_key); // send request via client
            }
            i=i+(int)(this.number_of_files/number_of_clients);
            number_clients_idx++;

        }
        Thread.sleep(500);
        this.stopWatch.stop();

        // get benchmark info
        long elapsedTime= this.stopWatch.getTime(); //in mSec
        long throughput= ((long) this.number_of_files) /  elapsedTime;
        long latency = elapsedTime / ((long) this.number_of_files);

        saveReport("report",
                number_of_clients,number_of_servers,CACHE_STRATEGY,CACHE_SIZE
                ,elapsedTime,this.number_of_files,throughput,latency);

        this.stopWatch.reset();

    }
    private ClientKVStore[] launchClientsKV(int number_of_clients, int number_of_servers) throws IOException {
        ClientKVStore[] clients = new ClientKVStore[number_of_clients];

        for(int i= 0; i<number_of_clients; i++){
            int port = 5000;
            if(number_of_servers > 1){
                if(number_of_servers >= number_of_clients)
                    port = 5000 + i;
                else
                    port = 5001;
            }
            ClientKVStore client = new ClientKVStore();
            client.connect(ADDRESS,port);
            clients[i] = client;

        }

        return clients;
    }



    private void launchECS() throws InterruptedException, IOException {
        Integer port = 1000;
        Thread local_thread_ecs = new Thread() {
            @Override
            public void run() {
                try {
                    StartECSServer.main(new String[]{
                            "-a", ADDRESS, "-ll", LOG_LEVEL,});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        local_thread_ecs.start();
        Thread.sleep(2000); // 2 seconds
    }
    private void launchServer(int number_of_servers) throws InterruptedException, IOException {
        for (int i=0; i<number_of_servers; i++){
            Integer port = 5000 + i;
            Thread local_thread_server = new Thread() {
                @Override
                public void run() {
                    try {
                        StartSimpleNioServer.main(new String[]{"-p", port.toString(),
                                "-a", ADDRESS,
                                "-ll", LOG_LEVEL,
                                "-c", CACHE_SIZE.toString(),
                                "-s", CACHE_STRATEGY});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            local_thread_server.start(); // started the server
            Thread.sleep(2000);
        }

    }
    private void saveReport(String reportName, int number_of_clients, int number_of_servers,
                            String cacheStrategy, int cacheSize,long runtime,
                            int number_of_files,long throughput,long latency ) throws IOException {

        String USER_DIR = System.getProperty("user.dir");
        Path perfDir = Paths.get(USER_DIR + File.separator+ "perf_eval_reports");
        if (!Files.exists(perfDir))
            Files.createDirectories(perfDir);

        String fileName =  perfDir+ File.separator+reportName+ ".txt";


        FileWriter fw = new FileWriter(fileName, true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("-------------" + " Starting experiment with "  + "-------------");
        bw.newLine();
        bw.write("Number of clients: " + number_of_clients );
        bw.newLine();
        bw.write("Number of servers: " + number_of_servers );
        bw.newLine();
        bw.write("Used cache strategy: " + cacheStrategy );
        bw.newLine();
        bw.write("Used cache size: " + cacheSize );
        bw.newLine();
        bw.write("-------------" + " Results "  + "-------------");
        bw.newLine();
        bw.write("Total elapsed time in mSec: " + runtime);
        bw.newLine();
        bw.write("Number of operations: " + number_of_files);
        bw.newLine();
        bw.write("Throughput: " + throughput + " -------------" + " Latency: " + latency);
        bw.newLine();
        bw.write("-------------" + "Experiment Finished" + "-------------");
        bw.newLine();
        bw.close();

    }

}
