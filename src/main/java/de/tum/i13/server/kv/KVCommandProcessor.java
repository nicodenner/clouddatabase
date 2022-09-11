package de.tum.i13.server.kv;

import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Constants;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class KVCommandProcessor implements CommandProcessor {
    private PersistenceKVStore kvStore;
    public static Logger logger = Logger.getLogger(KVCommandProcessor.class.getName());

    public KVCommandProcessor(PersistenceKVStore kvStore) {
        this.kvStore = kvStore;
    }
    /**
     * processes the upcoming command from client.
     *
     * @param command the command alongside with arguments.
     * @return the message returned from the server (success or error).
     */
    @Override
    public String process(String command) {
        String[] commandArr = command.trim().split("\\s+");

        String cmd = commandArr[0];
        switch(cmd){
            case "get":
                return handleGetCommand(commandArr);
            case "put":
                return handlePutCommand(commandArr);
            case "delete":
                return handleDeleteCommand(commandArr);
            case "keyrange":
                return handleKeyRange(commandArr);
            case "keyrange_read":
                return handleKeyRangeRead(commandArr);
            case "register":
                return handleRegisterCommand(commandArr);
            case "login":
                return handleLoginCommand(commandArr);
            case "check":
                return handleCheckCommand(commandArr);

            default:
                logger.warning("Error: unknown command: " + cmd);
                return "error unknown command!";
        }

    }

    private String handleKeyRangeRead(String[] commandArr) {
        logger.info("Executing keyrange_read command...");
        if (this.kvStore.getMetadata().getData().size() <= 2) {
            String keyr = kvStore.getKeyRange();
            if(keyr !=null){
                logger.fine(Constants.KEYRANGE_READ_SUCCESS + keyr);
                return Constants.KEYRANGE_READ_SUCCESS + keyr;
            } else {
                logger.warning(keyr);
                return keyr;
            }
        }

        String keyr = kvStore.getKeyRangeRead();
        if(keyr !=null){
            logger.fine(Constants.KEYRANGE_READ_SUCCESS + keyr);
            return Constants.KEYRANGE_READ_SUCCESS + keyr;
        } else {
            logger.warning(keyr);
            return keyr;
        }
    }

    private String handleKeyRange(String[] commandArr) {
        logger.info("Executing keyrange command...");
        String keyr = kvStore.getKeyRange();
        if(keyr !=null){
            logger.fine(Constants.KEYRANGE_SUCCESS + keyr);
            return Constants.KEYRANGE_SUCCESS + keyr;
        } else {
            logger.warning(keyr);
            return keyr;
        }
    }
    /**
     * handles put command from client.
     *
     * @param commandArr the command alongside with key and value(s).
     * @return the response returned from the server (success or error).
     */
    private String handlePutCommand(String[] commandArr) {
        logger.info("Executing put command...");
        if(commandArr.length >= 4) {
            String res;
            try {
                String value = commandArr.length==4?commandArr[3]:constructValue(commandArr);
                String user = commandArr[1];
                if(commandArr[2].getBytes().length>20 || value.getBytes().length>122880){
                    String errorMsg = Constants.PUT_ERROR + " key or value are too long!";
                    logger.warning(errorMsg);
                    return errorMsg;
                }
                res = kvStore.put(commandArr[2], value, user);
                if(res.equals("insert")) {
                    logger.fine(Constants.PUT_SUCCESS+" " + commandArr[2]);
                    boolean replica_1InUse = this.kvStore.getReplica_1().isInUse();
                    boolean replica_2InUse = this.kvStore.getReplica_2().isInUse();
                    this.kvStore.getEcsthread().makeDataConsistentAgain(replica_1InUse, replica_2InUse);
                    return Constants.PUT_SUCCESS+" " + commandArr[2];
                } else if(res.equals("update")){
                    logger.fine(Constants.PUT_UPDATE+" " + commandArr[2]);
                    boolean replica_1InUse = this.kvStore.getReplica_1().isInUse();
                    boolean replica_2InUse = this.kvStore.getReplica_2().isInUse();
                    this.kvStore.getEcsthread().makeDataConsistentAgain(replica_1InUse, replica_2InUse);
                    return Constants.PUT_UPDATE +" "+ commandArr[2];
                } else {
                    logger.warning(res);
                    return res;
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.severe(Constants.PUT_ERROR+": exception thrown!");
                return Constants.PUT_ERROR+" " + commandArr[2] + " exception thrown!";
            }

        } else {
            logger.warning(Constants.PUT_ERROR+": invalid number of arguments!");
        	return Constants.PUT_ERROR+" " + commandArr[2] + " invalid number of arguments!";
        }
    }

    /**
     * handles multiples words values in put command.
     *
     * @param commandArr the command alongside with key and value(s).
     * @return the concatenated value.
     */
    private String constructValue(String[] commandArr) {
        String result = commandArr[3];
        for(int i=4; i<commandArr.length; i++)
            result= result + " " + commandArr[i];
        return result;
    }

    /**
     * handles delete command from client.
     *
     * @param commandArr the command alongside with key.
     * @return the response returned from the server (success or error).
     */
    private String handleDeleteCommand(String[] commandArr) {
        logger.info("Executing delete command...");

        if(commandArr.length == 3) {
            String value;
            try {
                value = kvStore.delete(commandArr[2], commandArr[1]);
                if(value.equals(Constants.FOUND_DELETED)) {
                    logger.fine(Constants.DELETE_SUCCESS+" " + commandArr[2]);
                    boolean replica_1InUse = this.kvStore.getReplica_1().isInUse();
                    boolean replica_2InUse = this.kvStore.getReplica_2().isInUse();
                    this.kvStore.getEcsthread().makeDataConsistentAgain(replica_1InUse, replica_2InUse);
                    return Constants.DELETE_SUCCESS+" " + commandArr[2];
                } else if (value.equals(Constants.NOT_FOUND)) {
                    logger.warning(Constants.DELETE_ERROR+" " + commandArr[2]);
                    return Constants.DELETE_ERROR +" "+ commandArr[2] + " key is not found!";
                } else {
                    logger.warning(value);
                    return value;
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.severe(Constants.DELETE_ERROR+": exception thrown!");
                return Constants.DELETE_ERROR+" " + commandArr[2] + " exception thrown!";
            }

        } else {
            logger.warning(Constants.DELETE_ERROR+": invalid number of arguments!");
        	return Constants.DELETE_ERROR+" " + commandArr[2] + " invalid number of arguments!";
        }
    }

     /**
     * handles get command from client.
     *
     * @param commandArr the command alongside with key.
     * @return the response returned from the server (success or error).
     */  
    private String handleGetCommand(String[] commandArr) {
        logger.info("Executing get command...");
        if(commandArr.length == 2) { 
            String value;
            try {
                value = kvStore.get(commandArr[1]);
                if(value == null) {
                    return Constants.GET_ERROR +" "+ commandArr[1] + " key is not found!";
                } else if(value.equals(Constants.SERVER_NOTRESPONSIBLE) || value.equals(Constants.SERVER_STOPPED)) {
                    return value;
                } else {
                    return Constants.GET_SUCCESS+" " + commandArr[1] + " " + value;
                }
            } catch (Exception e) {
                logger.severe(Constants.GET_ERROR+": exception thrown!");
                e.printStackTrace();
                return Constants.GET_ERROR+" " + commandArr[1] + " exception thrown!"; 
            }

        } else {
            logger.warning(Constants.GET_ERROR+": invalid number of arguments!");
        	return Constants.GET_ERROR+" " + commandArr[1] + " invalid number of arguments!"; 
        }
    }

    /**
     * handles register command from client.
     *
     * @param commandArr the command alongside with key.
     * @return the response returned from the server (success or error).
     */  
    private String handleRegisterCommand(String[] commandArr) {
        logger.info("Executing register command...");
        if(commandArr.length == 3) {
            return kvStore.register(commandArr[1], commandArr[2]);
        } else {
            logger.warning("register_error: invalid number of arguments!");
        	return "register_error: invalid number of arguments!"; 
        }
    }

    /**
     * handles login command from client.
     *
     * @param commandArr the command alongside with key.
     * @return the response returned from the server (success or error).
     */  
    private String handleLoginCommand(String[] commandArr) {
        logger.info("Executing login command...");
        if(commandArr.length == 2) {
            return kvStore.login(commandArr[1]);
        } else {
            logger.warning("login_error: invalid number of arguments!");
        	return "login_error: invalid number of arguments!"; 
        }
    }
    

    private String handleCheckCommand(String[] commandArr) {
        logger.info("Executing check command...");
        return kvStore.check(commandArr[1])? Constants.LOGIN_SUCCESS : 
        Constants.DELETE_ERROR+" authentication failed! Word mismatch~";
  
    }
    @Override
    public String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress) {
        logger.info("Connection to the Storage Server is accepted at address: " + address.toString());
        return "Connection to the Storage Server is accepted at address: " + address.toString() +"\r\n";
    }

    @Override
    public void connectionClosed(InetAddress address) {
        logger.info("connection closed: " + address.toString());
    }
}
