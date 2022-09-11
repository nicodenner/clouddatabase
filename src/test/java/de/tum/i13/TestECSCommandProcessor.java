// package de.tum.i13;

// import de.tum.i13.ecs.ECSCommandProcessor;
// import de.tum.i13.server.kv.KVCommandProcessor;
// import de.tum.i13.server.kv.PersistenceKVStore;
// import de.tum.i13.server.nio.StartSimpleNioServer;
// import de.tum.i13.shared.Constants;
// import org.junit.jupiter.api.AfterAll;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// import java.io.BufferedReader;
// import java.io.IOException;
// import java.io.InputStreamReader;
// import java.io.PrintWriter;
// import java.math.BigInteger;
// import java.net.InetSocketAddress;
// import java.net.Socket;
// import java.nio.charset.Charset;
// import java.nio.file.Paths;
// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;
// import java.util.Arrays;

// import static org.junit.jupiter.api.Assertions.assertEquals;

// public class TestECSCommandProcessor {
//     private ECSCommandProcessor commandProcessor;

//     // ----------------------------------------- JUnit utility methods -------------------------------------------------
//     @BeforeAll
//     public static void buildTestEnvironment() {

//     }

//     @AfterAll
//     public static void tearDownEnvironment() {

//     }

//     @BeforeEach
//     public void restartTestEnvironment() {
//         this.commandProcessor = new ECSCommandProcessor();
//     }

//     // ----------------------------------------------- TESTS -----------------------------------------------------------
//     //@Test
//     public void testJoin() throws InterruptedException, IOException, NoSuchAlgorithmException {
//         Integer PORT_1 = 5001;
//         String ADDRESS_1 = "127.0.0.1";
//         Thread th_1 = new Thread() {
//             @Override
//             public void run() {
//                 try {
//                     StartSimpleNioTestServer.main(new String[]{"-p", PORT_1.toString(), "-a", ADDRESS_1, "-ll", "WARNING"});
//                 } catch (IOException e) {
//                     e.printStackTrace();
//                 }
//             }
//         };
//         th_1.start(); // started the server
//         Thread.sleep(2000);

//         Socket s_1 = new Socket();
//         s_1.connect(new InetSocketAddress(ADDRESS_1, PORT_1));
//         BufferedReader input_1 = new BufferedReader(new InputStreamReader(s_1.getInputStream()));
//         input_1.readLine();

//         String hash = computeHash(ADDRESS_1, PORT_1);
//         assertEquals("join_success", this.commandProcessor.process("join " + ADDRESS_1 + " " + PORT_1 + " " + PORT_1));
//         assertEquals("metadata_update " + hash + "," + hash + "," + ADDRESS_1 + ":" + PORT_1, this.commandProcessor.process("get_metadata"));

//         Integer PORT_2 = 5002;
//         String ADDRESS_2 = "127.0.0.1";
//         Thread th_2 = new Thread() {
//             @Override
//             public void run() {
//                 try {
//                     StartSimpleNioTestServer.main(new String[]{"-p", PORT_2.toString(), "-a", ADDRESS_2, "-ll", "WARNING"});
//                 } catch (IOException e) {
//                     e.printStackTrace();
//                 }
//             }
//         };
//         th_2.start(); // started the server
//         Thread.sleep(2000);

//         Socket s_2 = new Socket();
//         s_2.connect(new InetSocketAddress(ADDRESS_2, PORT_2));
//         BufferedReader input_2 = new BufferedReader(new InputStreamReader(s_2.getInputStream()));
//         input_2.readLine();

//         assertEquals("join_success " + ADDRESS_1 + " " + PORT_1 + " " + PORT_1, this.commandProcessor.process("join " + ADDRESS_2 + " " + PORT_2 + " " + PORT_2));
//         assertEquals("data_transfer_complete_success", this.commandProcessor.process("data_transfer_complete " + ADDRESS_1 + " " + PORT_1 + " " + PORT_1));

//         th_1.stop();
//         th_2.stop();
//     }

//     //@Test
//     public void testGetMetadataUpdate() throws InterruptedException, IOException, NoSuchAlgorithmException {
//         Integer PORT_1 = 5003;
//         String ADDRESS_1 = "127.0.0.1";
//         Thread th_1 = new Thread() {
//             @Override
//             public void run() {
//                 try {
//                     StartSimpleNioTestServer.main(new String[]{"-p", PORT_1.toString(), "-a", ADDRESS_1, "-ll", "WARNING"});
//                 } catch (IOException e) {
//                     e.printStackTrace();
//                 }
//             }
//         };
//         th_1.start(); // started the server
//         Thread.sleep(2000);

//         Socket s_1 = new Socket();
//         s_1.connect(new InetSocketAddress(ADDRESS_1, PORT_1));
//         BufferedReader input_1 = new BufferedReader(new InputStreamReader(s_1.getInputStream()));
//         input_1.readLine();

//         assertEquals("metadata_update_error no servers available.", this.commandProcessor.process("get_metadata"));
//         String HASH_1 = computeHash(ADDRESS_1, PORT_1);
//         this.commandProcessor.process("join " + ADDRESS_1 + " " + PORT_1 + " " + PORT_1);
//         assertEquals("metadata_update " + HASH_1 + "," + HASH_1 + "," + ADDRESS_1 + ":" + PORT_1, this.commandProcessor.process("get_metadata"));

//         Integer PORT_2 = 5004;
//         String ADDRESS_2 = "127.0.0.1";
//         Thread th_2 = new Thread() {
//             @Override
//             public void run() {
//                 try {
//                     StartSimpleNioTestServer.main(new String[]{"-p", PORT_2.toString(), "-a", ADDRESS_2, "-ll", "WARNING"});
//                 } catch (IOException e) {
//                     e.printStackTrace();
//                 }
//             }
//         };
//         th_2.start(); // started the server
//         Thread.sleep(2000);

//         Socket s_2 = new Socket();
//         s_2.connect(new InetSocketAddress(ADDRESS_2, PORT_2));
//         BufferedReader input_2 = new BufferedReader(new InputStreamReader(s_2.getInputStream()));
//         input_2.readLine();

//         String HASH_2 = computeHash(ADDRESS_2, PORT_2);
//         this.commandProcessor.process("join " + ADDRESS_2 + " " + PORT_2 + " " + PORT_2);
//         assertEquals("metadata_update " + HASH_1 + "," + HASH_2 + "," + ADDRESS_1 + ":" + PORT_1 + ";"
//                 + HASH_2 + "," + HASH_1 + "," + ADDRESS_2 + ":" + PORT_2, this.commandProcessor.process("get_metadata"));

//         th_1.stop();
//         th_2.stop();
//     }

//     //@Test
//     public void testDataTransferComplete() throws InterruptedException, IOException, NoSuchAlgorithmException {
//         Integer PORT_1 = 5005;
//         String ADDRESS_1 = "127.0.0.1";
//         assertEquals("data_transfer_complete_error no server with this ip and port available.", this.commandProcessor.process("data_transfer_complete " + ADDRESS_1 + " " + PORT_1 + " " + PORT_1));

//         Thread th_1 = new Thread() {
//             @Override
//             public void run() {
//                 try {
//                     StartSimpleNioTestServer.main(new String[]{"-p", PORT_1.toString(), "-a", ADDRESS_1, "-ll", "WARNING"});
//                 } catch (IOException e) {
//                     e.printStackTrace();
//                 }
//             }
//         };
//         th_1.start(); // started the server
//         Thread.sleep(2000);

//         Socket s_1 = new Socket();
//         s_1.connect(new InetSocketAddress(ADDRESS_1, PORT_1));
//         BufferedReader input_1 = new BufferedReader(new InputStreamReader(s_1.getInputStream()));
//         input_1.readLine();

//         this.commandProcessor.process("join " + ADDRESS_1 + " " + PORT_1 + " " + PORT_1);
//         String HASH_1 = computeHash(ADDRESS_1, PORT_1);
//         assertEquals("metadata_update " + HASH_1 + "," + HASH_1 + "," + ADDRESS_1 + ":" + PORT_1, this.commandProcessor.process("get_metadata"));
//         assertEquals("data_transfer_complete_error server was not locked.", this.commandProcessor.process("data_transfer_complete " + ADDRESS_1 + " " + PORT_1 + " " + PORT_1));

//         th_1.stop();
//     }

//     //@Test
//     public void testLeave() throws InterruptedException, IOException, NoSuchAlgorithmException {
//         Integer PORT_1 = 5006;
//         String ADDRESS_1 = "127.0.0.1";
//         Thread th_1 = new Thread() {
//             @Override
//             public void run() {
//                 try {
//                     StartSimpleNioTestServer.main(new String[]{"-p", PORT_1.toString(), "-a", ADDRESS_1, "-ll", "WARNING"});
//                 } catch (IOException e) {
//                     e.printStackTrace();
//                 }
//             }
//         };
//         th_1.start(); // started the server
//         Thread.sleep(2000);

//         Socket s_1 = new Socket();
//         s_1.connect(new InetSocketAddress(ADDRESS_1, PORT_1));
//         BufferedReader input_1 = new BufferedReader(new InputStreamReader(s_1.getInputStream()));
//         input_1.readLine();

//         String hash_1 = computeHash(ADDRESS_1, PORT_1);
//         assertEquals("join_success", this.commandProcessor.process("join " + ADDRESS_1 + " " + PORT_1 + " " + PORT_1));
//         assertEquals("metadata_update " + hash_1 + "," + hash_1 + "," + ADDRESS_1 + ":" + PORT_1, this.commandProcessor.process("get_metadata"));

//         Integer PORT_2 = 5007;
//         String ADDRESS_2 = "127.0.0.1";
//         Thread th_2 = new Thread() {
//             @Override
//             public void run() {
//                 try {
//                     StartSimpleNioTestServer.main(new String[]{"-p", PORT_2.toString(), "-a", ADDRESS_2, "-ll", "WARNING"});
//                 } catch (IOException e) {
//                     e.printStackTrace();
//                 }
//             }
//         };
//         th_2.start(); // started the server
//         Thread.sleep(2000);

//         Socket s_2 = new Socket();
//         s_2.connect(new InetSocketAddress(ADDRESS_2, PORT_2));
//         BufferedReader input_2 = new BufferedReader(new InputStreamReader(s_2.getInputStream()));
//         input_2.readLine();

//         assertEquals("join_success " + ADDRESS_1 + " " + PORT_1 + " " + PORT_1, this.commandProcessor.process("join " + ADDRESS_2 + " " + PORT_2 + " " + PORT_2));
//         assertEquals("data_transfer_complete_success", this.commandProcessor.process("data_transfer_complete " + ADDRESS_1 + " " + PORT_1+ " " + PORT_1));

//         String hash_2 = computeHash(ADDRESS_2, PORT_2);
//         assertEquals("leave_error no server with this ip and port available.", this.commandProcessor.process("leave 123.1.1.1 1234"));
//         assertEquals("leave_success " + ADDRESS_2 + " " + PORT_2+ " " + PORT_2, this.commandProcessor.process("leave " + ADDRESS_1 + " " + PORT_1));
//         assertEquals("metadata_update " + hash_2 + "," + hash_2 + "," + ADDRESS_2 + ":" + PORT_2, this.commandProcessor.process("get_metadata"));

//         assertEquals("leave_success", this.commandProcessor.process("leave " + ADDRESS_2 + " " + PORT_2));
//         assertEquals("metadata_update_error no servers available.", this.commandProcessor.process("get_metadata"));

//         th_1.stop();
//         th_2.stop();
//     }

//     // ----------------------------------------- UTILITY METHODS -------------------------------------------------------
//     private String doRequest(Socket s, String req) throws IOException {
//         PrintWriter output = new PrintWriter(s.getOutputStream());
//         BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

//         output.write(req + "\r\n");
//         output.flush();

//         String res = input.readLine();
//         return res;
//     }

//     private String computeHash(String ip, int port) throws NoSuchAlgorithmException {
//         String ipAndPort = ip + ":" + port;
//         MessageDigest md = MessageDigest.getInstance("MD5");
//         md.update(ipAndPort.getBytes());
//         byte[] digest = md.digest();
//         StringBuilder hexString = new StringBuilder();
//         for (byte b : digest) {
//             hexString.append(Integer.toHexString(0xFF & b));
//         }
//         return hexString.toString();
//     }

//     private BigInteger hashToInt(String hash) {
//         return new BigInteger(hash, 16);
//     }

//     private String intToHash(BigInteger bigInteger) {
//         return bigInteger.toString(16);
//     }
// }
