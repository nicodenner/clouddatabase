package de.tum.i13.ecs;

import de.tum.i13.server.ECSConnection.MessageObject;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.NeighborsAndSelf;
import de.tum.i13.shared.ServerEntry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Instant;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * TimerTask that executes the heartbeat for each server to check if they are still responding.
 */
class PingTask extends TimerTask {
    public static Logger logger = Logger.getLogger(PingTask.class.getName());

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private Metadata metadata;
    private String ip;
    private int clientPort;
    private int serverPort;
    private ECSSocketManager socketManager;

    public PingTask(String ip, int clientPort, int serverPort, Metadata metadata) {
        this.metadata = metadata;
        this.ip = ip;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
        this.socketManager = new ECSSocketManager();
    }

    @Override
    public void run() {
        logger.info("Check heartbeat on " + this.ip + ":" + this.serverPort);
        String heartbeatMessage = "heartbeat " + this.ip + " " + this.clientPort;

        try {
            this.socketManager.connect(ip, serverPort);
        } catch (IOException e) {
            logger.info("Server has been shut down, that's why PingTask will not work anymore.");
            this.cancel(); // stop ping task

            if (this.metadata.getData().size() == 0) {
                // no other server left so no one needs to get data
                return;
            }

            ServerEntry successor = this.metadata.deleteServer(this.ip, this.clientPort).getSuccessor();
            try {
                this.socketManager.connect(successor.getIp(), successor.getServerPort());
                this.socketManager.sendMessageObject(new MessageObject("transfer_data_pred_leaving", null, new HashMap<>()));
                this.socketManager.disconnect();
            } catch (IOException ex) {
                this.cancel();
                logger.info("Unable to connect to successor of server which does not respond.");
            }

            return;
        }

        try {
            this.socketManager.sendMessageObject(new MessageObject(heartbeatMessage, null, null));
            this.socketManager.disconnect();
            this.metadata.getServerEntry(this.ip + ":" + this.clientPort).updateStartTime(Instant.now());
        } catch (IOException e) {
            this.cancel();
            logger.info("Unable to send heartbeat message.");
        }
    }
}