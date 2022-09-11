package de.tum.i13.shared;

/**
 * Models a neighborhood in the Metadata hash ring including the predecessor and successor of a particular server.
 */
public class Neighbors {
    private ServerEntry predecessor;
    private ServerEntry successor;

    public Neighbors(ServerEntry predecessor, ServerEntry successor) {
        this.predecessor = predecessor;
        this.successor = successor;
    }

    public ServerEntry getPredecessor() {
        return this.predecessor;
    }

    public ServerEntry getSuccessor() {
        return this.successor;
    }
}
