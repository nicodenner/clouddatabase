package de.tum.i13;

import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.PersistenceKVStore;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestKVCommandProcessor {
    private static final String TESTING_DIRECTORY = "src/test/testdata/";
    private KVCommandProcessor commandProcessor;

    // ----------------------------------------- JUnit utility methods -------------------------------------------------
    //@BeforeAll
    public static void buildTestEnvironment() {
        deleteFiles();
    }

    //@AfterAll
    public static void tearDownEnvironment() {
        deleteFiles();
    }

    //@BeforeEach
    public void restartTestEnvironment() {
        deleteFiles();
        Config cfg = new Config();
        cfg.cacheStrategy = Constants.FIFO;
        cfg.cacheSize = 10;
        cfg.dataDir = Paths.get(TESTING_DIRECTORY);
        PersistenceKVStore kvStore = new PersistenceKVStore(cfg);
        this.commandProcessor = new KVCommandProcessor(kvStore);
    }
    /*
    // ----------------------------------------- PUT TESTS -------------------------------------------------------------
    //@Test
    public void testPutSuccessNormalInput() {
        assertEquals("put_success key", this.commandProcessor.process("put key val"));
        assertEquals("put_success val", this.commandProcessor.process("put val key"));
        assertEquals("put_success 12345", this.commandProcessor.process("put 12345 a"));
        assertEquals("put_success a", this.commandProcessor.process("put a 12345"));
        assertEquals("put_success b", this.commandProcessor.process("put b 12345 ffwfwaf fwfawfk0g4jo"));
    }

    //@Test
    public void testPutSuccessEdgeCases() {
        // test if keys up to 20 Bytes are accepted
        byte[] keyArray = new byte[20];
        Arrays.fill(keyArray, (byte) 'a'); // to generate random string: new Random().nextBytes(array);
        String keyString = new String(keyArray, Charset.forName(Constants.TELNET_ENCODING));
        assertEquals("put_success " + keyString, this.commandProcessor.process("put " + keyString + " val"));

        // test if value up to 120 KBytes are accepted
        byte[] valueArray = new byte[122880];
        Arrays.fill(valueArray, (byte) 'a');
        String valueString = new String(valueArray, Charset.forName(Constants.TELNET_ENCODING));
        assertEquals("put_success key", this.commandProcessor.process("put key " + valueString));

        // check if special characters are sanitized
        assertEquals("put_success key1", this.commandProcessor.process("put key1\r\n 12345"));
        assertEquals("put_success key2", this.commandProcessor.process("put key2 12345\r\n"));
        assertEquals("put_success key3", this.commandProcessor.process("put key3\n 12345"));
        assertEquals("put_success key4", this.commandProcessor.process("put key4 12345\n"));
        assertEquals("put_success key5", this.commandProcessor.process("put key5\r 12345"));
        assertEquals("put_success key6", this.commandProcessor.process("put key6 12345\r"));
        assertEquals("put_success key7", this.commandProcessor.process("put key7\n\r 12345"));
        assertEquals("put_success key8", this.commandProcessor.process("put key8 12345\n\r"));
    }

    //@Test
    public void testPutUpdateNormalInput() {
        this.commandProcessor.process("put key val1");
        assertEquals("put_update key", this.commandProcessor.process("put key val2"));
        assertEquals("put_update key", this.commandProcessor.process("put key val3"));
    }

    //@Test
    public void testPutUpdateEdgeCases() {
        this.commandProcessor.process("put key val1");

        byte[] valueArray = new byte[122880];
        Arrays.fill(valueArray, (byte) 'a');
        String valueString = new String(valueArray, Charset.forName(Constants.TELNET_ENCODING));
        assertEquals("put_update key", this.commandProcessor.process("put key " + valueString));
        assertEquals("put_update key", this.commandProcessor.process("put key val1"));
    }

    // ----------------------------------------- GET TESTS -------------------------------------------------------------
    //@Test
    public void testGetSuccessNormalInput() {
        this.commandProcessor.process("put key val");
        assertEquals("get_success key val", this.commandProcessor.process("get key"));
        this.commandProcessor.process("put val key");
        assertEquals("get_success val key", this.commandProcessor.process("get val"));
        this.commandProcessor.process("put 12345 a");
        assertEquals("get_success 12345 a", this.commandProcessor.process("get 12345"));
        this.commandProcessor.process("put a 12345");
        assertEquals("get_success a 12345", this.commandProcessor.process("get a"));
        this.commandProcessor.process("put b 12345 ffwfwaf fwfawfk0g4jo");
        assertEquals("get_success b 12345 ffwfwaf fwfawfk0g4jo", this.commandProcessor.process("get b"));
    }

    //@Test
    public void testGetSuccessEdgeCases() {
        byte[] keyArray = new byte[20];
        Arrays.fill(keyArray, (byte) 'a'); // to generate random string: new Random().nextBytes(array);
        String keyString = new String(keyArray, Charset.forName(Constants.TELNET_ENCODING));
        this.commandProcessor.process("put " + keyString + " val");
        assertEquals("get_success " + keyString + " val", this.commandProcessor.process("get " + keyString));

        byte[] valueArray = new byte[122880];
        Arrays.fill(valueArray, (byte) 'a');
        String valueString = new String(valueArray, Charset.forName(Constants.TELNET_ENCODING));
        this.commandProcessor.process("put key " + valueString);
        assertEquals("get_success key " + valueString, this.commandProcessor.process("get key"));

        // check if special characters are sanitized
        this.commandProcessor.process("put key1\r\n 12345");
        assertEquals("get_success key1 12345", this.commandProcessor.process("get key1"));
        this.commandProcessor.process("put key2 12345\r\n");
        assertEquals("get_success key2 12345", this.commandProcessor.process("get key2"));
        this.commandProcessor.process("put key3\n 12345");
        assertEquals("get_success key3 12345", this.commandProcessor.process("get key3"));
        this.commandProcessor.process("put key4 12345\n");
        assertEquals("get_success key4 12345", this.commandProcessor.process("get key4"));
        this.commandProcessor.process("put key5\r 12345");
        assertEquals("get_success key5 12345", this.commandProcessor.process("get key5"));
        this.commandProcessor.process("put key6 12345\r");
        assertEquals("get_success key6 12345", this.commandProcessor.process("get key6"));
        this.commandProcessor.process("put key7\n\r 12345");
        assertEquals("get_success key7 12345", this.commandProcessor.process("get key7"));
        this.commandProcessor.process("put key8 12345\n\r");
        assertEquals("get_success key8 12345", this.commandProcessor.process("get key8"));
    }

    //@Test
    public void testGetError() {
        assertEquals("get_error key key is not found!", this.commandProcessor.process("get key"));

        byte[] keyArray = new byte[20];
        Arrays.fill(keyArray, (byte) 'a'); // to generate random string: new Random().nextBytes(array);
        String keyString = new String(keyArray, Charset.forName(Constants.TELNET_ENCODING));
        assertEquals("get_error " + keyString + " key is not found!", this.commandProcessor.process("get " + keyString));
    }

    // ----------------------------------------- DELETE TESTS ----------------------------------------------------------
    //@Test
    public void testDeleteSuccess() {
        this.commandProcessor.process("put key val");
        assertEquals("delete_success key", this.commandProcessor.process("delete key"));
    }

    //@Test
    public void testDeleteError() {
        assertEquals("delete_error key key is not found!", this.commandProcessor.process("delete key"));

        byte[] keyArray = new byte[20];
        Arrays.fill(keyArray, (byte) 'a'); // to generate random string: new Random().nextBytes(array);
        String keyString = new String(keyArray, Charset.forName(Constants.TELNET_ENCODING));
        assertEquals("delete_error " + keyString + " key is not found!", this.commandProcessor.process("delete " + keyString));

        this.commandProcessor.process("put key val");
        this.commandProcessor.process("delete key");
        assertEquals("delete_error key key is not found!", this.commandProcessor.process("delete key")); // already deleted
    }

    // ----------------------------------------- ERROR TESTS -----------------------------------------------------------
    //@Test
    public void testArbitraryCommand() {
        assertEquals("error unknown command!", this.commandProcessor.process("hello"));
        assertEquals("error unknown command!", this.commandProcessor.process("hello foo"));
        assertEquals("error unknown command!", this.commandProcessor.process("hello foo bar"));
        assertEquals("error unknown command!", this.commandProcessor.process("PUT"));
        assertEquals("put_error a invalid number of arguments!", this.commandProcessor.process("put a"));
        assertEquals("error unknown command!", this.commandProcessor.process("puta b"));
        assertEquals("error unknown command!", this.commandProcessor.process("GET"));
        assertEquals("get_error a invalid number of arguments!", this.commandProcessor.process("get a b"));
        assertEquals("error unknown command!", this.commandProcessor.process("DELETE"));
        assertEquals("delete_error a invalid number of arguments!", this.commandProcessor.process("delete a b"));
    }

    // ----------------------------------------- COMBINED TESTS --------------------------------------------------------
    //@Test
    public void testAllCommands() {
        assertEquals("delete_error key key is not found!", this.commandProcessor.process("delete key"));
        assertEquals("get_error key key is not found!", this.commandProcessor.process("get key"));
        assertEquals("put_success key", this.commandProcessor.process("put key val1"));
        assertEquals("get_success key val1", this.commandProcessor.process("get key"));
        assertEquals("put_update key", this.commandProcessor.process("put key val2"));
        assertEquals("get_success key val2", this.commandProcessor.process("get key"));
        assertEquals("delete_success key", this.commandProcessor.process("delete key"));
        assertEquals("get_error key key is not found!", this.commandProcessor.process("get key"));
        assertEquals("delete_error key key is not found!", this.commandProcessor.process("delete key"));
    }

     */

    // ----------------------------------------- UTILITY METHODS -------------------------------------------------------
    private static void deleteFiles() {
        File directory = new File(TESTING_DIRECTORY);
        for (File file: Objects.requireNonNull(directory.listFiles())) {
            if (!file.isDirectory() && !file.getName().equals(".gitignore")) {
                file.delete();
            }
        }
    }
}
