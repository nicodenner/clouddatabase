package de.tum.i13.shared;

/**
 * Models a neighborhood in the Metadata hash ring including itself, the predecessor and the successor of a particular server.
 */
public class NeighborsAndSelf {
    private ServerEntry predecessor;
    private ServerEntry self;
    private ServerEntry successor;

    public NeighborsAndSelf(ServerEntry predecessor, ServerEntry self, ServerEntry successor) {
        this.predecessor = predecessor;
        this.self = self;
        this.successor = successor;
    }

    public ServerEntry getPredecessor() {
        return this.predecessor;
    }

    public ServerEntry getSelf() {
        return this.self;
    }

    public ServerEntry getSuccessor() {
        return this.successor;
    }
}
