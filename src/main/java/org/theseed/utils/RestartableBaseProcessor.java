/**
 *
 */
package org.theseed.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.TabbedLineReader;

/**
 * This is a base class for restartable processes.  It is presumed that the output file is tab-delimited with headers,
 * and one of the columns contains item IDs.
 *
 * The "--resume" option specifies the old output file for resuming.
 *
 * Call "isProcessed" to find out of an item is already processed.
 *
 * Call "markProcessed" for each item processed.
 *
 * Call "println" or "format" to write to the output stream.
 *
 * Call "setup" to set up the output stream.
 *
 * @author Bruce Parrello
 *
 */
public abstract class RestartableBaseProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(RestartableBaseProcessor.class);
    /** output stream */
    private PrintStream output;
    /** set of IDs for items already processed */
    private Set<String> processedItems;

    // COMMAND-LINE OPTIONS

    /** old output file if we are resuming */
    @Option(name = "--resume", metaVar = "output.log", usage = "if we are resuming, the output file from the interrupted run")
    private File resumeFile;

    /**
     * Construct a restartable processor.
     */
    public RestartableBaseProcessor() {
        this.resumeFile = null;
    }

    /**
     * Check for a resume situation. (This should be called during validation.)
     *
     * @param header 	heading line for output file
     * @param idCol		name of ID column
     *
     * @return the number of items already processed
     *
     * @throws IOException
     */
    protected int setup(String header, String idCol) throws IOException {
        int retVal = 0;
        this.processedItems = new HashSet<String>();
        // Check for a resume situation.
        if (this.resumeFile == null) {
            // Normal processing.  Put the log to the standard output.
            System.out.println(header);
            this.output = System.out;
        } else if (! this.resumeFile.exists()) {
            // Here the resume file is a new file.  Create it and put the header in it.
            FileOutputStream outStream = new FileOutputStream(this.resumeFile);
            this.output = new PrintStream(outStream, true);
            this.output.println(header);
        } else {
            // Resume processing.  Save the roles we've already seen.
            try (TabbedLineReader reader = new TabbedLineReader(this.resumeFile)) {
                int idColIdx = reader.findField(idCol);
                for (TabbedLineReader.Line line : reader) {
                    this.processedItems.add(line.get(idColIdx));
                }
            }
            // Open the resume file for append-style output with autoflush.
            FileOutputStream outStream = new FileOutputStream(this.resumeFile, true);
            this.output = new PrintStream(outStream, true);
            // Count the number of items already processed.
            retVal = this.processedItems.size();
        }
        return retVal;
    }

    /**
     * @return TRUE if the specified item has already been processed
     */
    protected boolean isProcessed(String itemId) {
        boolean retVal = false;
        if (this.processedItems.contains(itemId)) {
            log.info("Skipping {}:  already processed.", itemId);
            retVal = true;
        }
        return retVal;
    }

    /**
     * Denote that an item has been processed.
     *
     * @param itemId	ID of the processed item
     *
     * @return the total number of items processed so far
     */
    protected int markProcessed(String itemId) {
        this.processedItems.add(itemId);
        return this.processedItems.size();
    }

    /**
     * Write a line of formatted output.
     *
     * @param format	output format string
     * @param args		arguments
     */
    public void format(String format, Object... args) {
        this.output.format(format, args);
    }

    /**
     * Write a string as a line of output.
     *
     * @param line		text to write
     */
    public void println(String line) {
        this.output.println(line);
    }

    /**
     * Remove the processed items from a collection.
     *
     * @param itemSet	collection to update
     */
    public void removeProcessed(Collection<String> itemSet) {
        itemSet.removeAll(this.processedItems);
    }

    /**
     * Close the output stream.
     */
    public void closeOutput() {
        this.output.close();
    }

}
