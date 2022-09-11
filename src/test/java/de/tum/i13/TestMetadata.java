package de.tum.i13;

import de.tum.i13.shared.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class TestMetadata {

    // ----------------------------------------- JUnit utility methods -------------------------------------------------
    @BeforeAll
    public static void buildTestEnvironment() {

    }

    @AfterAll
    public static void tearDownEnvironment() {

    }

    @BeforeEach
    public void restartTestEnvironment() {

    }

    // ------------------------------------------------ TESTS ----------------------------------------------------------
    @Test
    public void testAddServerViaString() {
        String TEST_IP = "127.0.0.1";
        int TEST_PORT = 3000;
        String TEST_START_INDEX = "FFFFFFFFFFFFFFFF";
        String TEST_END_INDEX = "0000000000000000";
        String serverStr = TEST_START_INDEX + "," + TEST_END_INDEX + "," + TEST_IP + ":" + TEST_PORT;

        Metadata metadata = new Metadata();
        metadata.addServerViaString(serverStr, true);
        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        ServerEntry server = data.firstEntry().getValue();

        assertEquals(TEST_IP, server.getIp());
        assertEquals(TEST_PORT, server.getClientPort());
        assertEquals(TEST_START_INDEX, server.getStartIndex());
        assertEquals(TEST_END_INDEX, server.getEndIndex());
    }

    @Test
    public void testAddServerViaStringMultiple() {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3000;
        String TEST_START_INDEX_1 = "8888888888888888";
        String TEST_END_INDEX_1 = "3333333333333333";
        String serverStr_1 = TEST_START_INDEX_1 + "," + TEST_END_INDEX_1 + "," + TEST_IP_1 + ":" + TEST_PORT_1;

        String TEST_IP_2 = "127.0.0.2";
        int TEST_PORT_2 = 3002;
        String TEST_START_INDEX_2 = "3333333333333333";
        String TEST_END_INDEX_2 = "8888888888888888";
        String serverStr_2 = TEST_START_INDEX_2 + "," + TEST_END_INDEX_2 + "," + TEST_IP_2 + ":" + TEST_PORT_2;

        String entries = serverStr_1 + ";" + serverStr_2;
        Metadata metadata = new Metadata(true, entries);

        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        ServerEntry server_1 = data.lastEntry().getValue();
        ServerEntry server_2 = data.firstEntry().getValue();

        assertEquals(2, data.size());

        assertEquals(TEST_IP_1, server_1.getIp());
        assertEquals(TEST_PORT_1, server_1.getClientPort());
        assertEquals(TEST_START_INDEX_1, server_1.getStartIndex());
        assertEquals(TEST_END_INDEX_1, server_1.getEndIndex());

        assertEquals(TEST_IP_2, server_2.getIp());
        assertEquals(TEST_PORT_2, server_2.getClientPort());
        assertEquals(TEST_START_INDEX_2, server_2.getStartIndex());
        assertEquals(TEST_END_INDEX_2, server_2.getEndIndex());
    }

    @Test
    public void testAddNewServerOne() throws NoSuchAlgorithmException {
        String TEST_IP = "127.0.0.1";
        int TEST_PORT = 3001;
        String TEST_POSITION = computeHash(TEST_IP, TEST_PORT); // integer: 11638329695381480068006089459546486987

        Metadata metadata = new Metadata();
        Neighbors neighbors = metadata.addNewServer(TEST_IP, TEST_PORT, TEST_PORT);
        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        Map.Entry<BigInteger, ServerEntry> server = data.firstEntry();
        String serversToString = metadata.toString();

        assertNull(neighbors.getPredecessor());
        assertNull(neighbors.getSuccessor());

        assertEquals(1, data.size());

        assertEquals(hashToInt(TEST_POSITION), server.getKey());
        assertEquals(TEST_IP, server.getValue().getIp());
        assertEquals(TEST_PORT, server.getValue().getClientPort());
        assertEquals(TEST_POSITION, server.getValue().getStartIndex());
        assertEquals(TEST_POSITION, server.getValue().getEndIndex());

        assertEquals(TEST_POSITION + "," + TEST_POSITION + "," + TEST_IP + ":" + TEST_PORT + ":" + TEST_PORT, serversToString);
    }

    @Test
    public void testAddNewServerTwo() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1); // integer: 11638329695381480068006089459546486987

        String TEST_IP_2 = "127.0.0.2";
        int TEST_PORT_2 = 3002;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2); // integer: 333367160230099421107893970092079413397

        Metadata metadata = new Metadata();
        Neighbors neighbors_1 = metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        Neighbors neighbors_2 = metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        String serversToString = metadata.toClientString();

        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        Map.Entry<BigInteger, ServerEntry> server_1 = data.firstEntry();
        Map.Entry<BigInteger, ServerEntry> server_2 = data.lastEntry();

        // check if two elements where added
        assertEquals(2, data.size());

        // check if order of servers is preserved after insertion
        assert(hashToInt(TEST_POSITION_1).compareTo(hashToInt(TEST_POSITION_2)) < 0);
        assert(server_1.getKey().compareTo(server_2.getKey()) < 0);

        // check correct neighbors
        assertNull(neighbors_1.getPredecessor());
        assertNull(neighbors_1.getSuccessor());
        assertEquals(server_1.getValue(), neighbors_2.getPredecessor());
        assertEquals(server_1.getValue(), neighbors_2.getSuccessor());

        // check if servers where added with right attributes and indices
        assertEquals(hashToInt(TEST_POSITION_1), server_1.getKey());
        assertEquals(TEST_IP_1, server_1.getValue().getIp());
        assertEquals(TEST_PORT_1, server_1.getValue().getClientPort());
        assertEquals(TEST_POSITION_1, server_1.getValue().getStartIndex());
        assertEquals(TEST_POSITION_2, server_1.getValue().getEndIndex());
        assertEquals(hashToInt(TEST_POSITION_2), server_2.getKey());
        assertEquals(TEST_IP_2, server_2.getValue().getIp());
        assertEquals(TEST_PORT_2, server_2.getValue().getClientPort());
        assertEquals(TEST_POSITION_2, server_2.getValue().getStartIndex());
        assertEquals(TEST_POSITION_1, server_2.getValue().getEndIndex());

        // check if toString method displays the order of servers properly
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_1 + "," + TEST_IP_2 + ":" + TEST_PORT_2, serversToString);
    }

    @Test
    public void testAddNewServerThreeCase1() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.3";
        int TEST_PORT_2 = 3003;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.2";
        int TEST_PORT_3 = 3002;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        // invariant: TEST_POSITION_1 < TEST_POSITION_2 < TEST_POSITION_3
        assert(hashToInt(TEST_POSITION_1).compareTo(hashToInt(TEST_POSITION_2)) < 0);
        assert(hashToInt(TEST_POSITION_2).compareTo(hashToInt(TEST_POSITION_3)) < 0);
        assert(hashToInt(TEST_POSITION_3).compareTo(hashToInt(TEST_POSITION_1)) > 0);

        BigInteger[] TEST_POSITIONS = {hashToInt(TEST_POSITION_1), hashToInt(TEST_POSITION_2), hashToInt(TEST_POSITION_3)};
        String[] TEST_IPS = {TEST_IP_1, TEST_IP_2, TEST_IP_3};
        int[] TEST_PORTS = {TEST_PORT_1, TEST_PORT_2, TEST_PORT_3};
        String[] TEST_START_INDICES = {TEST_POSITION_1, TEST_POSITION_2, TEST_POSITION_3};
        String[] TEST_END_INDICES = {TEST_POSITION_2, TEST_POSITION_3, TEST_POSITION_1};

        /* Add two servers */
        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        TreeMap<BigInteger, ServerEntry> data = metadata.getData();

        Map.Entry<BigInteger, ServerEntry> server_2 = data.firstEntry();
        Map.Entry<BigInteger, ServerEntry> server_3 = data.lastEntry();

        // check if two elements where added
        assertEquals(2, data.size());

        // check if order of servers is preserved after insertion
        assert(server_2.getKey().compareTo(server_3.getKey()) < 0);

        /* Now insert a third server at the front */
        Neighbors neighbors_1 = metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        String serversToString = metadata.toClientString();
        data = metadata.getData();
        assertEquals(3, data.size());

        // check correct neighbors
        assertEquals(server_2.getValue(), neighbors_1.getSuccessor());
        assertEquals(server_3.getValue(), neighbors_1.getPredecessor());

        // check correct attributes
        int i = 0;
        for (Map.Entry<BigInteger, ServerEntry> s : data.entrySet()) {
            assertEquals(TEST_POSITIONS[i], s.getKey());
            assertEquals(TEST_IPS[i], s.getValue().getIp());
            assertEquals(TEST_PORTS[i], s.getValue().getClientPort());
            assertEquals(TEST_START_INDICES[i], s.getValue().getStartIndex());
            assertEquals(TEST_END_INDICES[i], s.getValue().getEndIndex());
            i++;
        }

        // check if toString method displays the order of servers properly
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, serversToString);
    }

    @Test
    public void testAddNewServerThreeCase2() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.3";
        int TEST_PORT_2 = 3003;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.2";
        int TEST_PORT_3 = 3002;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        // invariant: TEST_POSITION_1 < TEST_POSITION_2 < TEST_POSITION_3
        assert(hashToInt(TEST_POSITION_1).compareTo(hashToInt(TEST_POSITION_2)) < 0);
        assert(hashToInt(TEST_POSITION_2).compareTo(hashToInt(TEST_POSITION_3)) < 0);
        assert(hashToInt(TEST_POSITION_3).compareTo(hashToInt(TEST_POSITION_1)) > 0);

        BigInteger[] TEST_POSITIONS = {hashToInt(TEST_POSITION_1), hashToInt(TEST_POSITION_2), hashToInt(TEST_POSITION_3)};
        String[] TEST_IPS = {TEST_IP_1, TEST_IP_2, TEST_IP_3};
        int[] TEST_PORTS = {TEST_PORT_1, TEST_PORT_2, TEST_PORT_3};
        String[] TEST_START_INDICES = {TEST_POSITION_1, TEST_POSITION_2, TEST_POSITION_3};
        String[] TEST_END_INDICES = {TEST_POSITION_2, TEST_POSITION_3, TEST_POSITION_1};

        /* Add two servers */
        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        TreeMap<BigInteger, ServerEntry> data = metadata.getData();

        Map.Entry<BigInteger, ServerEntry> server_1 = data.firstEntry();
        Map.Entry<BigInteger, ServerEntry> server_3 = data.lastEntry();

        // check if two elements where added
        assertEquals(2, data.size());

        // check if order of servers is preserved after insertion
        assert(server_1.getKey().compareTo(server_3.getKey()) < 0);

        /* Now insert a third server at the front */
        Neighbors neighbors_2 = metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        String serversToString = metadata.toClientString();
        data = metadata.getData();
        assertEquals(3, data.size());

        // check correct neighbors
        assertEquals(server_3.getValue(), neighbors_2.getSuccessor());
        assertEquals(server_1.getValue(), neighbors_2.getPredecessor());

        // check correct attributes
        int i = 0;
        for (Map.Entry<BigInteger, ServerEntry> s : data.entrySet()) {
            assertEquals(TEST_POSITIONS[i], s.getKey());
            assertEquals(TEST_IPS[i], s.getValue().getIp());
            assertEquals(TEST_PORTS[i], s.getValue().getClientPort());
            assertEquals(TEST_START_INDICES[i], s.getValue().getStartIndex());
            assertEquals(TEST_END_INDICES[i], s.getValue().getEndIndex());
            i++;
        }

        // check if toString method displays the order of servers properly
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, serversToString);
    }

    @Test
    public void testAddNewServerThreeCase3() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.3";
        int TEST_PORT_2 = 3003;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.2";
        int TEST_PORT_3 = 3002;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        // invariant: TEST_POSITION_1 < TEST_POSITION_2 < TEST_POSITION_3
        assert(hashToInt(TEST_POSITION_1).compareTo(hashToInt(TEST_POSITION_2)) < 0);
        assert(hashToInt(TEST_POSITION_2).compareTo(hashToInt(TEST_POSITION_3)) < 0);
        assert(hashToInt(TEST_POSITION_3).compareTo(hashToInt(TEST_POSITION_1)) > 0);

        BigInteger[] TEST_POSITIONS = {hashToInt(TEST_POSITION_1), hashToInt(TEST_POSITION_2), hashToInt(TEST_POSITION_3)};
        String[] TEST_IPS = {TEST_IP_1, TEST_IP_2, TEST_IP_3};
        int[] TEST_PORTS = {TEST_PORT_1, TEST_PORT_2, TEST_PORT_3};
        String[] TEST_START_INDICES = {TEST_POSITION_1, TEST_POSITION_2, TEST_POSITION_3};
        String[] TEST_END_INDICES = {TEST_POSITION_2, TEST_POSITION_3, TEST_POSITION_1};

        /* Add two servers */
        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        TreeMap<BigInteger, ServerEntry> data = metadata.getData();

        Map.Entry<BigInteger, ServerEntry> server_1 = data.firstEntry();
        Map.Entry<BigInteger, ServerEntry> server_2 = data.lastEntry();

        // check if two elements where added
        assertEquals(2, data.size());

        // check if order of servers is preserved after insertion
        assert(server_1.getKey().compareTo(server_2.getKey()) < 0);

        /* Now insert a third server at the front */
        Neighbors neighbors_3 = metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        String serversToString = metadata.toClientString();
        data = metadata.getData();
        assertEquals(3, data.size());

        // check correct neighbors
        assertEquals(server_1.getValue(), neighbors_3.getSuccessor());
        assertEquals(server_2.getValue(), neighbors_3.getPredecessor());

        // check correct attributes
        int i = 0;
        for (Map.Entry<BigInteger, ServerEntry> s : data.entrySet()) {
            assertEquals(TEST_POSITIONS[i], s.getKey());
            assertEquals(TEST_IPS[i], s.getValue().getIp());
            assertEquals(TEST_PORTS[i], s.getValue().getClientPort());
            assertEquals(TEST_START_INDICES[i], s.getValue().getStartIndex());
            assertEquals(TEST_END_INDICES[i], s.getValue().getEndIndex());
            i++;
        }

        // check if toString method displays the order of servers properly
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, serversToString);
    }

    @Test
    public void testAddNewServerMultiple() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.4";
        int TEST_PORT_1 = 3004;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.1";
        int TEST_PORT_2 = 3001;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.5";
        int TEST_PORT_3 = 3005;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        String TEST_IP_4 = "127.0.0.3";
        int TEST_PORT_4 = 3003;
        String TEST_POSITION_4 = computeHash(TEST_IP_4, TEST_PORT_4);

        String TEST_IP_5 = "127.0.0.2";
        int TEST_PORT_5 = 3002;
        String TEST_POSITION_5 = computeHash(TEST_IP_5, TEST_PORT_5);

        // invariant: TEST_POSITION_1 < TEST_POSITION_2 < TEST_POSITION_3 < TEST_POSITION_4 < TEST_POSITION_5
        assert(hashToInt(TEST_POSITION_1).compareTo(hashToInt(TEST_POSITION_2)) < 0);
        assert(hashToInt(TEST_POSITION_2).compareTo(hashToInt(TEST_POSITION_3)) < 0);
        assert(hashToInt(TEST_POSITION_3).compareTo(hashToInt(TEST_POSITION_4)) < 0);
        assert(hashToInt(TEST_POSITION_4).compareTo(hashToInt(TEST_POSITION_5)) < 0);
        assert(hashToInt(TEST_POSITION_5).compareTo(hashToInt(TEST_POSITION_1)) > 0);

        BigInteger[] TEST_POSITIONS = {hashToInt(TEST_POSITION_1), hashToInt(TEST_POSITION_2), hashToInt(TEST_POSITION_3),
                hashToInt(TEST_POSITION_4), hashToInt(TEST_POSITION_5)};
        String[] TEST_IPS = {TEST_IP_1, TEST_IP_2, TEST_IP_3, TEST_IP_4, TEST_IP_5};
        int[] TEST_PORTS = {TEST_PORT_1, TEST_PORT_2, TEST_PORT_3, TEST_PORT_4, TEST_PORT_5};
        String[] TEST_START_INDICES = {TEST_POSITION_1, TEST_POSITION_2, TEST_POSITION_3, TEST_POSITION_4, TEST_POSITION_5};
        String[] TEST_END_INDICES = {TEST_POSITION_2, TEST_POSITION_3, TEST_POSITION_4, TEST_POSITION_5, TEST_POSITION_1};

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        metadata.addNewServer(TEST_IP_5, TEST_PORT_5, TEST_PORT_5);
        metadata.addNewServer(TEST_IP_4, TEST_PORT_4, TEST_PORT_4);

        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        String serversToString = metadata.toClientString();

        assertEquals(data.size(), 5);

        // check correct attributes
        int i = 0;
        for (Map.Entry<BigInteger, ServerEntry> s : data.entrySet()) {
            assertEquals(TEST_POSITIONS[i], s.getKey());
            assertEquals(TEST_IPS[i], s.getValue().getIp());
            assertEquals(TEST_PORTS[i], s.getValue().getClientPort());
            assertEquals(TEST_START_INDICES[i], s.getValue().getStartIndex());
            assertEquals(TEST_END_INDICES[i], s.getValue().getEndIndex());
            i++;
        }

        // check if toString method displays the order of servers properly
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_4 + "," + TEST_IP_3 + ":" + TEST_PORT_3 + ";"
                + TEST_POSITION_4 + "," + TEST_POSITION_5 + "," + TEST_IP_4 + ":" + TEST_PORT_4 + ";"
                + TEST_POSITION_5 + "," + TEST_POSITION_1 + "," + TEST_IP_5 + ":" + TEST_PORT_5, serversToString);
    }

    @Test
    public void testDeleteServerOne() throws NoSuchAlgorithmException {
        String TEST_IP = "127.0.0.1";
        int TEST_PORT = 3001;
        String TEST_POSITION = computeHash(TEST_IP, TEST_PORT);

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP, TEST_PORT, TEST_PORT);
        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        ServerEntry server = data.firstEntry().getValue();
        assertEquals(data.size(), 1);

        NeighborsAndSelf neighborsAndSelf = metadata.deleteServer(TEST_IP, TEST_PORT);
        data = metadata.getData();

        assertEquals(0, data.size());
        assertNull(neighborsAndSelf.getPredecessor());
        assertEquals(server, neighborsAndSelf.getSelf());
        assertNull(neighborsAndSelf.getSuccessor());
    }

    @Test
    public void testDeleteServerTwo() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.2";
        int TEST_PORT_2 = 3002;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);

        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        assertEquals(2, data.size());
        Map.Entry<BigInteger, ServerEntry> deletedServer = data.firstEntry();
        Map.Entry<BigInteger, ServerEntry> otherServer = data.lastEntry();

        NeighborsAndSelf neighborsAndSelf = metadata.deleteServer(TEST_IP_1, TEST_PORT_1);
        data = metadata.getData();
        String serversToString = metadata.toClientString();

        assertEquals(1, data.size());
        assertEquals(otherServer.getValue(), neighborsAndSelf.getPredecessor());
        assertEquals(deletedServer.getValue(), neighborsAndSelf.getSelf());
        assertEquals(otherServer.getValue(), neighborsAndSelf.getSuccessor());

        assertEquals(hashToInt(TEST_POSITION_2), otherServer.getKey());
        assertEquals(TEST_IP_2, otherServer.getValue().getIp());
        assertEquals(TEST_PORT_2, otherServer.getValue().getClientPort());
        assertEquals(TEST_POSITION_2, otherServer.getValue().getStartIndex());
        assertEquals(TEST_POSITION_2, otherServer.getValue().getEndIndex());

        assertEquals(TEST_POSITION_2 + "," + TEST_POSITION_2 + "," + TEST_IP_2 + ":" + TEST_PORT_2, serversToString);

        /* now test for deletion of the second server*/
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        assertEquals(2, metadata.getData().size());

        deletedServer = data.lastEntry();
        otherServer = data.firstEntry();

        neighborsAndSelf = metadata.deleteServer(TEST_IP_2, TEST_PORT_2);
        data = metadata.getData();
        serversToString = metadata.toClientString();

        assertEquals(1, data.size());
        assertEquals(otherServer.getValue(), neighborsAndSelf.getPredecessor());
        assertEquals(deletedServer.getValue(), neighborsAndSelf.getSelf());
        assertEquals(otherServer.getValue(), neighborsAndSelf.getSuccessor());

        assertEquals(hashToInt(TEST_POSITION_1), otherServer.getKey());
        assertEquals(TEST_IP_1, otherServer.getValue().getIp());
        assertEquals(TEST_PORT_1, otherServer.getValue().getClientPort());
        assertEquals(TEST_POSITION_1, otherServer.getValue().getStartIndex());
        assertEquals(TEST_POSITION_1, otherServer.getValue().getEndIndex());

        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_1+ "," + TEST_IP_1 + ":" + TEST_PORT_1, serversToString);
    }

    @Test
    public void testGetNeighborsCase1() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.3";
        int TEST_PORT_2 = 3003;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.2";
        int TEST_PORT_3 = 3002;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        System.out.println(metadata.toString());
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, metadata.toClientString());


        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        assertEquals(3, data.size());

        NeighborsAndSelf neighborsAndSelf = metadata.getNeighbors(TEST_IP_1, TEST_PORT_1);
        data = metadata.getData();

        ServerEntry server_1 = metadata.getServerEntry(TEST_IP_1 + ":" + TEST_PORT_1);
        ServerEntry server_2 = metadata.getServerEntry(TEST_IP_2 + ":" + TEST_PORT_2);
        ServerEntry server_3 = metadata.getServerEntry(TEST_IP_3 + ":" + TEST_PORT_3);

        assertEquals(server_2, neighborsAndSelf.getSuccessor());
        assertEquals(server_1, neighborsAndSelf.getSelf());
        assertEquals(server_3, neighborsAndSelf.getPredecessor());
    }

    @Test
    public void testGetNeighborsCase2() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.3";
        int TEST_PORT_2 = 3003;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.2";
        int TEST_PORT_3 = 3002;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        System.out.println(metadata.toString());
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, metadata.toClientString());


        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        assertEquals(3, data.size());

        NeighborsAndSelf neighborsAndSelf = metadata.getNeighbors(TEST_IP_2, TEST_PORT_2);
        data = metadata.getData();

        ServerEntry server_1 = metadata.getServerEntry(TEST_IP_1 + ":" + TEST_PORT_1);
        ServerEntry server_2 = metadata.getServerEntry(TEST_IP_2 + ":" + TEST_PORT_2);
        ServerEntry server_3 = metadata.getServerEntry(TEST_IP_3 + ":" + TEST_PORT_3);

        assertEquals(server_3, neighborsAndSelf.getSuccessor());
        assertEquals(server_2, neighborsAndSelf.getSelf());
        assertEquals(server_1, neighborsAndSelf.getPredecessor());
    }

    @Test
    public void testGetNeighborsCase3() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.3";
        int TEST_PORT_2 = 3003;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.2";
        int TEST_PORT_3 = 3002;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        System.out.println(metadata.toString());
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, metadata.toClientString());


        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        assertEquals(3, data.size());

        NeighborsAndSelf neighborsAndSelf = metadata.getNeighbors(TEST_IP_3, TEST_PORT_3);
        data = metadata.getData();

        ServerEntry server_1 = metadata.getServerEntry(TEST_IP_1 + ":" + TEST_PORT_1);
        ServerEntry server_2 = metadata.getServerEntry(TEST_IP_2 + ":" + TEST_PORT_2);
        ServerEntry server_3 = metadata.getServerEntry(TEST_IP_3 + ":" + TEST_PORT_3);

        assertEquals(server_1, neighborsAndSelf.getSuccessor());
        assertEquals(server_3, neighborsAndSelf.getSelf());
        assertEquals(server_2, neighborsAndSelf.getPredecessor()); // server 3
    }

    @Test
    public void testDeleteServerThreeCase1() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.3";
        int TEST_PORT_2 = 3003;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.2";
        int TEST_PORT_3 = 3002;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        System.out.println(metadata.toString());
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, metadata.toClientString());


        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        assertEquals(3, data.size());
        Map.Entry<BigInteger, ServerEntry> deletedServer = data.firstEntry();

        NeighborsAndSelf neighborsAndSelf = metadata.deleteServer(TEST_IP_1, TEST_PORT_1);
        System.out.println(metadata.toString());
        data = metadata.getData();
        Map.Entry<BigInteger, ServerEntry> server_2 = data.firstEntry();
        Map.Entry<BigInteger, ServerEntry> server_3 = data.lastEntry();

        assertEquals(2, data.size());
        assertEquals(server_2.getValue(), neighborsAndSelf.getSuccessor());
        assertEquals(deletedServer.getValue(), neighborsAndSelf.getSelf());
        assertEquals(server_3.getValue(), neighborsAndSelf.getPredecessor());
        assertEquals(neighborsAndSelf.getSelf().getStartIndex(), TEST_POSITION_1);

        BigInteger[] TEST_POSITIONS = {hashToInt(TEST_POSITION_2), hashToInt(TEST_POSITION_3)};
        String[] TEST_IPS = {TEST_IP_2, TEST_IP_3};
        int[] TEST_PORTS = {TEST_PORT_2, TEST_PORT_3};
        String[] TEST_START_INDICES = {TEST_POSITION_2, TEST_POSITION_3};
        String[] TEST_END_INDICES = {TEST_POSITION_3, TEST_POSITION_2};

        // check correct attributes
        int i = 0;
        for (Map.Entry<BigInteger, ServerEntry> s : data.entrySet()) {
            assertEquals(TEST_POSITIONS[i], s.getKey());
            assertEquals(TEST_IPS[i], s.getValue().getIp());
            assertEquals(TEST_PORTS[i], s.getValue().getClientPort());
            assertEquals(TEST_START_INDICES[i], s.getValue().getStartIndex());
            assertEquals(TEST_END_INDICES[i], s.getValue().getEndIndex());
            i++;
        }

        assertEquals(TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_2 + "," + TEST_IP_3 + ":" + TEST_PORT_3, metadata.toClientString());
    }

    @Test
    public void testDeleteServerThreeCase2() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.3";
        int TEST_PORT_2 = 3003;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.2";
        int TEST_PORT_3 = 3002;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        System.out.println(metadata.toString());
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, metadata.toClientString());

        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        assertEquals(3, data.size());
        Map.Entry<BigInteger, ServerEntry> server_1 = data.firstEntry();

        NeighborsAndSelf neighborsAndSelf = metadata.deleteServer(TEST_IP_2, TEST_PORT_2);
        System.out.println(metadata.toString());

        Map.Entry<BigInteger, ServerEntry> server_3 = data.lastEntry();
        data = metadata.getData();

        assertEquals(2, data.size());
        assertEquals(server_3.getValue(), neighborsAndSelf.getSuccessor());
        assertEquals(server_1.getValue(), neighborsAndSelf.getPredecessor());
        assertEquals(neighborsAndSelf.getSelf().getStartIndex(), TEST_POSITION_2);

        BigInteger[] TEST_POSITIONS = {hashToInt(TEST_POSITION_1), hashToInt(TEST_POSITION_3)};
        String[] TEST_IPS = {TEST_IP_1, TEST_IP_3};
        int[] TEST_PORTS = {TEST_PORT_1, TEST_PORT_3};
        String[] TEST_START_INDICES = {TEST_POSITION_1, TEST_POSITION_3};
        String[] TEST_END_INDICES = {TEST_POSITION_3, TEST_POSITION_1};

        // check correct attributes
        int i = 0;
        for (Map.Entry<BigInteger, ServerEntry> s : data.entrySet()) {
            assertEquals(TEST_POSITIONS[i], s.getKey());
            assertEquals(TEST_IPS[i], s.getValue().getIp());
            assertEquals(TEST_PORTS[i], s.getValue().getClientPort());
            assertEquals(TEST_START_INDICES[i], s.getValue().getStartIndex());
            assertEquals(TEST_END_INDICES[i], s.getValue().getEndIndex());
            i++;
        }

        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_3 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, (metadata.toClientString()));
    }

    @Test
    public void testDeleteServerThreeCase3() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 3001;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.3";
        int TEST_PORT_2 = 3003;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.2";
        int TEST_PORT_3 = 3002;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        //System.out.println(TEST_POSITION_1 + " " + TEST_POSITION_2 + " " + TEST_POSITION_3);
        //System.out.println(new BigInteger(TEST_POSITION_1, 16) + " " + new BigInteger(TEST_POSITION_2, 16) + " " + new BigInteger(TEST_POSITION_3, 16));

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        System.out.println(metadata.toString());
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_1 + "," + TEST_IP_3 + ":" + TEST_PORT_3, metadata.toClientString());

        TreeMap<BigInteger, ServerEntry> data = metadata.getData();
        assertEquals(3, data.size());
        Map.Entry<BigInteger, ServerEntry> deletedServer = data.lastEntry();

        NeighborsAndSelf neighborsAndSelf = metadata.deleteServer(TEST_IP_3, TEST_PORT_3);
        System.out.println(metadata.toString());
        data = metadata.getData();
        Map.Entry<BigInteger, ServerEntry> server_1 = data.firstEntry();
        Map.Entry<BigInteger, ServerEntry> server_2 = data.lastEntry();

        assertEquals(2, data.size());
        assertEquals(server_1.getValue(), neighborsAndSelf.getSuccessor());
        assertEquals(deletedServer.getValue(), neighborsAndSelf.getSelf());
        assertEquals(server_2.getValue(), neighborsAndSelf.getPredecessor());
        assertEquals(deletedServer.getValue().getStartIndex(), TEST_POSITION_3);
        assertEquals(neighborsAndSelf.getSelf().getStartIndex(), TEST_POSITION_3);

        BigInteger[] TEST_POSITIONS = {hashToInt(TEST_POSITION_1), hashToInt(TEST_POSITION_2)};
        String[] TEST_IPS = {TEST_IP_1, TEST_IP_2};
        int[] TEST_PORTS = {TEST_PORT_1, TEST_PORT_2};
        String[] TEST_START_INDICES = {TEST_POSITION_1, TEST_POSITION_2};
        String[] TEST_END_INDICES = {TEST_POSITION_2, TEST_POSITION_1};

        // check correct attributes
        int i = 0;
        for (Map.Entry<BigInteger, ServerEntry> s : data.entrySet()) {
            assertEquals(TEST_POSITIONS[i], s.getKey());
            assertEquals(TEST_IPS[i], s.getValue().getIp());
            assertEquals(TEST_PORTS[i], s.getValue().getClientPort());
            assertEquals(TEST_START_INDICES[i], s.getValue().getStartIndex());
            assertEquals(TEST_END_INDICES[i], s.getValue().getEndIndex());
            i++;
        }

        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_1 + "," + TEST_IP_2 + ":" + TEST_PORT_2, metadata.toClientString());
    }

    @Test
    public void testDeleteServerMultiple() throws NoSuchAlgorithmException {
        String TEST_IP_1 = "127.0.0.4";
        int TEST_PORT_1 = 3004;
        String TEST_POSITION_1 = computeHash(TEST_IP_1, TEST_PORT_1);

        String TEST_IP_2 = "127.0.0.1";
        int TEST_PORT_2 = 3001;
        String TEST_POSITION_2 = computeHash(TEST_IP_2, TEST_PORT_2);

        String TEST_IP_3 = "127.0.0.5";
        int TEST_PORT_3 = 3005;
        String TEST_POSITION_3 = computeHash(TEST_IP_3, TEST_PORT_3);

        String TEST_IP_4 = "127.0.0.3";
        int TEST_PORT_4 = 3003;
        String TEST_POSITION_4 = computeHash(TEST_IP_4, TEST_PORT_4);

        String TEST_IP_5 = "127.0.0.2";
        int TEST_PORT_5 = 3002;
        String TEST_POSITION_5 = computeHash(TEST_IP_5, TEST_PORT_5);

        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_2, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_3, TEST_PORT_3, TEST_PORT_3);
        metadata.addNewServer(TEST_IP_5, TEST_PORT_5, TEST_PORT_5);
        metadata.addNewServer(TEST_IP_4, TEST_PORT_4, TEST_PORT_4);

        // check if toString method displays the order of servers properly
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_3 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_3 + "," + TEST_POSITION_4 + "," + TEST_IP_3 + ":" + TEST_PORT_3 + ";"
                + TEST_POSITION_4 + "," + TEST_POSITION_5 + "," + TEST_IP_4 + ":" + TEST_PORT_4 + ";"
                + TEST_POSITION_5 + "," + TEST_POSITION_1 + "," + TEST_IP_5 + ":" + TEST_PORT_5, metadata.toClientString());

        metadata.deleteServer(TEST_IP_3, TEST_PORT_3);
        assertEquals(TEST_POSITION_1 + "," + TEST_POSITION_2 + "," + TEST_IP_1 + ":" + TEST_PORT_1 + ";"
                + TEST_POSITION_2 + "," + TEST_POSITION_4 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_4 + "," + TEST_POSITION_5 + "," + TEST_IP_4 + ":" + TEST_PORT_4 + ";"
                + TEST_POSITION_5 + "," + TEST_POSITION_1 + "," + TEST_IP_5 + ":" + TEST_PORT_5, metadata.toClientString());

        metadata.deleteServer(TEST_IP_1, TEST_PORT_1);
        assertEquals(TEST_POSITION_2 + "," + TEST_POSITION_4 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_4 + "," + TEST_POSITION_5 + "," + TEST_IP_4 + ":" + TEST_PORT_4 + ";"
                + TEST_POSITION_5 + "," + TEST_POSITION_2 + "," + TEST_IP_5 + ":" + TEST_PORT_5, metadata.toClientString());

        metadata.deleteServer(TEST_IP_5, TEST_PORT_5);
        assertEquals(TEST_POSITION_2 + "," + TEST_POSITION_4 + "," + TEST_IP_2 + ":" + TEST_PORT_2 + ";"
                + TEST_POSITION_4 + "," + TEST_POSITION_2 + "," + TEST_IP_4 + ":" + TEST_PORT_4, metadata.toClientString());

        metadata.deleteServer(TEST_IP_4, TEST_PORT_4);
        assertEquals(TEST_POSITION_2 + "," + TEST_POSITION_2 + "," + TEST_IP_2 + ":" + TEST_PORT_2, metadata.toClientString());

        metadata.deleteServer(TEST_IP_2, TEST_PORT_2);
        assertEquals("", metadata.toClientString());
    }

    @Test
    public void testGetNeighborsInvariant() {
        String TEST_IP_1 = "127.0.0.1";
        int TEST_PORT_1 = 7175;

        int TEST_PORT_2 = 7177;

        int TEST_PORT_3 = 7179;

        int TEST_PORT_4 = 5153;


        Metadata metadata = new Metadata();
        metadata.addNewServer(TEST_IP_1, TEST_PORT_1, TEST_PORT_1);
        metadata.addNewServer(TEST_IP_1, TEST_PORT_2, TEST_PORT_2);
        metadata.addNewServer(TEST_IP_1, TEST_PORT_3, TEST_PORT_3);
        metadata.addNewServer(TEST_IP_1, TEST_PORT_4, TEST_PORT_4);

        String before=metadata.toString();
        metadata.getNeighbors(TEST_IP_1, TEST_PORT_1);
        String after=metadata.toString();
        assertEquals(before, after);

        before = metadata.toString();
        metadata.getNeighbors(TEST_IP_1, TEST_PORT_2);
        after = metadata.toString();
        assertEquals(before, after);

        before = metadata.toString();
        metadata.getNeighbors(TEST_IP_1, TEST_PORT_3);
        after = metadata.toString();
        assertEquals(before, after);

        before = metadata.toString();
        metadata.getNeighbors(TEST_IP_1, TEST_PORT_4);
        after = metadata.toString();
        assertEquals(before, after);
    }

    // ----------------------------------------- UTILITY METHODS -------------------------------------------------------
    private String computeHash(String ip, int port) throws NoSuchAlgorithmException {
        return Metadata.generateHash(ip +":" + port);
    }

    private BigInteger hashToInt(String hash) {
        return new BigInteger(hash, 16);
    }

    private String intToHash(BigInteger bigInteger) {
        return bigInteger.toString(16);
    }


}

