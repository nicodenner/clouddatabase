package de.tum.i13.ecs;

import de.tum.i13.shared.LogSetup;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class of the ECS that reads the starting parameters and starts the server.
 */
public class StartECSServer {
    public static Logger logger = Logger.getLogger(StartECSServer.class.getName());

    public static void main(String[] args) throws IOException {
        ECSConfig cfg = ECSConfig.parseCommandlineArgs(args);
        LogSetup.setupLogging(cfg.logfile);
        logger.info("Config: " + cfg.toString());
        logger.info("Starting ECS server...");

        Level logLevel;
        try {
            logLevel = Level.parse(cfg.loglevel);
            logger.setLevel(logLevel);
            logger.info("LOGLEVEL set to: "+ logLevel.getName());
        } catch (IllegalArgumentException e) {
            String errorMsg = "Invalid log level provided. Choose from: ALL, CONFIG, FINE, FINEST, INFO, OFF, SEVERE, WARNING";
            logger.severe(errorMsg);
            throw new IllegalArgumentException();
        }

        ECSServer ecsServer = new ECSServer(cfg.address, cfg.port);
        ecsServer.start();
    }
}
