package de.tum.i13.ecs;

import de.tum.i13.server.ECSConnection.MessageObject;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.MyTimer;
import de.tum.i13.shared.ServerEntry;
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
import java.util.logging.Logger;
import java.time.Duration;
import java.time.Instant;

/**
 * This class implements the ECS server which serves as a bootstrap for the servers.
 */
public class ECSServer extends Thread {
    public static Logger logger = Logger.getLogger(ECSServer.class.getName());
    private Metadata metadata;
    private String address;
    private int port;
    private ServerSocket serverSocket; 
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private HashMap<String, PublicKey> latestUsersList;
    public ECSServer(String address, int port) {
        this.metadata = new Metadata();
        this.address = address;
        this.port = port;
        this.ois = null;
        this.oos = null;
        this.latestUsersList = new HashMap<>();

        MyTimer t = new MyTimer();
        GossipTask gossipTask = new GossipTask(this.metadata);
        t.scheduleAtFixedRate(gossipTask, 0, 5000);
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("closing the ECS server");
                shutdown();
            }
       });
        startServerSocket();
        processCommands();
    }

    /**
     * Starts the serversocket resposible for accepting connections.
     */
    public void startServerSocket(){
        try {
            this.serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(this.address, this.port));
            logger.info("Starting the ECS Server on address: "+ this.address +" and port: "+ this.port);
        } catch (IOException e) {
            logger.severe("Error starting the ECS");
        }
    }

    /**
     * Listens to commands from servers and process them.
     */
    public void processCommands() {
        while(true){
            try{
                logger.info("Processing commands from the Servers...");
                Socket s = this.serverSocket.accept();
                this.oos = new ObjectOutputStream((s.getOutputStream()));
                this.oos.flush();
                this.ois =new ObjectInputStream( (s.getInputStream()));
                MessageObject mobj = (MessageObject) this.receiveMessageObject();
                String in = mobj.getMessage().trim();
                String[] commandArr = in.split(" ");
                logger.info("New command received from Servers: " + in);
                String cmd = commandArr[0];
                logger.info("Processing command: " + cmd);
                switch(cmd){
                    case "join":
                        handleJoinCommand(commandArr); break;
                    case "leave":
                        handleLeaveCommand(mobj); break;
                    case "get_metadata":
                        handleGetMetadataCommand(commandArr); break;
                    case "data_transfer_complete":
                        handleDataTransferComplete(commandArr); break;
                    case "still_alive":
                        handleStillAlive(commandArr); break;
                    case "gossip_success":
                        handleGossipSuccess(); break;
                    case "gossip_fail":
                        handleGossipFailure(commandArr); break;
                    case "users_list":
                        handleUsersList(mobj); break;
                    default:
                        logger.warning("Error! unknown command: " + cmd);
                }
    
            } catch (IOException e){
                logger.info("Error processing commands from the Servers");
                e.printStackTrace();
            }
        }

    }

    private void handleGossipSuccess() {
        logger.info("Gossip success!");
    }

    private void handleGossipFailure(String[] commandArr) {
        String ip = commandArr[1];
        int serverPort = Integer.parseInt(commandArr[2]);

        logger.info("Gossip failure: " + ip + ":" + serverPort);
        this.metadata.deleteServer(ip, serverPort);
    }

    /**
     * Handles the joining of a new server.
     *
     * @param commandArr array containing the join data including the server ip, clientPort and serverPort.
     */
    private void handleJoinCommand(String[] commandArr) {
        logger.info("Processing join command.");
        if(commandArr.length == 4) {
            String ip = commandArr[1];
            int clientPort = Integer.parseInt(commandArr[2]);
            int serverPort = Integer.parseInt(commandArr[3]);
            ServerEntry predecessor = this.metadata.addNewServer(ip, clientPort, serverPort).getPredecessor();

            if (predecessor == null) {
                sendMetadataUpdates();

                logger.info("join_success");
                sendMessageToTheServer(new MessageObject("join_success"+"\r\n", null, null), ip, serverPort);
                return;
            }
            sendMetadataUpdates();
            blockServer(predecessor, ip, clientPort, serverPort);

            String reply = "join_success " + predecessor.getIp() + " " + predecessor.getClientPort() + " " + predecessor.getServerPort();
            logger.info(reply);
            if(this.latestUsersList.size()>0){
                reply += " alongside with the userslist";
                sendMessageToTheServer(new MessageObject(reply+"\r\n", this.latestUsersList), ip, serverPort);
            }
            else {
                sendMessageToTheServer(new MessageObject(reply+"\r\n", null, null), ip, serverPort);
            }
            return;
        } else {
            logger.severe("join_error invalid number of arguments.");
            return;
        }
    }

    /**
     * Handles the leave command from a server.
     *
     * @param mobj message object containing the leave message and the map of key-value pairs from the exiting server.
     */
    private void handleLeaveCommand(MessageObject mobj) {
        String[] commandArr = mobj.getMessage().trim().split("\\s+");
        logger.info("Processing leave command.");
        if (commandArr.length == 6) {
            String ip = commandArr[1];
            int clientPort = Integer.parseInt(commandArr[2]);
            String predIp =  commandArr[3];
            int predClientPort = Integer.parseInt(commandArr[4]);
            int predServerPort = Integer.parseInt(commandArr[5]);
            HashMap <String,String> data = mobj.getTransferedData();
            HashMap <String,String> owners = mobj.getKeyOwners();

            ServerEntry serverToBeDeleted = this.metadata.getData().get(hashToInt(Metadata.generateHash(ip + ":" + clientPort)));

            if (serverToBeDeleted == null) {
                logger.severe("leave_error no server with this ip and port available.");
                return;
            }

            this.metadata.deleteServer(ip, clientPort);
            //serverToBeDeleted.getTimer().cancel();
            sendMetadataUpdates();
            logger.info("Server: " + ip + ":" + clientPort +" left successfully.");

            logger.info("transfer data to predecessor " + predIp + ":" + predClientPort + " with command " + "transfer_data_pred_leaving");
            sendDataToTheServer(new MessageObject("transfer_data_pred_leaving", null, data, owners),  predIp, predServerPort);
            return;
        } else if (commandArr.length == 3){
            String ip = commandArr[1];
            int clientPort = Integer.parseInt(commandArr[2]);
            ServerEntry serverToBeDeleted = this.metadata.getData().get(hashToInt(Metadata.generateHash(ip + ":" + clientPort)));
            if (serverToBeDeleted == null) {
                logger.severe("leave_error no server with this ip and port available.");
                return;
            }
            this.metadata.deleteServer(ip, clientPort);
            sendMetadataUpdates();
            logger.info("Server: " + ip + ":" + clientPort +" left successfully.");

        } else {
            logger.severe("leave_error invalid number of arguments.");
            return;
        }
    }

    private void handleUsersList(MessageObject mobj){
        HashMap<String, PublicKey> users = mobj.getUsers();
        this.latestUsersList = users;
        String [] commandArr = mobj.getMessage().split(" ");
        for (Map.Entry<BigInteger, ServerEntry> server : this.metadata.getData().entrySet()) {
            if( !(commandArr[1].equals(server.getValue().getIp()) && 
            Integer.parseInt(commandArr[2]) == server.getValue().getClientPort()) ){    
                try {
                    ECSSocketManager socketManager = new ECSSocketManager();
                    socketManager.connect(server.getValue().getIp(), server.getValue().getServerPort());
                    logger.info("Send a message object with users list and message: users_update" + " to the server: " + server.getValue().getIp() + ":" +server.getValue().getClientPort());
                    socketManager.sendMessageObject( new MessageObject("users_update", users));
                    socketManager.disconnect();
                } catch (IOException e) {
                    logger.severe("Error sending the users list to the server: " + server.getValue().getIp() + ":" + server.getValue().getClientPort());
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * Handles the response by the server to the heartbeat request.
     *
     * @param commandArr an array containing the response by the server including its ip and clientPort.
     */
    private void handleStillAlive(String[] commandArr) {
        String ip = commandArr[1];
        String clientPort = commandArr[2];
        Instant start = this.metadata.getServerEntry(ip + ":" + clientPort).getStartTime();
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        logger.info("Received still_alive from " + ip + ":" + clientPort + ", time taken: " + timeElapsed.toMillis() + "ms");
    }

    /**
     * Handles the get metadata update command.
     *
     * @param commandArr an array containing the request.
     */
    private void handleGetMetadataCommand(String[] commandArr) {
        logger.info("Processing get metadata command.");
        if (commandArr.length == 1) {
            if (this.metadata.getData().size() == 0) {
                logger.severe("metadata_update_error no servers available.");
            }
            logger.info("metadata_update " + this.metadata.toString());
        } else {
            logger.severe("metadata_update_error invalid number of arguments.");
        }
    }

    /**
     * Handles the successful data transfer and sends the diasble_lock message
     *
     * @param commandArr array containing the success message and the ip and port of the locked server.
     */
    private void handleDataTransferComplete(String[] commandArr) {
        logger.info("Processing data transfer complete command.");
        if (commandArr.length == 4) {
            String ip = commandArr[1];
            int clientPort = Integer.parseInt(commandArr[2]);
            int serverPort = Integer.parseInt(commandArr[3]);
            ServerEntry blockedServer = this.metadata.getData().get(hashToInt(Metadata.generateHash(ip + ":" + clientPort)));

            if (blockedServer == null) {
                logger.severe("disable_lock error: no server with this ip and port available.");
                return; 
            }

            if (blockedServer.isWriteLockEnabled()) {
                blockedServer.setWriteLockEnabled(false);
                logger.info("Sending disable_lock to the server");
                sendMessageToTheServer(new MessageObject("disable_write_lock", null, null), ip, serverPort);
                return; 
            } else {
                logger.severe("disable_lock error: server was not locked.");
                return;
            }
        } else {
            logger.severe("disable_lock error: invalid number of arguments.");
            return;
        }
    }

    /**
     * Blocks a server for data transfer by sending the enable_write_lock message.
     *
     * @param server server to be blocked.
     * @param ip Ip of the server which will get data from the locked server.
     * @param clientPort the client communications port ofthe server which will get data from the locked server.
     * @param serverPort the server/ecs communications port of the server which will get data from the locked server.
     */
    private void blockServer(ServerEntry server, String ip, int clientPort, int serverPort) {
        logger.info("Block server " + server.getStartIndex());
        this.sendMessageToTheServer(new MessageObject("enable_write_lock " + ip + " " + clientPort + " " + serverPort, null, null), server.getIp(), server.getServerPort());
        server.setWriteLockEnabled(true);
    }

    /**
     * Sends the updated metadata to every server.
     */
    private void sendMetadataUpdates() {
        for (Map.Entry<BigInteger, ServerEntry> server : this.metadata.getData().entrySet()) {
            try {
                ECSSocketManager socketManager = new ECSSocketManager();
                socketManager.connect(server.getValue().getIp(), server.getValue().getServerPort());
                logger.info("Send a message object with metadata and message: metadata_update" + " to the server: " + server.getValue().getIp() + ":" +server.getValue().getClientPort());

                socketManager.sendMessageObject( new MessageObject("metadata_update", metadata, null));
                socketManager.disconnect();
            } catch (IOException e) {
                logger.severe("Error sending the metadata to the server: " + server.getValue().getIp() + ":" + server.getValue().getClientPort());
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a message to a server.
     *
     * @param mobj the message object containing the message to be send.
     * @param ip the ip of the receiving server.
     * @param serverPort the port of the receiving server.
     */
    private void sendMessageToTheServer(MessageObject mobj, String ip, int serverPort) {
        try {
            ECSSocketManager socketManager = new ECSSocketManager();
            try {
                socketManager.connect(ip, serverPort);
            } catch (IOException e) {
                e.printStackTrace();
                logger.severe("Connection to server failed.");
            }
            socketManager.sendMessageObject(mobj);
            logger.info("Send a message: " + mobj.getMessage() + " to the server: " + ip + ":" + serverPort);
            socketManager.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe("Error sending the message to the server");
        }
    }

    /**
     * Sends data to a server.
     *
     * @param mobj the message object containing the data to be send.
     * @param ip the ip of the receiving server.
     * @param serverPort the port of the receiving server.
     */
    private void sendDataToTheServer(MessageObject mobj, String ip, int serverPort){
        try {
            ECSSocketManager socketManager = new ECSSocketManager();
            socketManager.connect(ip, serverPort);
            socketManager.sendMessageObject(mobj);
            logger.info("Send a message object with data and message: " + mobj.getMessage() + " to the server: " + ip + ":" + serverPort);
            socketManager.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe("Error sending the message to the server");
        }
    }

    /**
     * Receives a message from a server.
     *
     * @return the received message object
     * @throws IOException if the InputStream has already been closed.
     */
    private MessageObject receiveMessageObject() throws IOException {
        MessageObject mobj;
        try {
            mobj = (MessageObject) this.ois.readObject();
            logger.info("Receiving a message object containing the message: ("+ mobj.getMessage() +") from a server/ECS");
            return mobj;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            logger.severe("Error receiving the message object");
            return null;
        }
    }

    /**
     * Converts a hex hash value to an integer.
     *
     * @param hash the hash value
     * @return the integer as a BigInteger object.
     */
    private BigInteger hashToInt(String hash) {
        return new BigInteger(hash, 16);
    }

    /**
     * Responsible for closing down the server in a safe manner.
     */
    public void shutdown(){
        logger.info("shutdown triggered, now closing everything");
            try {
                if(this.serverSocket!=null)
                    this.serverSocket.close();
                if(this.ois!=null)
                    this.ois.close();
                if(this.oos!=null)
                    this.oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
   
    }

    /**
     * Provides the metadata managed by the ECS.
     *
     * @return the metadata.
     */
    public Metadata getMetadata() {
        return this.metadata;
    }
}
