package de.tum.i13.server.nio;

import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.PersistenceKVStore;
import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Config;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.Config.parseCommandlineArgs;
import static de.tum.i13.shared.LogSetup.setupLogging;

public class StartSimpleNioServer {

    public static Logger logger = Logger.getLogger(StartSimpleNioServer.class.getName());

    public static void main(String[] args) throws IOException {
        Config cfg = parseCommandlineArgs(args);  //Do not change this
        setupLogging(cfg.logfile);
        logger.info("Config: " + cfg.toString());

        logger.info("starting server");
        if(cfg.cacheSize <= 0) 
        	cfg.cacheSize = 10;
        
        Level logLevel;
        try {
            logLevel = Level.parse(cfg.loglevel);
            logger.info("LOGLEVEL set to: "+ logLevel.getName());
            logger.setLevel(logLevel);

        } catch (IllegalArgumentException e) {
            String errorMsg = "Invalid log level provided. Choose from: ALL, CONFIG, FINE, FINEST, INFO, OFF, SEVERE, WARNING";
            logger.severe(errorMsg);
            throw new IllegalArgumentException();
        }
        //Replace with your Key Value command processor
        PersistenceKVStore pkvstore = new PersistenceKVStore(cfg);
        CommandProcessor echoLogic = new KVCommandProcessor(pkvstore);

        SimpleNioServer sn = new SimpleNioServer(echoLogic);
        sn.bindSockets(cfg.listenaddr, cfg.port);
        System.out.println("Server started on address:" + cfg.listenaddr+ " and port:" + cfg.port);
        sn.start();
    }
}
