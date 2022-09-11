package de.tum.i13.client;

import de.tum.i13.shared.Constants;
import de.tum.i13.shared.LogSetup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.logging.Logger;

/**
 * Entrypoint into the client application. This class handles the user input and checks it for syntactical correctness
 * before forwarding it to the respective client function.
 */
public class ClientShell {
    private final static Logger LOGGER = Logger.getLogger(ClientShell.class.getName());

    /**
     * Main function which serves as a shell for interacting with the client.
     *
     * @param args optional program arguments.
     */
    public static void main(String[] args) {
        LogSetup.setupLogging(Paths.get("logs/kv-client.log"));
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        ClientKVStore client = new ClientKVStore();

        boolean isQuit = false;
        boolean isLoggedIn = false;
        PublicKey pk = client.checkAndGenerateKeysPair();
        while (!isQuit) {
            // read user input
            System.out.print("StorageClient> ");
            String user_input = "";
            try {
                user_input = input.readLine();
                if (user_input == null) {
                    return;
                }
            } catch (IOException e) {
                LOGGER.severe("StorageClient> " + "Error with reading the user input:" + e.toString());
            }

            // get user command
            String[] user_input_arr = user_input.trim().split("\\s+");
            String user_command = "";
            if (user_input_arr.length > 0) {
                user_command = user_input_arr[0];
            } else {
                printEchoLine(client.help());
                continue;
            }

            // branch with respect to user command
            switch (user_command) {
                case "connect":
                    // ----------------------------------------- connect -----------------------------------------------
                    if (user_input_arr.length != 3) {
                        printEchoLine("Invalid input for command \"connect\": invalid amount of input arguments provided.");
                    } else if (!user_input_arr[1].matches(Constants.VALID_IP_REGEX) && !user_input_arr[1].matches(Constants.VALID_HOSTNAME_REGEX)) {
                        printEchoLine("Invalid input for command \"connect\": invalid host provided.");
                    } else if (!user_input_arr[2].matches(Constants.VALID_PORT_REGEX)) {
                        printEchoLine("Invalid input for command \"connect\": invalid port provided.");
                    } else {
                        String host = user_input_arr[1];
                        int port = -1;
                        try {
                            port = Integer.parseInt(user_input_arr[2]);
                            printEchoLine(client.connect(host, port));
                        } catch (NumberFormatException e) {
                            printEchoLine("Invalid input for command \"connect\": invalid port provided.");
                        }
                    }
                    break;
                case "disconnect":
                    // --------------------------------------- disconnect ----------------------------------------------
                    if (user_input_arr.length != 1) {
                        printEchoLine("Invalid input for command \"disconnect\": invalid amount of input arguments provided, see \"help\" command.");
                    } else {
                        printEchoLine(client.disconnect());
                        isLoggedIn = false;
                    }
                    break;
                case "logLevel":
                    // ---------------------------------------- logLevel -----------------------------------------------
                    if (user_input_arr.length != 2) {
                        printEchoLine("Invalid input for command \"logLevel\": invalid amount of input arguments provided, see \"help\" command.");
                    } else if (!user_input_arr[1].matches(Constants.VALID_LEVEL_REGEX)) {
                        printEchoLine("Invalid input for command \"logLevel\": invalid log level syntax.");
                    } else {
                        String logLevel = user_input_arr[1];
                        printEchoLine(client.setLogLevel(logLevel));
                    }
                    break;
                case "help":
                    // ------------------------------------------ help -------------------------------------------------
                    printEchoLine(client.help());
                    break;
                case "quit":
                    // ------------------------------------------ quit -------------------------------------------------
                    printEchoLine(client.quit());
                    isQuit = true;
                    break;
                case "register":
                if (user_input_arr.length != 2) {
                    printEchoLine("Invalid input for command \"register\": invalid amount of input arguments provided, see \"help\" command.");
                } else {
                    String username = user_input_arr[1];
                    String response = client.register(username, pk);
                    printEchoLine(response);
                } break;
                case "login":
                if (user_input_arr.length != 2) {
                    printEchoLine("Invalid input for command \"login\": invalid amount of input arguments provided, see \"help\" command.");
                } else {
                    String username = user_input_arr[1];
                    String response = client.login(username);
                    printEchoLine(response);
                    if(response.equals(Constants.LOGIN_SUCCESS)){
                        isLoggedIn = true;
                    }
                } break;
                case "put":
                    if(!isLoggedIn){
                        printEchoLine("You are not logged in, please register/login!");
                        break;
                    }
                    // ------------------------------------------ put -------------------------------------------------
                    if (user_input_arr.length < 3) {
                        printEchoLine("Invalid input for command \"put\": invalid amount of input arguments provided, see \"help\" command.");
                    } else {
                        String key = user_input_arr[1];
                        String value = "";

                        if (user_input_arr.length > 3) {
                            for (int i = 2; i < user_input_arr.length; i++) {
                                value += user_input_arr[i] + " ";
                            }
                            value = value.trim();
                        } else {
                            value = user_input_arr[2];
                        }

                        if (!key.equals("null") && !value.equals("null")) {
                            // insert or delete
                            printEchoLine(client.put(key, value));
                        } else if (value.equals("null")) {
                            // delete
                            printEchoLine(client.delete(key));
                        } else {
                            printEchoLine("Invalid input for command \"put\": invalid arguments.");
                        }
                    }
                    break;
                case "get":
                     if(!isLoggedIn){
                        printEchoLine("You are not logged in, please register/login!");
                        break;
                    }
                    // ------------------------------------------ get -------------------------------------------------
                    if (user_input_arr.length != 2) {
                        printEchoLine("Invalid input for command \"get\": invalid amount of input arguments provided, see \"help\" command.");
                    } else {
                        String key = user_input_arr[1];
                        if (key.getBytes().length > 20) {
                            printEchoLine("Invalid input for command \"key\": keys are only allowed to have a size up to 20 bytes.");
                        } else {
                            String getResponse = client.get(key);

                            if (getResponse.startsWith("get_success")) {
                                printEchoLine(getResponse.split(" ",3)[2]);
                            } else {
                                printEchoLine(getResponse);
                            }

                        }
                    }
                    break;
                default:
                    // ------------------------------------------ help -------------------------------------------------
                    printEchoLine(client.help());
                    break;
            }
        }
    }

    private static void printEchoLine(String msg) {
        System.out.println("StorageClient> " + msg);
    }
}
