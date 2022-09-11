package de.tum.i13.shared;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

/**
 * Deals with configuring the logger.
 */
public class LogSetup {
    public static void setupLogging(Path logfile) {
        Logger logger = LogManager.getLogManager().getLogger("");
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] %5$s %6$s%n");

        if(!Files.exists(logfile.getParent())){
            try {
                Files.createDirectory(logfile.getParent());
            } catch (IOException e) {
                System.out.println("Could not create logger directory");
                e.printStackTrace();
                System.exit(-1);
            }
        }

        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler(logfile.toString(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        for (Handler h : logger.getHandlers()) {
            h.setLevel(Level.ALL);
        }
        logger.setLevel(Level.ALL); //we want log everything
    }

    /**
     * Sets up a logger which will log all messages to a log file.
     *
     * By default the log level is set to ALL.
     *
     * @param logfile the name of the log file which will be created and written to when logs are issued.
     */
    public static void setupLogging(String logfile) {
        Logger logger = LogManager.getLogManager().getLogger("");
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] %5$s %6$s%n");

        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler(logfile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Handler h : logger.getHandlers()) {
            h.setLevel(Level.OFF);
        }
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        logger.setLevel(Level.ALL); //we want log everything
    }
}