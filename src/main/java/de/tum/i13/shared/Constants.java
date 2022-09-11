package de.tum.i13.shared;

public class Constants {
	public static final String TELNET_ENCODING = "ISO-8859-1"; // encoding for telnet
	
    public static final String [] FILENAMES = {"a-h.txt","i-q.txt","r-z.txt","num.txt"};

	public static final String [] OWNER_FILENAMES = {"u_a-h.txt","u_i-q.txt","u_r-z.txt","u_num.txt"};

	public static final String SERVER_NOTRESPONSIBLE = "server_not_responsible";
	public static final String SERVER_STOPPED = "server_stopped";
	public static final String SERVER_WRITELOCK = "server_write_lock";
	public static final String SERVER_AVAILABLE = "server_available";

	public static final String KEYRANGE_SUCCESS = "keyrange_success ";
	public static final String KEYRANGE_READ_SUCCESS = "keyrange_read_success ";
	
	public static final String GET_ERROR = "get_error";
	public static final String GET_SUCCESS = "get_success";

	public static final String PUT_SUCCESS = "put_success";
	public static final String PUT_UPDATE = "put_update";
	public static final String PUT_ERROR = "put_error";

	public static final String DELETE_SUCCESS = "delete_success";
	public static final String DELETE_ERROR = "delete_error";
	public static final String NOT_FOUND = "not_found";
	public static final String FOUND_DELETED = "found_deleted";

	public static final String REGISTER_ERROR = "register_error Username is already taken!";
	public static final String REGISTER_SUCCESS = "register_success User added successfully!";

	public static final String LOGIN_ERROR = "login_error";
	public static final String LOGIN_SUCCESS = "login_success";

	public static final String NO_TRANSFERRED_DATA = "no_transferred_data";
	public static final String FIFO = "FIFO";
	public static final String LRU = "LRU";
	public static final String LFU = "LFU";

	public static String VALID_IP_REGEX = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
	public static String VALID_HOSTNAME_REGEX = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
	public static String VALID_PORT_REGEX = "\\d+";
	public static String VALID_LEVEL_REGEX = "[A-Z]+";

	public static String HELP_MESSAGE = "The client provides the following commands:\n" +
			"connect <address> <port>:   Tries to establish a TCP- connection to the echo server based on the given server address and the port number of the echo service. \n" +
			"disconnect:                 Tries to disconnect from the connected server. \n" +
			"put <key> <value>:          Inserts a key-value pair into the storage server data structures or updates the current value with the given value if the server already contains the specified key. \n" +
			"put <key> <value>:          Deletes the entry for the given key if <value> equals null. \n" +
			"get <key>:                  Retrieves the value for the given key from the storage server. \n" +
			"register <username>:        Registers the username in the server users list. \n" +
			"login <username>:           Logs the user in to be able to query the servers. \n" +
			"logLevel <level>:           Sets the logger to the specified log level. \n" +
			"help:                       Shows the intended usage of the client application and describes its set of commands. \n" +
			"quit:                       Tears down the active connection to the server and exits the program execution.";
}
