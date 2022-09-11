package de.tum.i13;

import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestKVIntegration {
    private static final String ADDRESS = "127.0.0.1";
    private static final String TESTING_DIRECTORY = "src/test/testdata/";
    private static final String LOG_DIRECTORY = "testlogs/test.log";
    private static final String LOG_LEVEL = "OFF";
    private static final Integer CACHE_SIZE = 150;
    private static final String CACHE_STRATEGY = "LRU";

    // ----------------------------------------- JUnit utility methods -------------------------------------------------
    //@BeforeAll
    public static void buildTestEnvironment() {
        deleteFiles(TESTING_DIRECTORY);
        //deleteFiles(LOG_DIRECTORY);
    }

    //@AfterAll
    public static void tearDownEnvironment() {
        deleteFiles(TESTING_DIRECTORY);
        //deleteFiles(LOG_DIRECTORY);
    }

    //@BeforeEach
    public void restartTestEnvironment() {
        deleteFiles(TESTING_DIRECTORY);
        //deleteFiles(LOG_DIRECTORY);
    }
    /*
    // ----------------------------------------- TESTS -----------------------------------------------------------------
    //@Test
    public void testPut() throws InterruptedException, IOException {
        Integer port = 4000;
        Thread th = new Thread() {
            @Override
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-p", port.toString(),
                            "-a", ADDRESS,
                            "-d", TESTING_DIRECTORY,
                            "-l", LOG_DIRECTORY,
                            "-ll", LOG_LEVEL,
                            "-c", CACHE_SIZE.toString(),
                            "-s", CACHE_STRATEGY});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start(); // started the server
        Thread.sleep(2000);

        Socket s = new Socket();
        s.connect(new InetSocketAddress(ADDRESS, port));
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
        input.readLine();

        assertEquals("put_success key", doRequest(s, "put key val"));
        th.stop();
    }

    //@Test
    public void testResponseOrder() throws InterruptedException, IOException {
        Integer port = 4001;
        Thread th = new Thread() {
            @Override
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-p", port.toString(),
                            "-a", ADDRESS,
                            "-d", TESTING_DIRECTORY,
                            "-l", LOG_DIRECTORY,
                            "-ll", LOG_LEVEL,
                            "-c", CACHE_SIZE.toString(),
                            "-s", CACHE_STRATEGY});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start(); // started the server
        Thread.sleep(2000);

        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", port));
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
        input.readLine();

        assertEquals("put_success k", doRequest(s, "put k foo"));
        assertEquals("put_update k", doRequest(s, "put k bar"));
        th.stop();
    }

    //@Test
    public void testMultipleCommands() throws InterruptedException, IOException {
        Integer port = 4002;
        Thread th = new Thread() {
            @Override
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-p", port.toString(),
                            "-a", ADDRESS,
                            "-d", TESTING_DIRECTORY,
                            "-l", LOG_DIRECTORY,
                            "-ll", LOG_LEVEL,
                            "-c", CACHE_SIZE.toString(),
                            "-s", CACHE_STRATEGY});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start(); // started the server
        Thread.sleep(2000);

        Socket s = new Socket();
        s.connect(new InetSocketAddress(ADDRESS, port));
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
        input.readLine();

        assertEquals("delete_error key key is not found!", doRequest(s, "delete key"));
        assertEquals("get_error key key is not found!", doRequest(s, "get key"));
        assertEquals("put_success key", doRequest(s, "put key val1"));
        assertEquals("get_success key val1", doRequest(s, "get key"));
        assertEquals("put_update key", doRequest(s, "put key val2"));
        assertEquals("get_success key val2", doRequest(s, "get key"));
        assertEquals("delete_success key", doRequest(s, "delete key"));
        assertEquals("get_error key key is not found!", doRequest(s, "get key"));
        assertEquals("delete_error key key is not found!", doRequest(s, "delete key"));
        th.stop();
    }

    //@Test
    public void testMultipleClients() throws InterruptedException, IOException {
        Integer port = 4003;
        Thread th = new Thread() {
            @Override
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-p", port.toString(),
                            "-a", ADDRESS,
                            "-d", TESTING_DIRECTORY,
                            "-l", LOG_DIRECTORY,
                            "-ll", LOG_LEVEL,
                            "-c", CACHE_SIZE.toString(),
                            "-s", CACHE_STRATEGY});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start(); // started the server
        Thread.sleep(2000);

        Socket client1 = new Socket();
        client1.connect(new InetSocketAddress("127.0.0.1", port));
        BufferedReader input_client1 = new BufferedReader(new InputStreamReader(client1.getInputStream()));
        input_client1.readLine();

        Socket client2 = new Socket();
        client2.connect(new InetSocketAddress("127.0.0.1", port));
        BufferedReader input_client2 = new BufferedReader(new InputStreamReader(client2.getInputStream()));
        input_client2.readLine();

        assertEquals("put_success key", doRequest(client1, "put key val1"));
        assertEquals("get_success key val1", doRequest(client2, "get key"));
        th.stop();
    }

    //@Test
    public void testPutError() throws InterruptedException, IOException {
        Integer port = 4004;
        Thread th = new Thread() {
            @Override
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-p", port.toString(),
                            "-a", ADDRESS,
                            "-d", TESTING_DIRECTORY,
                            "-l", LOG_DIRECTORY,
                            "-ll", LOG_LEVEL,
                            "-c", CACHE_SIZE.toString(),
                            "-s", CACHE_STRATEGY});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start(); // started the server
        Thread.sleep(2000);

        Socket s = new Socket();
        s.connect(new InetSocketAddress(ADDRESS, port));
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
        input.readLine();

        // test if keys greater than 20 Bytes are rejected
        byte[] keyArray = new byte[21];
        Arrays.fill(keyArray, (byte) 'a'); // to generate random string: new Random().nextBytes(array);
        String keyString = new String(keyArray, Charset.forName(Constants.TELNET_ENCODING));
        assertEquals("put_error key or value are too long!", doRequest(s,"put " + keyString + " val"));

        // test if values greater than 120 KBytes are rejected
        byte[] valueArray = new byte[122881];
        Arrays.fill(valueArray, (byte) 'a');
        String valueString = new String(valueArray, Charset.forName(Constants.TELNET_ENCODING));
        assertEquals("put_error key or value are too long!", doRequest(s,"put key " + valueString));
        th.stop();
    } */


    // ----------------------------------------- UTILITY METHODS -------------------------------------------------------
    private static void deleteFiles(String directory) {
        File directoryFile = new File(directory);
        for (File file: Objects.requireNonNull(directoryFile.listFiles())) {
            if (!file.isDirectory() && !file.getName().equals(".gitignore")) {
                file.delete();
            }
        }
    }

    private String doRequest(Socket s, String req) throws IOException {
        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        output.write(req + "\r\n");
        output.flush();

        String res = input.readLine();
        return res;
    }


    /*
    private String doRequest(String req) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(ADDRESS, PORT));
        String res = doRequest(s, req);
        s.close();

        return res;
    }
     */

}
