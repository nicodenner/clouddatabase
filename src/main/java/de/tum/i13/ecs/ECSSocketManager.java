package de.tum.i13.ecs;

import de.tum.i13.server.ECSConnection.MessageObject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

/**
 * Deals with the connection between ECS and the servers.
 */
public class ECSSocketManager {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean isConnected = false;
    private final static Logger LOGGER = Logger.getLogger(ECSSocketManager.class.getName());

    public ECSSocketManager() {
    }

    /**
     * Connect to a provided host via a socket.
     *
     * @param host the hostname.
     * @param port the port of the host.
     * @return an error message if an error occurred.
     */
    public String connect(String host, int port) throws IOException {
        // check if we are already connected to a host
        if (this.isConnected) {
            String errorMsg = "Connection failed, since client is already connected to a server.";
            LOGGER.severe(errorMsg);
            return errorMsg;
        }

        // open the socket and respective streams for receiving data
        this.socket = new Socket(host, port);
        this.in = new ObjectInputStream(socket.getInputStream());
        this.out = new ObjectOutputStream(socket.getOutputStream());

        this.isConnected = true;
        return "";
    }


    /**
     * Disconnect from the host.
     *
     * @return the disconnection message OR alternatively an error message if a semantic error occurred.
     */
    public String disconnect() throws IOException {
        // can not disconnect if no connection was established
        if (!this.isConnected) {
            String errorMsg = "Disconnection failed, since ecs is not connected to any server.";
            LOGGER.severe(errorMsg);
            return errorMsg;
        }

        // get host and port for disconnection message
        int port = this.socket.getPort();
        // String host = this.socket.getInetAddress().getHostAddress();
        String host = this.socket.getRemoteSocketAddress().toString();

        // close streams and socket
        this.in.close();

        this.out.close();

        this.socket.close();

        this.isConnected = false;
        String response = "Connection terminated: " +  host + " / " + port;
        LOGGER.info(response);
        return response;
    }

    /**
     * Send MessageObject via the OutputStream.
     *
     * @param mobj the messageObject.
     * @throws IOException gets thrown if there is an error with the OutputStream.
     */
    public void sendMessageObject(MessageObject mobj) throws IOException {
        out.writeObject(mobj);
        out.flush();
    }

    /**
     * Receive MessageObject via InputStream.
     *
     * @return the received message objejct.
     */
    public MessageObject receiveMessageObject() {
        MessageObject mobj;
        try {
            mobj = (MessageObject) this.in.readObject();
            return mobj;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Indicates if a connection to a host has been established.
     *
     * @return true if a socket connection exists, false if not.
     */
    public boolean isConnected() {
        return isConnected;
    }
}