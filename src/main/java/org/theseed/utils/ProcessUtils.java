/**
 *
 */
package org.theseed.utils;

import java.util.List;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

/**
 * This is a utility class for managing external processer.
 *
 * @author Bruce Parrello
 *
 */
public class ProcessUtils {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ProcessUtils.class);

    /**
     * Wait for a process to finish.  If it has a nonzero exit code, log the error output.
     *
     * @param name			name of the process
     * @param process		process to finish
     * @param logMessages	log messages to echo
     * @throws InterruptedException
     */
    public static int finishProcess(String name, Process process, List<String> logMessages) throws InterruptedException {
        int retVal = process.waitFor();
        if (retVal != 0) {
            // We have an error. Output the error log.
            log.error("Output from {} error log follows.", name);
            for (String message : logMessages)
                log.error("   {}", message);
        }
        return retVal;
    }


}
