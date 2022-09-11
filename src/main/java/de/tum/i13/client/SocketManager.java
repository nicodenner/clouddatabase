package de.tum.i13.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Manages the connection to a host for sending and receiving messages via a socket.
 */
public class SocketManager {
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private boolean isConnected = false;
    private final static Logger LOGGER = Logger.getLogger(SocketManager.class.getName());
    private String currServerIp;
    private int currServerPort;

    /**
     * Connect to a provided host via a socket.
     *
     * @param host the hostname.
     * @param port the port of the host.
     * @return the connection message received by the host OR alternatively an error message if a semantic error occurred.
     */
    public String connect(String host, int port) {
        this.currServerIp = host;
        this.currServerPort = port;

        // check if we are already connected to a host
        if (this.isConnected) {
            String errorMsg = "Connection failed, since client is already connected to a server.";
            LOGGER.severe("connect | " + errorMsg);
            return errorMsg;
        }

        // open the socket and respective streams for receiving data
        try {
            this.socket = new Socket(host, port);

            try {
                this.in = socket.getInputStream();
            } catch(IOException e) {
                String errorMsg = "Invalid input for command \"connect\": error when establishing input stream.";
                LOGGER.severe("connect | " + errorMsg);
                return errorMsg;
            }

            try {
                this.out = socket.getOutputStream();
            } catch(IOException e) {
                String errorMsg = "Invalid input for command \"connect\": error when establishing output stream.";
                LOGGER.severe("connect | " + errorMsg);
                return errorMsg;
            }

        } catch(UnknownHostException e) {
            String errorMsg = "Invalid input for command \"connect\": unknown host.";
            LOGGER.severe("connect | " + errorMsg);
            return errorMsg;
        } catch(IOException e) {
            String errorMsg = "Invalid input for command \"connect\": invalid host or port.";
            LOGGER.severe("connect | " + errorMsg);
            return errorMsg;
        }

        // receive connection message by the host
        String connection_msg;
        try {
            connection_msg = new String(this.receive());
            connection_msg = connection_msg.trim();
            LOGGER.info("connect | " + connection_msg);
        } catch (IOException e) {
            String errorMsg = "Error while retrieving the socket connection message.";
            LOGGER.severe("connect | " + errorMsg);
            return errorMsg;
        }

        this.isConnected = true;
        return connection_msg;
    }

    /**
     * Disconnect from the host.
     *
     * @return the disconnection message OR alternatively an error message if a semantic error occurred.
     */
    public String disconnect() {
        // can not disconnect if no connection was established
        if (!this.isConnected) {
            String errorMsg = "Disconnection failed, since client is not connected to any server.";
            LOGGER.severe("disconnect | " + errorMsg);
            return errorMsg;
        }

        // get host and port for disconnection message
        int port = this.socket.getPort();
        // String host = this.socket.getInetAddress().getHostAddress();
        String host = this.socket.getRemoteSocketAddress().toString();

        // close streams and socket
        try {
            this.in.close();
        } catch (IOException e) {
            String errorMsg = "Invalid input for command \"disconnect\": error when closing input stream.";
            LOGGER.severe("disconnect | " + errorMsg);
            return errorMsg;
        }

        try {
            this.out.close();
        } catch (IOException e) {
            String errorMsg = "Invalid input for command \"disconnect\": error when closing output stream.";
            LOGGER.severe("disconnect | " + errorMsg);
            return errorMsg;
        }

        try {
            this.socket.close();
        } catch (IOException e) {
            String errorMsg = "Invalid input for command \"disconnect\": error when closing client socket.";
            LOGGER.severe("disconnect | " + errorMsg);
            return errorMsg;
        }

        this.isConnected = false;
        String response = "Connection terminated: " +  host + " / " + port;
        LOGGER.info("disconnect | " + response);
        return response;
    }

    /**
     * Sends a message to the host if a connection was established.
     *
     * @param message the message to be sent
     * @return the response by the server OR alternatively an error message if a semantic error occurred.
     */
    public String send(String message) {
        // can not send a message if no connection was established
        if (!this.isConnected) {
            String errorMsg = "Error! Not connected!";
            LOGGER.severe("send | " + errorMsg);
            return errorMsg;
        }

        // append message with carriage-return
        String message_with_cr = message + "\r\n";
        byte[] sendByte = message_with_cr.getBytes(StandardCharsets.ISO_8859_1);

        if (sendByte.length > 122880) {
            String errorMsg = "Invalid input for command \"send\": maximum message size of 128 KByte exceeded.";
            LOGGER.severe("send | " + errorMsg);
            return errorMsg;
        }

        // send message
        try {
            this.send(sendByte);
            LOGGER.info("send | Message send to server: " + message);
        } catch (IOException e) {
            String errorMsg = "Invalid input for command \"send\": message sending failed.";
            LOGGER.severe("send | " + errorMsg);
            return errorMsg;
        }

        // receive echo message
        String response;
        try {
            byte[] recvByte = this.receive();
            response = new String(recvByte, StandardCharsets.ISO_8859_1).trim();
            LOGGER.info("send | Message received from server: " + response);
        } catch (IOException e) {
            String errorMsg = "Invalid input for command \"send\": message receiving failed.";
            LOGGER.severe("send | " + errorMsg);
            return errorMsg;
        }

        return response;
    }

    /**
     * Send message via the OutputStream.
     *
     * @param message the message as a byte array.
     * @throws IOException gets thrown if there is an error with the OutputStream.
     */
    public void send(byte[] message) throws IOException {
        out.write(message);
        out.flush();
    }

    /**
     * Receive message via the InputStream.
     *
     * @return the received message as a byte array (excluding any empty bytes if the message is smaller than 128KBytes).
     * @throws IOException gets thrown if there is an error with the InputStream.
     */
    public byte[] receive() throws IOException {
        byte[] response = new byte[500000]; // TODO: maybe choose a better number
        int received_bytes = this.in.read(response);
        return Arrays.copyOfRange(response, 0, received_bytes);
    }

    /**
     * Indicates if a connection to a host has been established.
     *
     * @return true if a socket connection exists, false if not.
     */
    public boolean isConnected() {
        return isConnected;
    }

    public String getCurrentServerIp() {return this.currServerIp;}

    public int getCurrServerPort() {return this.currServerPort;}
}
