package de.tum.i13.client;

import java.util.Random;

public class BackoffService {
    public static int defaultRetries=2;
    public static long defaultWaitTimeInMills=10000;
    private final int numberOfRetries;
    private final long defaultTimeToWait;
    private final Random random = new Random();
    private long timeToWait;
    private int numberOfTriesLeft;

    public BackoffService() {
        this(defaultRetries, defaultWaitTimeInMills);
    }

    public BackoffService(int numberOfRetries, long defaultTimeToWait){
        this.numberOfRetries = numberOfRetries;
        this.numberOfTriesLeft = numberOfRetries;
        this.defaultTimeToWait = defaultTimeToWait;
        this.timeToWait = defaultTimeToWait;
    }

    /**
     * Indicates if a retry should be executed or not according to the number of retries left.
     *
     * @return true if a retry should be performed, false if not.
     */
    public boolean shouldRetry() {
        return numberOfTriesLeft > 0;
    }

    /**
     * Responsible for handling the case where a try was unsuccessful.
     */
    public void errorOccured() {
        numberOfTriesLeft--;
        if (!shouldRetry()) {
            // retry failed
        }
        waitUntilNextTry();
        timeToWait += random.nextInt(1000);
    }

    /**
     * Provides the defined time to wait during tries.
     *
     * @return the time to wait.
     */
    public long getTimeToWait() {
        return this.timeToWait;
    }

    /**
     * No retry needed since the last one was successful.
     * Therefore reset the number of tries left so that shouldRetry() exits the loop.
     */
    public void doNotRetry() {
        numberOfTriesLeft = 0;
    }

    /**
     * Resets all parameters.
     */
    public void reset() {
        this.numberOfTriesLeft = numberOfRetries;
        this.timeToWait = defaultTimeToWait;
    }

    /**
     * Waits for a predefined amount of time.
     */
    private void waitUntilNextTry() {
        try {
            Thread.sleep(timeToWait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
