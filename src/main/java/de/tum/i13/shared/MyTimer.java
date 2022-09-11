package de.tum.i13.shared;

import java.io.Serializable;
import java.util.Timer;

/**
 * Necessary proxy to make the Timer serializable (needed for making it transferable via the ObjectStream).
 */
public class MyTimer extends Timer implements Serializable {

    public MyTimer() {
        super();
    }
}
