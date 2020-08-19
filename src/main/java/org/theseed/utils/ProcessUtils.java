/**
 *
 */
package org.theseed.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

    /**
     * Run a sub-command.  Standard output is to a file, and error output is echoed to our own error stream.
     *
     * @param program	program to run
     * @param outFile	file to contain the standard output
     * @param parms		list of parameters
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public int runCommand(File program, File outFile, List<String> parms) throws IOException, InterruptedException {
        // Define the command to run.
        String[] command = new String[parms.size() + 1];
        command[0] = program.getAbsolutePath();
        for (int i = 0; i < parms.size(); i++)
            command[i+1] = parms.get(i);
        // Connect to the appropriate error and output pipes.
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(outFile);
        // Start the process and wait for it to finish.
        log.info("Starting {}.", StringUtils.join(command, ' '));
        Process process = builder.start();
        // Wait for the process to finish.
        int retVal = process.waitFor();
        log.info("{} processor returned {}.", program.getName(), retVal);
        return retVal;
    }


    /**
     * Return the file location of an executable in the path.
     */

}
