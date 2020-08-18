/**
 *
 */
package org.theseed.io;

import java.util.List;

/**
 * This is a simple class that sets up a thread to intercept error output from
 * an external process.  The client passes in a message buffer to receive the
 * errors.
 *
 * @author Bruce Parrello
 *
 */
public class ErrorQueue extends Thread {

    // FIELDS
    private LineReader errorStream;
    private List<String> errorMessages;

    public ErrorQueue(LineReader errorStream, List<String> messageBuffer) {
        this.errorMessages = messageBuffer;
        this.errorStream = errorStream;
    }

    @Override
    public void run() {
        for (String line : this.errorStream)
            errorMessages.add(line);
    }

}


