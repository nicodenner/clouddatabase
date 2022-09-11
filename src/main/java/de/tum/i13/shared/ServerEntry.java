package de.tum.i13.shared;

import java.io.Serializable;
import java.time.Instant;

/**
 * Describes a server in the system, which is managed by the ECS and stored as an Entity in the Metadata.
 */
public class ServerEntry implements Serializable {
    private String ip;
    private int clientPort;
    private int serverPort;
    private String startIndex; // is the position of the server, so the hash(<ip>:<port>)
    private String endIndex;
    private boolean writeLockEnabled;
    private MyTimer timer;
    private boolean isLeaving;
    private Instant start;

    public ServerEntry(String ip, int clientPort,int serverPort, String startIndex, String endIndex) {
        this.ip = ip;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.writeLockEnabled = false;
        this.isLeaving = false;
        this.start = Instant.now();
    }

    public ServerEntry(String entry){
        String[] data = entry.split(",");
        this.ip = data[2].split(":")[0];
        this.clientPort = Integer.parseInt(data[2].split(":")[1]);
        this.serverPort = Integer.parseInt(data[2].split(":")[2]);
        this.startIndex = data[0];
        this.endIndex = data[1];
    }

    public ServerEntry(boolean isClient, String entry){
        if (isClient) {
            String[] data = entry.split(",");
            this.ip = data[2].split(":")[0];
            this.clientPort = Integer.parseInt(data[2].split(":")[1]);
            this.serverPort = -1;
            this.startIndex = data[0];
            this.endIndex = data[1];
        } else {
            String[] data = entry.split(",");
            this.ip = data[2].split(":")[0];
            this.clientPort = Integer.parseInt(data[2].split(":")[1]);
            this.serverPort = Integer.parseInt(data[2].split(":")[2]);
            this.startIndex = data[0];
            this.endIndex = data[1];
        }
    }

    public String getIp() {
        return ip;
    }

    public int getClientPort() {
        return clientPort;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getStartIndex() {
        return startIndex;
    }

    public String getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(String endIndex) {
        this.endIndex = endIndex;
    }

    public boolean isWriteLockEnabled() {
        return this.writeLockEnabled;
    }

    public void setWriteLockEnabled(boolean bool) {
        this.writeLockEnabled = bool;
    }

    public void setTimer(MyTimer timer) {
        this.timer = timer;
    }

    public MyTimer getTimer() {
        return this.timer;
    }

    public void setIsLeaving(boolean b) {
        this.isLeaving = b;
    }

    public boolean isLeaving() {
        return this.isLeaving;
    }

    public void updateStartTime(Instant start) {
        this.start = start;
    }

    public Instant getStartTime() {
        return this.start;
    }
}
