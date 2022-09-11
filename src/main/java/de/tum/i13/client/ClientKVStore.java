package de.tum.i13.client;

import de.tum.i13.security.RSA;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.ServerEntry;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Provides the client application logic.
 */
public class ClientKVStore {
    private SocketManager socketManager;
    private final static Logger LOGGER = Logger.getLogger(ClientKVStore.class.getName());
    private BackoffService backoffService;
    private Metadata metadata;

    private String username;

    /**
     * Client class constructor.
     */
    public ClientKVStore() {
        this.socketManager = new SocketManager();
        this.backoffService = new BackoffService();
        this.metadata = new Metadata();
    }

    /**
     * Connects client to a host.
     *
     * @param host the host.
     * @param port the port of the host.
     * @return the connection message received by the host OR alternatively an error message if a semantic error occurred.
     */
    public String connect(String host, int port) {
        String socketResponse = this.socketManager.connect(host, port);
        return socketResponse;
    }

    /**
     * Disconnects client from the host.
     *
     * @return the disconnection message OR alternatively an error message if a semantic error occurred.
     */
    public String disconnect() {
        String socketResponse = this.socketManager.disconnect();
        return socketResponse;
    }

    /**
     * Sends a message and receive the echo response from the host.
     *
     * @param message the message to be sent.
     * @return the echo message OR alternatively an error message if a semantic error occurred.
     */
    /*
    public String send(String message) {
        return this.socketManager.send(message);
    }
     */

    /**
     * Inserts a key-value pair into the KVServer.
     *
     * @param key   the key that identifies the given value.
     * @param value the value that is indexed by the given key.
     * @return a message that confirms the insertion of the tuple, update of a the tuple or an error.
     */
    public String put(String key, String value) {
        String putRequest = "put " + this.username + " " + key + " " + value;
        connectToCorrectServer(key);
        String putResponse = requestUntilNotStopped(putRequest);
        String putResponseCommand = putResponse.split(" ")[0];

        switch (putResponseCommand) {
            case "server_stopped":
                LOGGER.fine("PUT | server_stopped: " + putResponse);
                return putResponse;
            case "server_not_responsible":
                LOGGER.info("PUT | server_not_responsible: " + putResponse);
                fetchLatestMetadata();
                connectToCorrectServer(key);
                return requestUntilNotStopped(putRequest);
            case "server_write_lock":
                LOGGER.fine("PUT | server_write_lock: " + putResponse);
                return "This server is currently blocked for write requests, please try again later.";
            case "put_success":
                LOGGER.fine("PUT | put_success: " + putResponse);
                return "SUCCESS";
            case "put_update":
                LOGGER.fine("PUT | put_update: " + putResponse);
                return "SUCCESS";
            case "put_error":
                LOGGER.fine("PUT | put_error: " + putResponse);
                return "ERROR";
            default:
                LOGGER.severe("PUT | invalid put response received: " + putResponse);
                return "Error: invalid put response.";
        }
    }

    /**
     * Retrieves the value for a given key from the KVServer.
     *
     * @param key the key that identifies the value.
     * @return the value, which is indexed by the given key.
     * @throws Exception if put command cannot be executed (e.g. not connected to any
     *                   KV server).
     */
    public String get(String key) {
        String getRequest = "get " + key;
        if(this.metadata.numberOfServers()>=3)
            fetchLatestMetadataReplicas_AND_CHOSESERVER(key);

        String getResponse = requestUntilNotStopped(getRequest);
        String getResponseCommand = getResponse.split(" ")[0];

        switch (getResponseCommand) {
            case "server_stopped":
                LOGGER.fine("GET | server_stopped: " + getResponse);
                return getResponse;
            case "server_not_responsible":
                LOGGER.info("GET | server_not_responsible: " + getResponse);
                fetchLatestMetadata();
//                fetchLatestMetadataReplicas();
                connectToCorrectServer(key);
                return requestUntilNotStopped(getRequest);
            case "get_success":
                LOGGER.fine("GET | get_success: " + getResponse);
                return getResponse;
            case "get_error":
                LOGGER.fine("GET | get_error: " + getResponse);
                return getResponse;
            default:
                LOGGER.severe("GET | invalid get response received: " + getResponse);
                return "Error: invalid get response.";
        }
    }

    /**
     * Submits the username to the kvserver.
     *
     * @param username the username of the client.
     * @param publicKey the public key of the client.
     * @return the response from the server, if the username is duplicate or added successfully.
     * @throws Exception if put command cannot be executed (e.g. not connected to any
     *                   KV server).
     */
    public String register(String username, PublicKey publicKey) {
        String request = "register " + username + " "+ Base64.getEncoder().encodeToString(publicKey.getEncoded()) ;
        String response = this.socketManager.send(request);
        String responseCommand = response.split(" ")[0];

        switch (responseCommand) {
            case "server_stopped":
                return response;
            case "register_success":
                return response;
            case "register_error":
                return response;

            default:
                return "Error: invalid register response.";
        }
    }

    /**
     * Submits the username to the kvserver.
     *
     * @param username the username of the client.
     * @param publicKey the public key of the client.
     * @return the response from the server, if the username is duplicate or added successfully.
     * @throws Exception if put command cannot be executed (e.g. not connected to any
     *                   KV server).
     */
    public String login(String username) {
        this.username = username;
        String request = "login " + username;
        String response = this.socketManager.send(request);
        String responseCommand = response.split(" ")[0];

        switch (responseCommand) {
            case "server_stopped":
                return response;
            case "login_error":
                return response;
            case "decrypt_this":
                PrivateKey pv = this.readKeyFile().getPrivateKey();
                try {
                    String decryptedMsg = RSA.decrypt(response.split(" ")[1], pv);
                    String result = this.socketManager.send("check " + decryptedMsg);
                    return result;
                } catch (Exception e) {
                    e.printStackTrace();
                    return "login_error key decryption failed!";
                }
            default:
                return "Error: invalid login response.";
        }
    }
    /**
     * Deletes the value for a given key from the KVServer.
     *
     * @param key the key that identifies the value.
     * @return the value, which is indexed by the given key.
     * @throws Exception if put command cannot be executed (e.g. not connected to any
     *                   KV server).
     */
    public String delete(String key) {
        String deleteRequest = "delete " + this.username + " " + key;
        connectToCorrectServer(key);
        String deleteResponse = requestUntilNotStopped(deleteRequest);
        String deleteResponseCommand = deleteResponse.split(" ")[0];

        switch (deleteResponseCommand) {
            case "server_stopped":
                LOGGER.fine("DEL | server_stopped: " + deleteResponse);
                return deleteResponse;
            case "server_not_responsible":
                LOGGER.info("DEL | server_not_responsible: " + deleteResponse);
                fetchLatestMetadata();
                connectToCorrectServer(key);
                return requestUntilNotStopped(deleteRequest);
            case "server_write_lock":
                LOGGER.fine("DEL | server_write_lock: " + deleteResponse);
                return "This server is currently blocked for write requests, please try again later.";
            case "delete_success":
                LOGGER.fine("DEL | delete_success: " + deleteResponse);
                return "SUCCESS";
            case "delete_error":
                LOGGER.fine("DEL | delete_error: " + deleteResponse);
                return "ERROR";
            default:
                LOGGER.severe("DEL | invalid delete response received: " + deleteResponse);
                return "Error: invalid delete response.";
        }
    }

    /**
     * Quits the client application.
     *
     * If the connection was not already terminated it will be done so before quitting the application.
     * @return the quit message.
     */
    public String quit() {
        if (this.socketManager.isConnected()) {
            this.socketManager.disconnect();
            LOGGER.info("Disconnected from server.");
        }

        LOGGER.fine("Application exit!");
        return "Application exit!";
    }

    /**
     * Provides the help message with all commands provided by the application.
     *
     * @return the help message.
     */
    public String help() {
        return Constants.HELP_MESSAGE;
    }

    /**
     * Sets the logLevel from default ALL to the specified level.
     *
     * @param logLevelStr the required log level.
     * @return the log level message OR alternatively an error message if a semantic error occurred.
     */
    public String setLogLevel(String logLevelStr) {
        Level newLogLevel;
        try {
            newLogLevel = Level.parse(logLevelStr);
        } catch (IllegalArgumentException e) {
            String errorMsg = "Invalid log level provided. Choose from: ALL, CONFIG, FINE, FINEST, INFO, OFF, SEVERE, WARNING";
            LOGGER.severe("setLogLevel | " + errorMsg);
            return errorMsg;
        }

        Level currentLogLevel = LOGGER.getLevel();
        if (currentLogLevel == null) {
            currentLogLevel = Level.OFF;
        }

        LOGGER.setLevel(newLogLevel);
        String response = "loglevel set from " + currentLogLevel.toString() + " to " + newLogLevel.toString() + ".";
        LOGGER.fine("setLogLevel | " + response);
        return response;
    }

    /* ---------------------------------------- PRIVATE METHODS ------------------------------------------------------*/

    /**
     * Sends a provided request multiple times if server answered with server_stopped.
     *
     * Repeated requests stop according to a backoff strategy.
     *
     * @param request the request to be executed.
     * @return the response by the server or an error message after multiple unsuccessful tries.
     */
    private String requestUntilNotStopped(String request) {
        String response = this.socketManager.send(request);
        String responseCommand = response.split(" ")[0];

        if (responseCommand.equals("server_stopped")) {
            LOGGER.severe("requestUntilNotStopped | We received server stopped.");
            while (backoffService.shouldRetry()) {
                LOGGER.severe("requestUntilNotStopped | retrying...");
                backoffService.errorOccured();
                response = this.socketManager.send(request);
                responseCommand = response.split(" ")[0];

                if (!responseCommand.equals("server_stopped")) {
                    LOGGER.severe("requestUntilNotStopped | No further retry needed.");
                    backoffService.doNotRetry();
                }
            }

            if (responseCommand.equals("server_stopped")) {
                LOGGER.severe("requestUntilNotStopped | Maximum number of retries reached, we stop trying.");
                backoffService.reset();
                return response + " Maximum retries reached and server is not responding, stop trying...";
            } else {
                LOGGER.severe("requestUntilNotStopped | we got a response after retrying: " + request);
                return response;
            }
        } else {
            LOGGER.fine("requestUntilNotStopped | Request went through without problems.");
            return response;
        }
    }

    /**
     * Retrieves the latest metadata.
     *
     * @return indicator if the fetch was executed correctly or not.
     */
    private String fetchLatestMetadata() {
        String keyrangeResponse = requestUntilNotStopped("keyrange");
        String keyRangeResponseCommand = keyrangeResponse.split(" ")[0];

        if (keyRangeResponseCommand.equals("server_stopped")) {
            LOGGER.severe("searchForNewServerAndExecuteRequest | server has been stopped: " + keyrangeResponse);
            return "Server stopped";
        } else if (keyRangeResponseCommand.equals("keyrange_success")) {
            String metadata = keyrangeResponse.replaceFirst("keyrange_success ", "").trim();
            LOGGER.fine("searchForNewServerAndExecuteRequest | received metadata: " + metadata);
            this.metadata = new Metadata(true, metadata);
            return "Metadata received successfully.";
        } else {
            LOGGER.severe("searchForNewServerAndExecuteRequest | Error with executing keyrange command: " + keyrangeResponse);
            return "Error with executing keyrange command.";
        }
    }

    /**
     * Retrieves the latest metadata with corresponding replicas (keyrange_read) .
     *
     * @return indicator if the fetch was executed correctly or not.
     */
    private String fetchLatestMetadataReplicas_AND_CHOSESERVER(String key) {
        String correctHash = "";
        ArrayList<String> potentialServers= new ArrayList<String>();
        String keyrangeReadResponse = requestUntilNotStopped("keyrange_read");
        String keyRangeReadResponseCommand = keyrangeReadResponse.split(" ")[0];

        if (keyRangeReadResponseCommand.equals("server_stopped")) {
            LOGGER.severe("searchForNewServerAndExecuteRequest | server has been stopped: " + keyrangeReadResponse);
            return "Server stopped";
        }
        else if (keyRangeReadResponseCommand.equals("keyrange_read_success")) {
            String metadata = keyrangeReadResponse.replaceFirst("keyrange_read_success ", "").trim();
//            modfiy_Metadata(true,metadata);

            this.metadata = new Metadata(true, metadata);
            LOGGER.fine("searchForNewServerAndExecuteRequest | received metadata of " + this.metadata.numberOfServers()+" servers/replicas: " + metadata);
            for (Map.Entry<BigInteger, ServerEntry> server : this.metadata.getData().entrySet()) {
                if (Metadata.hashInRange(key, server.getValue().getStartIndex(), server.getValue().getEndIndex())) {
                    correctHash = server.getValue().getStartIndex()+ ","+server.getValue().getEndIndex();
                }
            }

            String[] metadata_lines = metadata.split(";");
            for (String metadata_server_line : metadata_lines){
                String[] metadata_server_line_splitted  = metadata_server_line.split(",");
                String startEndHash= metadata_server_line_splitted[0]+","+metadata_server_line_splitted[1];
                if (startEndHash.equals(correctHash)) {
                    potentialServers.add(metadata_server_line_splitted[2]);
                }
            }
            Random rand = new Random();
            String chosenServerOrReplicas = potentialServers.get(rand.nextInt(3)); // randomize whatever server from the 3
            String chosenServerOrReplicas_IP = chosenServerOrReplicas.split(":")[0];
            int chosenServerOrReplicas_PORT= Integer.parseInt(chosenServerOrReplicas.split(":")[1]);

            this.disconnect();
            this.connect(chosenServerOrReplicas_IP, chosenServerOrReplicas_PORT);
            LOGGER.info("sendRequestToCorrectServer | switch to a random potential server/replica: " + chosenServerOrReplicas_IP + ":" + chosenServerOrReplicas_PORT);


            return "Metadata along with replicas received successfully.";
        }

        else {
            LOGGER.severe("searchForNewServerAndExecuteRequest | Error with executing keyrange_read command: " + keyrangeReadResponse);
            return "Error with executing keyrange_read command.";}
    }


    /**
     * Search for a given key through the metadata to find the right server to send the request to.
     *
     * @param key the key in the request, needed to find the hash position
     */
    private void connectToCorrectServer(String key) {
        if (this.metadata.getData().size() == 0) {
            // we initially connected to a random server and therefore never fetched metadata
            LOGGER.info("sendRequestToCorrectServer | First server we connected to, we just try to send something.");
            return;
        }

        for (Map.Entry<BigInteger, ServerEntry> server : this.metadata.getData().entrySet()) {
            if (Metadata.hashInRange(key, server.getValue().getStartIndex(), server.getValue().getEndIndex())) {
                if (this.socketManager.getCurrentServerIp() == server.getValue().getIp() && this.socketManager.getCurrServerPort() == server.getValue().getClientPort()) {
                    // we are already connected to the right one
                    LOGGER.info("sendRequestToCorrectServer | stayed at our server " + server.getValue().getIp() + ":" + server.getValue().getClientPort());
                    return;
                } else {
                    // we need to connect to a different server
                    this.disconnect();
                    this.connect(server.getValue().getIp(), server.getValue().getClientPort());
                    LOGGER.info("sendRequestToCorrectServer | switch to server: " + server.getValue().getIp() + ":" + server.getValue().getClientPort());
                    return;
                }
            }
        }
        LOGGER.severe("sendRequestToCorrectServer | Error, no responsible server found!");
    }
    /**
     * Search for a given key through the metadata to find the right server/replica to send the get/read request to.
     *
     * @param key the key in the request, needed to find the hash position
     */
    private void connectToCorrectServerOrReplica(String key) {
        if (this.metadata.getData().size() == 0) {
            // we initially connected to a random server and therefore never fetched metadata
            LOGGER.info("sendRequestToCorrectServer | First server we connected to, we just try to send something.");
            return;
        }

        // add random factor (chose one of 3 possible servers: coordinator and 2 replicas)
        Random rndm = new Random();
        int rndmNumber = rndm.nextInt(this.metadata.getData().entrySet().size());
        Map.Entry<BigInteger, ServerEntry> server = (Map.Entry<BigInteger, ServerEntry>) this.metadata.getData().entrySet().toArray()[rndmNumber];


        if (this.socketManager.getCurrentServerIp() == server.getValue().getIp() && this.socketManager.getCurrServerPort() == server.getValue().getClientPort()) {
            // we are already connected to the chosen one
            LOGGER.info("sendRequestToCorrectServer | stayed at our server " + server.getValue().getIp() + ":" + server.getValue().getClientPort());
            return;
        } else {
            // we need to redirect connection to the chosen one
            this.disconnect();
            this.connect(server.getValue().getIp(), server.getValue().getClientPort());
            LOGGER.info("sendRequestToCorrectServer | switch to server: " + server.getValue().getIp() + ":" + server.getValue().getClientPort());
            return;
        }
//        LOGGER.severe("sendRequestToCorrectServer | Error, no responsible server found!");
    }

    public PublicKey checkAndGenerateKeysPair(){
        Path p = Paths.get("key/mykey.txt");
        if(!Files.exists(p)){
               try {
                Files.createDirectory(Paths.get("key/"));
                File file = new File(p.toString());
                file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
			}
            RSA rsa = new RSA();
            writeKeyFile(rsa);
            return rsa.getPublicKey();
        } else {
            return readKeyFile().getPublicKey();
        }
    }
    private void writeKeyFile(RSA rsa){
        try {
            FileOutputStream fos = new FileOutputStream("key/mykey.txt");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(rsa);
            oos.close();
            fos.close();
        } catch (FileNotFoundException e){
            System.out.println("Private key file not found!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private RSA readKeyFile(){
        RSA rsa = null;
        try {
            FileInputStream fis = new FileInputStream("key/mykey.txt");
            ObjectInputStream ois = new ObjectInputStream(fis);
            rsa = (RSA) ois.readObject();
            ois.close();
            fis.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return rsa;
    }
}
