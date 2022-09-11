package de.tum.i13.ecs;

import de.tum.i13.server.ECSConnection.MessageObject;
import de.tum.i13.shared.Metadata;
import de.tum.i13.shared.NeighborsAndSelf;
import de.tum.i13.shared.ServerEntry;
import java.io.IOException;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * A TimerTask which is concerned with initiating new gossip rounds for failure detection.
 */
public class GossipTask extends TimerTask {
    public static Logger logger = Logger.getLogger(PingTask.class.getName());

    private Metadata metadata;
    private ECSSocketManager socketManager;

    public GossipTask(Metadata metadata) {
        this.metadata = metadata;
        this.socketManager = new ECSSocketManager();
    }

    /**
     * Will execute a periodic gossip check to detect any failed servers in the circle.
     */
    @Override
    public void run() {
        if (this.metadata.getData().size() == 0) {
            // nothing to check here
            return;
        }

        ServerEntry server = this.metadata.getData().firstEntry().getValue();
        logger.info("Check gossip on " + server.getIp() + ":" + server.getServerPort());

        try {
            this.socketManager.connect(server.getIp(), server.getServerPort());
        } catch (IOException e) {
            logger.info("Server has been shut down, that's why GossipTask will not work anymore.");

            if (this.metadata.getData().size() == 0) {
                // no other server left so no one needs to get data
                return;
            }

            NeighborsAndSelf neighbors = this.metadata.deleteServer(server.getIp(), server.getServerPort());

            if (neighbors == null) {
                return;
            }

            ServerEntry successor = neighbors.getSuccessor();

            try {
                this.socketManager.connect(successor.getIp(), successor.getServerPort());
                this.socketManager.sendMessageObject(new MessageObject("transfer_data_pred_leaving", null, new HashMap<>()));
                this.socketManager.disconnect();
            } catch (IOException ex) {
                logger.info("Unable to connect to successor of server which does not respond.");
            }

            return;
        }

        try {
            this.socketManager.sendMessageObject(new MessageObject("start_gossip", null, null));
            this.socketManager.disconnect();
        } catch (IOException e) {
            logger.info("Unable to send gossip start message.");
        }
    }
}

