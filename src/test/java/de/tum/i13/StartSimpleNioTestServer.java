// package de.tum.i13;

// import de.tum.i13.server.nio.SimpleNioServer;
// import de.tum.i13.shared.CommandProcessor;
// import de.tum.i13.shared.Config;

// import java.io.IOException;
// import java.util.logging.Level;

// import static de.tum.i13.shared.Config.parseCommandlineArgs;
// import static de.tum.i13.shared.LogSetup.setupLogging;

// public class StartSimpleNioTestServer {

//     public static void main(String[] args) throws IOException {
//         Config cfg = parseCommandlineArgs(args);  //Do not change this
//         setupLogging(cfg.logfile);
//         // System.out.println("Config: " + cfg.toString());

//         if(cfg.cacheSize <= 0)
//             cfg.cacheSize = 10;

//         Level logLevel;
//         try {
//             logLevel = Level.parse(cfg.loglevel);

//         } catch (IllegalArgumentException e) {
//             String errorMsg = "Invalid log level provided. Choose from: ALL, CONFIG, FINE, FINEST, INFO, OFF, SEVERE, WARNING";
//             throw new IllegalArgumentException();
//         }
//         //Replace with your Key Value command processor
//         //PersistenceKVStore pkvstore = new PersistenceKVStore(cfg.cacheStrategy, cfg.cacheSize, cfg.dataDir);
//         CommandProcessor echoLogic = new CommandProcessorTest();

//         SimpleNioServer sn = new SimpleNioServer(echoLogic);
//         sn.bindSockets(cfg.listenaddr, cfg.port);
//         // System.out.println("Server started on address:" + cfg.listenaddr+ " and port:" + cfg.port);
//         sn.start();
//     }
// }
