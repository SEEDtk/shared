/**
 *
 */
package org.theseed.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.theseed.io.LineReader;

/**
 * This is a utility class for managing external processer.
 *
 * @author Bruce Parrello
 *
 */
public class ProcessUtils {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(ProcessUtils.class);

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
     * @return the exit code from the process
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static int runProgram(File program, File outFile, List<String> parms) throws IOException, InterruptedException {
        String[] command = buildCommand(program, parms);
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
     * @return the command to run in array form
     *
     * @param program	command program to run
     * @param parms		list of parameters
     */
    protected static String[] buildCommand(File program, List<String> parms) {
        // Define the command to run.
        String[] retVal = new String[parms.size() + 1];
        retVal[0] = program.getAbsolutePath();
        for (int i = 0; i < parms.size(); i++)
            retVal[i+1] = parms.get(i);
        return retVal;
    }

    /**
     * Run a sub-command.  Standard output is gathered into a list, and error output is echoed to our own error stream.
     *
     * @param program	program to run
     * @param parms		list of parameters
     *
     * @return the output from the program as a list of strings, or NULL if the program failed
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<String> runProgram(File program, List<String> parms) throws IOException, InterruptedException {
        String[] command = buildCommand(program, parms);
        // Connect to the appropriate error pipe.
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        // Start the process.
        Process process = builder.start();
        // Read the process output.
        List<String> retVal = new ArrayList<String>();
        try (LineReader reader = new LineReader(process.getInputStream())) {
            for (String line : reader)
                retVal.add(line);
            // Wait for the process to exit.
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Command {} failed with exit code {}.", program, exitCode);
                retVal = null;
            }
        }
        return retVal;
    }

}
