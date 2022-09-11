package de.tum.i13.shared;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Class that maintains the status of all servers currently running in the cloud.
 */
public class Metadata implements Serializable {
    public static Logger logger = Logger.getLogger(Metadata.class.getName());
    private TreeMap<BigInteger, ServerEntry> servers;

    public Metadata(String entries){
        this.servers = new TreeMap<>();
        String[] serverEntries = entries.split(";");
        for (String serverEntry : serverEntries) {
            addServerViaString(serverEntry);
        }
    }

    public Metadata(boolean isClient, String entries){
        this.servers = new TreeMap<>();
        String[] serverEntries = entries.split(";");
        for (String serverEntry : serverEntries) {
            addServerViaString(serverEntry, isClient);
        }
    }

    public Metadata() {
        this.servers = new TreeMap<>();
    }

    public TreeMap<BigInteger, ServerEntry> getData() {
        return servers;
    }

    public void addServerViaString(String serverData) {
        ServerEntry server = new ServerEntry(serverData.trim());
        logger.info("Add server via string " + server.getStartIndex());
        this.servers.putIfAbsent(hashToInt(server.getStartIndex()), server);
    }

    public void addServerViaString(String serverData, boolean isClient) {
        ServerEntry server = new ServerEntry(isClient, serverData.trim());
        logger.info("Add server via string " + server.getStartIndex());
        this.servers.putIfAbsent(hashToInt(server.getStartIndex()), server);
    }

    /**
     * Adds a new server to the system.
     *
     * @param ip the new servers ip.
     * @param clientPort the new servers port where clients connect to (used for hashing).
     * @param serverPort the new servers server port where servers and the ECS connect to.
     * @return its neighbors in the hashring.
     * (pred==succ if there are only two servers after insertion, pred==succ==null if there is only one server)
     */
    public Neighbors addNewServer(String ip, int clientPort, int serverPort) {
        // get key for new server to be added
        String ipAndPort = ip + ":" + clientPort;
        String newServerPosition = generateHash(ipAndPort);
        logger.info("Trying to add new server " + ip + ":" + clientPort + ":" + serverPort);
        /* --------------------------------- add new server to the circle --------------------------------- */
        ServerEntry newServer;
        if (this.servers.size() == 0) {
            // this is the first server
            newServer = new ServerEntry(ip, clientPort, serverPort, newServerPosition, newServerPosition);
            this.servers.putIfAbsent(hashToInt(newServerPosition), newServer);
            logger.info("Add first server " + newServer.getStartIndex());
            return new Neighbors(null, null);
        } else if (this.servers.size() == 1) {
            // we only have one other server
            Map.Entry<BigInteger, ServerEntry> s = this.servers.firstEntry();

            // update predecessors endIndex
            s.getValue().setEndIndex(newServerPosition);

            // set endIndex of new server
            String newEndIndex = s.getValue().getStartIndex();

            // create new server
            newServer = new ServerEntry(ip, clientPort, serverPort, newServerPosition, newEndIndex);
            this.servers.putIfAbsent(hashToInt(newServerPosition), newServer);
            logger.info("Add server " + newServer.getStartIndex());
            return new Neighbors(s.getValue(), s.getValue());
        }

        // we have at least two servers
        Map.Entry<BigInteger, ServerEntry> smallestServer = this.servers.firstEntry(); // first
        Map.Entry<BigInteger, ServerEntry> largestServer = this.servers.lastEntry(); // last

        if (hashToInt(newServerPosition).compareTo(smallestServer.getKey()) < 0
                && hashToInt(newServerPosition).compareTo(largestServer.getKey()) < 0) {
            // new < first < last
            largestServer.getValue().setEndIndex(newServerPosition);
            String newEndIndex = intToHash(smallestServer.getKey());
            newServer = new ServerEntry(ip, clientPort, serverPort, newServerPosition, newEndIndex);
            this.servers.putIfAbsent(hashToInt(newServerPosition), newServer);
            logger.info("Add server " + newServer.getStartIndex());
            return new Neighbors(largestServer.getValue(), smallestServer.getValue());
        } else if (hashToInt(newServerPosition).compareTo(smallestServer.getKey()) > 0
                && hashToInt(newServerPosition).compareTo(largestServer.getKey()) > 0) {
            // first < last < new
            largestServer.getValue().setEndIndex(newServerPosition);
            String newEndIndex = intToHash(smallestServer.getKey());
            newServer = new ServerEntry(ip, clientPort, serverPort, newServerPosition, newEndIndex);
            this.servers.putIfAbsent(hashToInt(newServerPosition), newServer);
            logger.info("Add server " + newServer.getStartIndex());
            return new Neighbors(largestServer.getValue(), smallestServer.getValue());
        }

        // first < new < last
        ServerEntry predecessor = null;
        ServerEntry successor = null;

        for (Map.Entry<BigInteger, ServerEntry> s : this.servers.entrySet()) {
            // search for predecessors and successors and update their indices
            if (hashToInt(newServerPosition).compareTo(s.getKey()) < 0) {
                // new < s.hash
                successor = s.getValue();

                if (predecessor == null) {
                    // should not happen
                    return null;
                } else {
                    // we have a successor and predecessor, so all is fine
                    predecessor.setEndIndex(newServerPosition);
                    String newEndIndex = successor.getStartIndex();
                    newServer = new ServerEntry(ip, clientPort, serverPort, newServerPosition, newEndIndex);
                    this.servers.putIfAbsent(hashToInt(newServerPosition), newServer);
                    logger.info("Add server " + newServer.getStartIndex());
                    return new Neighbors(predecessor, successor);
                }
            } else {
                // new > s.hash
                predecessor = s.getValue();
            }
        }
        return null;
    }

    /**
     * Deletes a server out of the system.
     *
     * @param ip the ip of the server to be deleted.
     * @param clientPort the client port of the server to be deleted.
     * @return the neighbourhood of the server to be deleted.
     * (pred==succ if there are only two servers before deletion, pred==succ==null if there is only one server)
     */
    public NeighborsAndSelf deleteServer(String ip, int clientPort) {
        logger.info("Delete server:" + ip + ":" + clientPort + " metadata: " + this.toString());

        // get key for new server to be deleted
        String ipAndPort = ip + ":" + clientPort;
        String delServerPosition = generateHash(ipAndPort);
        logger.info("Trying to delete server " + ip + ":" + clientPort);
        /* --------------------------------- delete server from the circle --------------------------------- */
        if (this.servers.size() == 1) {
            // we have no servers left after deleting this one
            ServerEntry serverToBeDeleted = this.servers.firstEntry().getValue();
            if (this.servers.remove(hashToInt(delServerPosition)) == null) {
                // error if we cant delete this server
                return null;
            }
            return new NeighborsAndSelf(null, serverToBeDeleted, null);
        } else if (this.servers.size() == 2) {
            // we only have two servers
            Map.Entry<BigInteger, ServerEntry> deletedServer;
            Map.Entry<BigInteger, ServerEntry> remainingServer;

            if (this.servers.firstEntry().getKey().compareTo(hashToInt(delServerPosition)) == 0) {
                deletedServer = this.servers.firstEntry();
                remainingServer = this.servers.lastEntry();
            } else {
                deletedServer = this.servers.lastEntry();
                remainingServer = this.servers.firstEntry();
            }

            // set endIndex of remaining server
            remainingServer.getValue().setEndIndex(remainingServer.getValue().getStartIndex());

            if (this.servers.remove(hashToInt(delServerPosition)) == null) {
                // error if we cant delete this server
                return null;
            }

            return new NeighborsAndSelf(remainingServer.getValue(), deletedServer.getValue(), remainingServer.getValue());
        }

        // we have more than two servers
        ServerEntry predecessor = null;
        ServerEntry serverToBeDeleted = null;
        ServerEntry successor = null;
        Map.Entry<BigInteger, ServerEntry> smallestServer = this.servers.firstEntry(); // first
        Map.Entry<BigInteger, ServerEntry> largestServer = this.servers.lastEntry(); // last

        // edge cases
        if (hashToInt(delServerPosition).compareTo(smallestServer.getKey()) == 0) {
            // del = first
            serverToBeDeleted = smallestServer.getValue();
            this.servers.remove(hashToInt(delServerPosition));
            successor = this.servers.firstEntry().getValue();
            predecessor = this.servers.lastEntry().getValue();
            predecessor.setEndIndex(serverToBeDeleted.getEndIndex());
            return new NeighborsAndSelf(predecessor, serverToBeDeleted, successor);
        } else if (hashToInt(delServerPosition).compareTo(largestServer.getKey()) == 0) {
            // del == last
            serverToBeDeleted = largestServer.getValue();
            successor = this.servers.firstEntry().getValue();
            this.servers.remove(hashToInt(delServerPosition));
            predecessor = this.servers.lastEntry().getValue();
            predecessor.setEndIndex(serverToBeDeleted.getEndIndex());
            return new NeighborsAndSelf(predecessor, serverToBeDeleted, successor);
        }

        // general case
        for (Map.Entry<BigInteger, ServerEntry> s : this.servers.entrySet()) {
            // search for predecessors and successors and update their indices
            if (hashToInt(delServerPosition).compareTo(s.getKey()) < 0) {
                // new < s
                successor = s.getValue();
            } else if (hashToInt(delServerPosition).compareTo(s.getKey()) == 0) {
                serverToBeDeleted = s.getValue();
            } else {
                // new > s
                predecessor = s.getValue();
            }

            if (predecessor != null && serverToBeDeleted != null && successor != null) {
                this.servers.remove(hashToInt(delServerPosition));
                predecessor.setEndIndex(serverToBeDeleted.getEndIndex());
                return new NeighborsAndSelf(predecessor, serverToBeDeleted, successor);
            }
        }

        return null;
    }

    /**
     * Similar to deleteServer, without deleting the server but only returning its neighbors.
     *
     * @param ip the ip of the server.
     * @param clientPort the client port of the server.
     * @return the neighbourhood of the server.
     * (pred==succ if there are only two servers, pred==succ==null if there is only one server)
     */
    public NeighborsAndSelf getNeighbors(String ip, int clientPort) {
        logger.severe("Get neighbors:" + ip + ":" + clientPort + " metadata: " + this.toString());
        // get key for new server to be deleted
        String ipAndPort = ip + ":" + clientPort;
        String delServerPosition = generateHash(ipAndPort);
        logger.info("We search for: " + ip + ":" + clientPort);
        /* --------------------------------- delete server from the circle --------------------------------- */
        if (this.servers.size() == 1) {
            // we have no servers left after deleting this one
            ServerEntry serverToBeDeleted = this.servers.firstEntry().getValue();
            return new NeighborsAndSelf(null, serverToBeDeleted, null);
        } else if (this.servers.size() == 2) {
            // we only have two servers
            Map.Entry<BigInteger, ServerEntry> deletedServer;
            Map.Entry<BigInteger, ServerEntry> remainingServer;

            if (this.servers.firstEntry().getKey().compareTo(hashToInt(delServerPosition)) == 0) {
                deletedServer = this.servers.firstEntry();
                remainingServer = this.servers.lastEntry();
            } else {
                deletedServer = this.servers.lastEntry();
                remainingServer = this.servers.firstEntry();
            }

            // set endIndex of remaining server
            //remainingServer.getValue().setEndIndex(remainingServer.getValue().getStartIndex());

            return new NeighborsAndSelf(remainingServer.getValue(), deletedServer.getValue(), remainingServer.getValue());
        }

        // we have more than two servers
        ServerEntry predecessor = null;
        ServerEntry serverToBeDeleted = null;
        ServerEntry successor = null;
        Map.Entry<BigInteger, ServerEntry> smallestServer = this.servers.firstEntry(); // first
        Map.Entry<BigInteger, ServerEntry> largestServer = this.servers.lastEntry(); // last

        // edge cases
        if (hashToInt(delServerPosition).compareTo(smallestServer.getKey()) == 0) {
            // del = first
            serverToBeDeleted = smallestServer.getValue();
            successor = getServerEntryAtIndex(1);
            predecessor = this.servers.lastEntry().getValue();
            //predecessor.setEndIndex(serverToBeDeleted.getEndIndex());
            return new NeighborsAndSelf(predecessor, serverToBeDeleted, successor);
        } else if (hashToInt(delServerPosition).compareTo(largestServer.getKey()) == 0) {
            // del == last
            serverToBeDeleted = largestServer.getValue();
            successor = this.servers.firstEntry().getValue();
            predecessor = getServerEntryAtIndex(this.servers.size()-2);
            //predecessor.setEndIndex(serverToBeDeleted.getEndIndex());
            return new NeighborsAndSelf(predecessor, serverToBeDeleted, successor);
        }

        // general case
        for (Map.Entry<BigInteger, ServerEntry> s : this.servers.entrySet()) {
            // search for predecessors and successors and update their indices
            if (hashToInt(delServerPosition).compareTo(s.getKey()) < 0) {
                // new < s
                successor = s.getValue();
            } else if (hashToInt(delServerPosition).compareTo(s.getKey()) == 0) {
                serverToBeDeleted = s.getValue();
            } else {
                // new > s
                predecessor = s.getValue();
            }

            if (predecessor != null && serverToBeDeleted != null && successor != null) {
                //predecessor.setEndIndex(serverToBeDeleted.getEndIndex());
                return new NeighborsAndSelf(predecessor, serverToBeDeleted, successor);
            }
        }

        return null;
    }

    /**
     * Turns the current Metadata into a string which is useful for the servers to initialize the Metadata on their side.
     *
     * @return <startIndex1>,<endIndex1>,<ip1>:<clientPort1>:<serverPort1>;<startIndex2>...
     */
    public String toString() {
        StringBuilder dataStr = new StringBuilder();

        for (Map.Entry<BigInteger, ServerEntry> server : this.servers.entrySet()) {
            dataStr.append(server.getValue().getStartIndex());
            dataStr.append(",");
            dataStr.append(server.getValue().getEndIndex());
            dataStr.append(",");
            dataStr.append(server.getValue().getIp());
            dataStr.append(":");
            dataStr.append(server.getValue().getClientPort());
            dataStr.append(":");
            dataStr.append(server.getValue().getServerPort());
            dataStr.append(";");
        }

        if (dataStr.length() > 0) {
            dataStr.deleteCharAt(dataStr.length()-1);  // delete trailing ";"
        }

        return dataStr.toString();
    }

    /**
     * Turns the current Metadata into a string which is useful for the clients.
     *
     * @return <startIndex1>,<endIndex1>,<ip1>:<clientPort1>;<startIndex2>...
     */
    public String toClientString() {
        StringBuilder dataStr = new StringBuilder();

        for (Map.Entry<BigInteger, ServerEntry> server : this.servers.entrySet()) {
            dataStr.append(server.getValue().getStartIndex());
            dataStr.append(",");
            dataStr.append(server.getValue().getEndIndex());
            dataStr.append(",");
            dataStr.append(server.getValue().getIp());
            dataStr.append(":");
            dataStr.append(server.getValue().getClientPort());
            dataStr.append(";");
        }

        if (dataStr.length() > 0) {
            dataStr.deleteCharAt(dataStr.length()-1);  // delete trailing ";"
        }

        return dataStr.toString();
    }

    public String keyrangeRead() {
        StringBuilder dataStr = new StringBuilder();

        for (Map.Entry<BigInteger, ServerEntry> server : this.servers.entrySet()) {
            String address = server.getValue().getIp();
            int port = server.getValue().getClientPort();
            NeighborsAndSelf serverAndReplicas = this.getCandidateReplicas(address, port);
            dataStr.append(this.serverAndReplicasToString(serverAndReplicas));
        }

        if (dataStr.length() > 0) {
            dataStr.deleteCharAt(dataStr.length()-1);  // delete trailing ";"
        }

        return dataStr.toString();
    }

    private String serverAndReplicasToString(NeighborsAndSelf serverAndReplicas) {

        StringBuilder dataStr = new StringBuilder();
        dataStr.append(serverAndReplicas.getSelf().getStartIndex());
        dataStr.append(",");
        dataStr.append(serverAndReplicas.getSelf().getEndIndex());
        dataStr.append(",");
        dataStr.append(serverAndReplicas.getSelf().getIp());
        dataStr.append(":");
        dataStr.append(serverAndReplicas.getSelf().getClientPort());
        dataStr.append(";");

        if(serverAndReplicas.getSuccessor()!=null){
            dataStr.append(serverAndReplicas.getSelf().getStartIndex());
            dataStr.append(",");
            dataStr.append(serverAndReplicas.getSelf().getEndIndex());
            dataStr.append(",");
            dataStr.append(serverAndReplicas.getSuccessor().getIp());
            dataStr.append(":");
            dataStr.append(serverAndReplicas.getSuccessor().getClientPort());
            dataStr.append(";");
        }
        if(serverAndReplicas.getPredecessor()!=null){
            dataStr.append(serverAndReplicas.getSelf().getStartIndex());
            dataStr.append(",");
            dataStr.append(serverAndReplicas.getSelf().getEndIndex());
            dataStr.append(",");
            dataStr.append(serverAndReplicas.getPredecessor().getIp());
            dataStr.append(":");
            dataStr.append(serverAndReplicas.getPredecessor().getClientPort());
            dataStr.append(";");  
        }
        return dataStr.toString();

    }

    /**
     * Generates a MD5 hash of the provided key.
     *
     * @param key for servers: <ip>:<port> and for clients: key
     * @return the hash as a 32 bit String.
     */
    public static String generateHash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            byte[] digest = md.digest();

            StringBuilder hexString = new StringBuilder();

            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a provided String is in the range of the provided hashes.
     *
     * @param key the key in question.
     * @param start the start hash.
     * @param end the end hash.
     * @return true if the key is in the range and false if not.
     */
    public static boolean hashInRange(String key, String start, String end){
        BigInteger keyhash = new BigInteger(generateHash(key), 16);
        BigInteger st = new BigInteger(start, 16);
        BigInteger en = new BigInteger(end, 16);
        
        if (st.compareTo(en) == 0) {
            // if there is only one server
            return true;
        }

        boolean result= false; 

        if (st.compareTo(en) < 0) {
            // start < end
            result= (keyhash.compareTo(st)>=0) && (keyhash.compareTo(en)<0); 
        } else {
            // start > end
            result = (! ((keyhash.compareTo(st)<0) && (keyhash.compareTo(en)>=0)) );
        }
        logger.fine("The key hash  for the key " + key + " is : " + keyhash);
        logger.fine("The start hash is: " + st );
        logger.fine("The end hash is: " + en );
        logger.fine("The result for this combination is: " + result);
        return result;
    }

    /**
     * Converts a hash to an integer.
     *
     * @param hash the hash value as a String.
     * @return the integer as a BigInteger.
     */
    private BigInteger hashToInt(String hash) {
        return new BigInteger(hash, 16);
    }

    /**
     * Converts an integer to a 32 bit MD5 hash.
     *
     * @param bigInteger the integer as a BigInteger.
     * @return the hash value as a String.
     */
    private String intToHash(BigInteger bigInteger) {
        return bigInteger.toString(16);
    }

    /**
     * Search for a server with the provided ip and port.
     *
     * @param ipAndPort the server information.
     * @return the ServerEntry if it exists and null if not.
     */
    public ServerEntry getServerEntry(String ipAndPort){
        BigInteger serverhash = hashToInt(generateHash(ipAndPort));
        return this.servers.get(serverhash);
    }

    /**
     * Returns a server at a particular index in the hash ring (servers sorted by hash).
     *
     * @param index the index.
     * @return the required ServerEntry.
     */
    public ServerEntry getServerEntryAtIndex(int index) {
        int i = 0;
        for (Map.Entry<BigInteger, ServerEntry> server : this.servers.entrySet()) {
            if (i==index) {
                return server.getValue();
            } else {
                i++;
            }
        }
        return null;
    }

    /**
     * Computes the heartbeat response times of all servers.
     */
    public void getServerResponseTimes() {
        for (Map.Entry<BigInteger, ServerEntry> server : this.servers.entrySet()) {
            Instant start = server.getValue().getStartTime();
            Instant end = Instant.now();
            Duration timeElapsed = Duration.between(start, end);
            logger.info("Server " + server.getValue().getIp() + ":" + server.getValue().getClientPort() + " " + timeElapsed.toMillis() + "ms");
        }
    }

    /**
     * returns the replicas of a server
     */
    public NeighborsAndSelf getCandidateReplicas(String ip, int clientPort) {
        logger.severe("Get the replicas:" + ip + ":" + clientPort);

        if (this.servers.size() == 3) {
            // if there are only 3 servers, then the successor is the first replica and the successor is the second one.
            NeighborsAndSelf replicas = this.getNeighbors(ip, clientPort);
            logger.info("Replicas are: " +  this.serverAndReplicasToString(replicas));
            return replicas;
        } else {
            /*
             *We have more than 3 servers, so the first replica is the successor 
             and the second replica is the successor of the the first replica
             */
            NeighborsAndSelf replicas = this.getNeighbors(ip, clientPort);
            ServerEntry firstReplica = replicas.getSuccessor();
            String replicaIp = firstReplica.getIp();
            int replicaPort =  firstReplica.getClientPort();
            ServerEntry secondReplica = this.getNeighbors(replicaIp, replicaPort).getSuccessor();
            NeighborsAndSelf reps = new NeighborsAndSelf(secondReplica, replicas.getSelf(), firstReplica);
            logger.info("Replicas are: " +  this.serverAndReplicasToString(reps));
            return reps;
        }
    }

    public int numberOfServers(){
        return this.servers.size();
    }
}
