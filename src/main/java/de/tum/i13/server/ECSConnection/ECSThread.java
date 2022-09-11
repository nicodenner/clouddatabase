package de.tum.i13.server.ECSConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import de.tum.i13.ecs.ECSSocketManager;
import de.tum.i13.server.kv.PersistenceKVStore;
import de.tum.i13.server.kv.ReplicaStorage;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.NeighborsAndSelf;
import de.tum.i13.shared.ServerEntry;

/*
 *  class to implement the ecs-server communications
 *  it's later extended to also include the server-server communications
 *  as the commands are intertwined
 */
public class ECSThread extends Thread{
    public static Logger logger = Logger.getLogger(ECSThread.class.getName());
    private PersistenceKVStore kvs;
    private String address;
    private int port;
    private InetSocketAddress ecsAddress;
    private int freePort;
    private Metadata metadata;
    private boolean connected;
    private boolean isLeaving;
    private boolean isReplicated1;
    private boolean isReplicated2;
    
    //Address and port of the server successor
    private String succAddress;
    private int succClientPort;
    private int succServerPort;

    //socket encapsulates the behavior of the active side (client)
    private Socket socket;

    //serverSocket encapsulates the behavior of the passive side (server)
    private ServerSocket serverSocket;

    //Used for exchanging string messages
    private ObjectInputStream ecsServerIs;
    private ObjectOutputStream ecsServerOs;

    private HashMap<String, String> transferredData = new HashMap<>();

    public ECSThread(PersistenceKVStore kvs, String address, int port, InetSocketAddress ecsAddress){
        this.kvs = kvs;
        this.address = address;
        this.port = port;
        this.ecsAddress = ecsAddress;
        this.socket = new Socket();
        this.metadata = new Metadata();
        this.ecsServerIs = null;
        this.ecsServerOs = null;
        this.connected = false;
        this.isLeaving = false;
        this.isReplicated1 = false;
        this.isReplicated2 = false;
    }

    @Override
    public void run() {
        // Shutdown hook to trigger the leave message
        Runtime.getRuntime().addShutdownHook(new Thread() {
             @Override
             public void run() {
                 triggerLeave();
             }
        });
        if (!this.isLeaving) {
            startServerSocket();
            connect();
            processCommands();
        }
    }

    /**
     * Start the serversocket resposible for accepting connections.
     */
    public void startServerSocket(){
        try {
            this.freePort = getFreePort();
            this.serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(this.address, freePort));
            logger.info("Starting the Server Socket on address: "+ this.address +" and port: "+ freePort);

        } catch (IOException e) {
            logger.severe("Error starting the Server Socket");
        }
    }

    /**
     * connect to the ECS and send the join message
     */
    private void connect() {
        logger.info("Starting the ECS connection");
        try{
            this.socket = new Socket(this.ecsAddress.getHostName(), this.ecsAddress.getPort());
            this.ecsServerOs = new ObjectOutputStream(this.socket.getOutputStream());
            this.ecsServerOs.flush();
            this.ecsServerIs =new ObjectInputStream(this.socket.getInputStream());
            this.connected = true;
            joinNetwork();
        } catch (IOException e){
            e.printStackTrace();
            logger.severe("Error connecting to the ECS! ");
        }
    }

    private void joinNetwork() {
        if(!connected) {
            connect();
        }
        try {
            String message = "join " + this.address + " " + this.port + " " + this.freePort;
            logger.info("Sending join message: " + message);
            message = message+"\r\n";
            MessageObject mobj = new MessageObject(message, null, null);
            this.ecsServerOs.writeObject(mobj);
            this.ecsServerOs.flush();

        } catch (IOException e) {
            logger.severe("Error sending join message to the ECS!");
            e.printStackTrace();
        }
    }

    /**
     * process command sent from the ecs and the servers
     */
    private void processCommands(){
        while(!this.kvs.getState().equals(Constants.SERVER_STOPPED)){
            try {
                logger.info("Processing commands from the ECS/Servers...");
                Socket s = this.serverSocket.accept();
                this.ecsServerOs = new ObjectOutputStream(s.getOutputStream());
                this.ecsServerOs.flush();                
                this.ecsServerIs =new ObjectInputStream(s.getInputStream());
                
                MessageObject mobj = (MessageObject) this.receiveMessageObject();
                String in = mobj.getMessage().trim();
                String[] commandArr = in.split(" ");

                logger.info("New command received from ECS/Servers: " + in);
                switch (commandArr[0]) {
                    case ("join_success"): //ECS
                        handleJoinSuccess(commandArr, mobj); break;
                    case ("enable_write_lock"): //ECS
                        triggerDataTransfer(commandArr); break;
                    case ("disable_write_lock"): //ECS
                        disableLock(); break;
                    case ("metadata_update"): //ECS
                        handleMetadataUpdate(mobj); break;
                    case ("users_update"): //ECS
                        handleUsersUpdate(mobj); break;
                    case ("transfer_data_pred_leaving"): //ECS
                         handleDataFromECS(mobj);break;
                    case ("heartbeat"): //ECS
                        handleHeartBeat(commandArr); break;
                    case ("start_gossip"): //ECS
                        handleStartGossip(); break;
                    case ("gossip"): //SERVER
                        handleGossip(); break;
                    case ("start_data_receipt")://SERVER
                        startDataReceipt(mobj); break;
                    case ("successful_data_receipt")://SERVER
                        handleSuccessfulReceipt(); break;
                    case ("receive_replica_data"):
                        handleReceivingReplicaData(mobj); break;
                    case ("eventual_consistency"):
                        handleEventualConsistency(mobj); break;
                    default:
                        logger.severe("We cant deal with the command: " + in);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleReceivingReplicaData(MessageObject mobj) {
        String [] commandArr = mobj.getMessage().trim().split(" ");
        logger.info("Received replicated data from server, acting as replica_"+commandArr[3]);
        String startIndex = commandArr[1];
        String endIndex = commandArr[2];
        if(commandArr[3].equals("1"))
            this.kvs.setReplica_1(new ReplicaStorage(mobj.getTransferedData(),startIndex, endIndex));
        else
            this.kvs.setReplica_2(new ReplicaStorage(mobj.getTransferedData(),startIndex, endIndex));
        // Check if this server already did its replication
        if(!isReplicated1){
            this.triggerReplicatedDataTransfer(1);
            this.isReplicated1 = true;
        } else if(!isReplicated2){
            this.triggerReplicatedDataTransfer(2);
            this.isReplicated2 = true;
        }
    }

    /**
     * Receives data of the exiting server from the ecs
     * @param mobj message object containing the he map of key-value pairs from the exiting server.
     */
    private void handleDataFromECS(MessageObject mobj) {
        logger.info("Received data of a leaving server from the ECS ");
        for (Map.Entry<String, String> entry : mobj.getTransferedData().entrySet()) {
            this.kvs.getFileManager().put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : mobj.getKeyOwners().entrySet()) {
            this.kvs.getOwnerManager().put(entry.getKey(), entry.getValue());
        }
        // update the replicas if needed.
        if(this.metadata.numberOfServers()>=3){
            /* we check if the number of server is more than 3
                */
            this.triggerReplicatedDataTransfer(1);
            this.isReplicated1 = true;
        }
    }

    private void handleHeartBeat(String[] commandArr) throws IOException {
        logger.info("Received a heartbeat message from the ecs, now reply to it");
        this.sendToECS("still_alive " + commandArr[1] + " " + commandArr[2]);
    }

    /**
     * If the server is the starting point of a gossip round, he will initiate the gossip to its successor.
     */
    private void handleStartGossip() {

        if (this.metadata.getData().size() == 1) {
            // we are the only one
            logger.info("Gossip round complete, we return to the ECS (since we are the only server)");

            // we stop gossip here and return to the ECS
            try {
                sendToECS("gossip_success");
                return;
            } catch (IOException ex) {
                logger.severe("Unable to contact ECS for gossip success");
                return;
            }
        }

        ServerEntry successor = this.metadata.getNeighbors(address, port).getSuccessor();
        logger.info("Gossip to successor: " + successor.getIp() + ":" + successor.getServerPort());

        try {
            sendToServer("gossip", successor.getIp(), successor.getServerPort());
        } catch (IOException e) {
            logger.severe("Unable to contact successor");
            try {
                sendToECS("gossip_fail " + successor.getIp() + " " + successor.getServerPort());
            } catch (IOException ex) {
                logger.severe("Unable to contact ECS for gossip fail");
            }
        }
    }

    /**
     * If a gossip message is has been received, the message is passed on to its successor to check if he is still there.
     * When the gossip returns to the starting server he knows that all servers are still there, and he can report to the ECS
     * that no failure was detected.
     */
    private void handleGossip() {
        Map.Entry<BigInteger, ServerEntry> firstServer = this.metadata.getData().firstEntry();

        if (firstServer != null && this.address.equals(firstServer.getValue().getIp()) && this.freePort == firstServer.getValue().getServerPort()) {
            // we are the server which initiated the gossip, so we return to ecs after a successful round
            logger.info("Gossip round complete, we return to the ECS since we initiated the round");

            // we stop gossip here and return to the ECS
            try {
                sendToECS("gossip_success");
                return;
            } catch (IOException ex) {
                logger.severe("Unable to contact ECS for gossip success");
                return;
            }
        }

        ServerEntry successor = this.metadata.getNeighbors(address, port).getSuccessor();
        logger.info("Gossip to successor: " + successor.getIp() + ":" + successor.getServerPort());

        try {
            sendToServer("gossip", successor.getIp(), successor.getServerPort());
        } catch (IOException e) {
            logger.severe("Unable to contact successor");
            try {
                sendToECS("gossip_fail " + successor.getIp() + " " + successor.getServerPort());
            } catch (IOException ex) {
                logger.severe("Unable to contact ECS for gossip fail");
            }
        }
    }

    /**
     * handles the successful join on the server side and set the successor's information
     * @param commandArr array containing ip, clientPort and serverPort of the successor.
     */
    private void handleJoinSuccess(String[] commandArr, MessageObject mobj) {
        logger.info("Handling a successful join...");
        if(commandArr.length>=4){
            this.succAddress = commandArr[1];
            this.succClientPort = Integer.parseInt(commandArr[2]);
            this.succServerPort = Integer.parseInt(commandArr[3]);
        }
        if (commandArr.length==5){
            handleUsersUpdate(mobj);
        }
    }

    /**
     * start the data transfer and change the server state to locked
     * @param commandArr array containing ip, clientPort and serverPort of the successor.
     */
    private void triggerDataTransfer(String[] commandArr) {
        this.succAddress = commandArr[1];
        this.succClientPort = Integer.parseInt(commandArr[2]);
        this.succServerPort = Integer.parseInt(commandArr[3]);

        if(this.succAddress!=null){
            this.kvs.setState(Constants.SERVER_WRITELOCK);
            this.startDataTransfer();

            logger.info("current metadata in the ecs thread is: " + this.metadata.toString());
            logger.info("Triggering data transfer...");
            return;
        }
        logger.info("SUCC ADDRESS IS NULL");
    }


    private void startDataTransfer() {
        try {
            String succIpAndPort = this.succAddress+":"+this.succClientPort;
            logger.info("START DATA TRANSFER: " + succIpAndPort);
            String succHash = this.metadata.getServerEntry(succIpAndPort).getStartIndex();
            String succHashEnd = this.metadata.getServerEntry(succIpAndPort).getEndIndex();

            String senderState = "not_leaving";

            logger.info("Starting data transfer from server "+ this.address+":"+this.port +" to server " + succIpAndPort);

            if(isLeaving){
                succHash = "ALL";
                senderState = "leaving";
            }
            HashMap<String,String> data = this.kvs.getFileManager().getTransferedKVPairs(succHash, succHashEnd);
            HashMap<String,String> owners = this.kvs.getOwnerManager().getTransferedKVPairs(succHash, succHashEnd);

            //connecting to the successor server.
            this.socket = new Socket(this.succAddress, this.succServerPort);
            this.ecsServerOs = new ObjectOutputStream(this.socket.getOutputStream());
            this.ecsServerOs.flush();
            this.ecsServerIs =new ObjectInputStream(this.socket.getInputStream());
            String reply = "start_data_receipt " + this.address + " " + this.port + " " + this.freePort + " " + senderState + "\r\n";
            logger.info("Sending a message:("+ reply +") to the server at "+ this.succAddress + ":" + this.succClientPort);
            logger.info("Sending data object ...");

            MessageObject mobj = new MessageObject(reply, null, data, owners);
            this.ecsServerOs.writeObject(mobj);
            this.ecsServerOs.flush();

        } catch (IOException e) {
            logger.severe("Error transferring data");
        }
    }

    private void startDataReceipt(MessageObject mobj){
        try {
            logger.info("Starting data receipt");
            String [] commandArr = mobj.getMessage().trim().split(" ");
            writeDataToFiles(mobj.getTransferedData(), mobj.getKeyOwners(), commandArr[1], commandArr[2], commandArr[3], commandArr[4]);
            logger.info("Received new data, updating the data on the replicas if needed...");
            if(this.metadata.numberOfServers()>=3 && this.metadata.numberOfServers()%2==0){
                /* we check if the number of server is more than 3
                 */
                this.triggerReplicatedDataTransfer(1);
                this.isReplicated1 = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * write the hashmap received to the server data files and sends the confirmation message to the sender server if it's not leaving
     * @param data the map of key-value pairs.
     * @param senderIp The ip of the sender server
     * @param senderClientPort the clientPort of the sender server
     * @param senderServerPort the serverPort of the sender server
     */
    private void writeDataToFiles(HashMap<String,String> data, HashMap<String,String> owners, String senderIp, String senderClientPort, String senderServerPort, String state) throws IOException {
        logger.info("Writing data to the server's files...");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            this.kvs.getFileManager().put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : owners.entrySet()) {
            this.kvs.getOwnerManager().put(entry.getKey(), entry.getValue());
        }

        if(state.equals("not_leaving")){
            logger.info("Sending successful_data_receipt message to the sender server...");

            //connecting to the successor server.
            this.socket = new Socket(senderIp, Integer.parseInt(senderServerPort));
            this.ecsServerOs = new ObjectOutputStream(this.socket.getOutputStream());
            this.ecsServerOs.flush();
            this.ecsServerIs =new ObjectInputStream(this.socket.getInputStream());
            String reply = "successful_data_receipt\r\n";
            logger.info("Sending a message:("+ reply +") to the server at "+ senderIp + ":" + senderClientPort);
            MessageObject mobj = new MessageObject(reply, null, data, owners);
            this.ecsServerOs.writeObject(mobj);
            this.ecsServerOs.flush();
        } else {
            logger.info("The successful_data_receipt message is not sent because the sender server is leaving");

        }
    }

    private void handleSuccessfulReceipt() throws IOException {
        logger.info("Sending data_transfer_complete message to the ECS...");
        this.sendToECS("data_transfer_complete "+ this.address + " " + this.port + " " + this.freePort);
    }

    /**
     * Receives and sets the metadata.
     * @param mobj message object containing the metadata
     */
    private void handleMetadataUpdate(MessageObject mobj) {
        this.metadata = mobj.getMetadata();
        this.kvs.setMetadata(mobj.getMetadata());
        logger.info("Received new Metadata from ECS: " +  this.metadata.toString());    
        checkReplicationState();
    }

    public void handleUsersUpdate(MessageObject mobj){
        this.kvs.getUsersManager().addUsers(mobj.getUsers());
        logger.info("Received new UsersList!");    

    }
    private void checkReplicationState() {
        // if the servers are less than 3, then there is nothing to be done
        if(this.metadata.numberOfServers()<3) {
            logger.info("No replicated data change is required: Servers are less than 3");
            // reset the replicated data 
            this.kvs.setReplica_1(new ReplicaStorage());
            this.kvs.setReplica_2(new ReplicaStorage());
            return;
        }
        // We check if we just hit 3 servers, then we send data to the replica servers
        if(this.metadata.numberOfServers()>=3){
            // logger.info("Sending the data to replica_1 and replica_2");
            // this.triggerReplicatedDataTransfer();
            this.isReplicated1 = false;
            this.isReplicated2 = false;
            return;
        }
    }

    private void triggerReplicatedDataTransfer(int i) {
        NeighborsAndSelf replicas = this.metadata.getCandidateReplicas(address, port);
        HashMap <String,String> data = this.kvs.getFileManager().getAllPairs();
        String message = "receive_replica_data " + replicas.getSelf().getStartIndex() + " " + replicas.getSelf().getEndIndex();
        // Sending to the first replica
        if(i==1)
        this.sendDataToTheServer(replicas.getSuccessor(), message + " 1", data);
        
        if(i==2)
        // Sending to the second replica
        this.sendDataToTheServer(replicas.getPredecessor(), message + " 2", data);
    }

    public void makeDataConsistentAgain(boolean replica_1InUse, boolean replica_2InUse) {
        if(this.metadata.numberOfServers()>=3){
            NeighborsAndSelf replicas = this.metadata.getCandidateReplicas(address, port);
            HashMap <String,String> data = this.kvs.getFileManager().getAllPairs();
            String message = "eventual_consistency "+replicas.getSelf().getStartIndex() + " " + replicas.getSelf().getEndIndex();
            // Sending to the first replica
            if(replica_1InUse)
                this.sendDataToTheServer(replicas.getSuccessor(), message + " 1", data);
            
            // Sending to the second replica
            if(replica_2InUse)
                this.sendDataToTheServer(replicas.getPredecessor(), message + " 2", data);
        }

    }

    private void handleEventualConsistency(MessageObject mobj) {
        logger.info("Handling consistency of the replica");
        String [] commandArr = mobj.getMessage().trim().split(" ");
        String startIndex = commandArr[1];
        String endIndex = commandArr[2];
        if(commandArr[3].equals("1"))
            this.kvs.setReplica_1(new ReplicaStorage(mobj.getTransferedData(),startIndex, endIndex));
        else
            this.kvs.setReplica_2(new ReplicaStorage(mobj.getTransferedData(),startIndex, endIndex));
    }

    private void sendDataToTheServer(ServerEntry server,String message, HashMap<String,String> data){
        try{
            //connecting to the successor server.
            this.socket = new Socket(server.getIp(), server.getServerPort());
            this.ecsServerOs = new ObjectOutputStream(this.socket.getOutputStream());
            this.ecsServerOs.flush();
            this.ecsServerIs =new ObjectInputStream(this.socket.getInputStream());
            String reply = message+"\r\n";
            logger.info("Sending a message:("+ reply +") to the server at "+ server.getIp() + ":" + server.getClientPort());
            MessageObject mobj = new MessageObject(reply, null, data);
            this.ecsServerOs.writeObject(mobj);
            this.ecsServerOs.flush();
        }catch (IOException e) {
            logger.severe("Error transferring data");
        }
    }

    private void disableLock() {
        logger.info("Write lock: Disabled, now deleting transferred data and updating the data on the replicas...");
        this.kvs.getFileManager().deleteTransferedKVPairs(this.transferredData);
        this.kvs.getCache().deleteTransferedKVPairs(this.transferredData);
        this.transferredData = new HashMap<>();
        this.kvs.setState(Constants.SERVER_AVAILABLE);
        if(this.metadata.numberOfServers()>=3){
            /* we check if the number of server is more than 3
             */
            this.triggerReplicatedDataTransfer(1);
            this.isReplicated1 = true;
        }
    }

    /*
     * send the leave message to the ecs to start the leaving protocol
     */
    public void triggerLeave() {
        try {
            this.kvs.setState(Constants.SERVER_STOPPED);
            this.isLeaving = true;
            ServerEntry successor = this.metadata.getNeighbors(this.address, this.port).getSuccessor();
            String x = "";
            HashMap <String, String> map = null;
            HashMap <String, String> owners = null;
            if(successor!=null){
                this.kvs.setState(Constants.SERVER_WRITELOCK);
                this.succAddress = successor.getIp();
                this.succClientPort = successor.getClientPort();
                this.succServerPort = successor.getServerPort();
                x = " " + this.succAddress + " " + this.succClientPort + " " + this.succServerPort + " ";
                map = this.kvs.getFileManager().getTransferedKVPairs("ALL", "");
                owners = this.kvs.getOwnerManager().getTransferedKVPairs("ALL", "");
            }
            String message = "leave " + this.address + " " + this.port + x;
            this.sendDataToECS(message, map, owners);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MessageObject receiveMessageObject() throws IOException {
        MessageObject mobj;
        try {
            mobj = (MessageObject) this.ecsServerIs.readObject();
            logger.info("Receiving a message object containing the message: ("+ mobj.getMessage() +") from a server/ECS");
            return mobj;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            logger.severe("Error receiving the message object");
            return null;
        }

    }

    private void sendToECS(String message) throws IOException {
        logger.info("Sending a message:("+ message +") to the ECS on " + this.ecsAddress.getHostName() + ":"+this.ecsAddress.getPort() );
        message = message+"\r\n";
        MessageObject mobj = new MessageObject(message, null, null);
        this.socket = new Socket(this.ecsAddress.getHostName(), this.ecsAddress.getPort());
        this.ecsServerOs = new ObjectOutputStream(this.socket.getOutputStream());
        this.ecsServerOs.flush();
        this.ecsServerOs.writeObject(mobj);
        this.ecsServerOs.flush();
    }

    private void sendToServer(String message, String ip, int port) throws IOException {
        logger.info("Sending a message:("+ message +") to the server on " + ip + ":" + port);
        message = message+"\r\n";
        MessageObject mobj = new MessageObject(message, null, null);
        this.socket = new Socket(ip, port);
        this.ecsServerOs = new ObjectOutputStream(this.socket.getOutputStream());
        this.ecsServerOs.flush();
        this.ecsServerOs.writeObject(mobj);
        this.ecsServerOs.flush();
    }

    private void sendDataToECS(String message, HashMap<String,String> data, HashMap<String,String> owners) throws IOException {
        logger.info("Sending the data to the ECS before exiting");
        this.socket = new Socket(this.ecsAddress.getHostName(), this.ecsAddress.getPort());
        this.ecsServerOs = new ObjectOutputStream(this.socket.getOutputStream());
        this.ecsServerOs.flush();
        this.ecsServerIs =new ObjectInputStream(this.socket.getInputStream());
        this.ecsServerOs.writeObject(new MessageObject(message, null, data, owners));
        this.ecsServerOs.flush();
    }
    
    // https://www.baeldung.com/java-free-port
    private int getFreePort() throws IOException{
        ServerSocket ss = new ServerSocket(0);
        try {
            return ss.getLocalPort();
        } finally{
            ss.close();
        }
    }
    public void triggerUsersBroadcast() throws IOException{
        logger.info("Triggering users broadcast to every server via ECS");
        
        this.socket = new Socket(this.ecsAddress.getHostName(), this.ecsAddress.getPort());
        this.ecsServerOs = new ObjectOutputStream(this.socket.getOutputStream());
        this.ecsServerOs.flush();
        this.ecsServerIs =new ObjectInputStream(this.socket.getInputStream());
        this.ecsServerOs.writeObject(new MessageObject("users_list " + this.address + " " + this.port, this.kvs.getUsersManager().getAllUsers()));
        this.ecsServerOs.flush();
    }
    public Metadata getMetadata(){
        logger.info("Getting Metadata from the ECSThread");
        return this.metadata;
    }

}