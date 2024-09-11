/**
 *
 */
package org.theseed.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.kohsuke.args4j.Option;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.LineReader;

/**
 * This is a subclass of the base reporting processor that handles an input text
 * file and an output report, similar to the pipe processor (which handles tab-delimited
 * input instead of line-by-line input).
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseTextProcessor extends BaseReportProcessor {

    // FIELDS
    /** input tab-delimited file */
    private LineReader inStream;

    // COMMAND-LINE OPTIONS

    /** input file (if not STDIN) */
    @Option(name = "--input", aliases = { "-i" }, metaVar = "inFile.tbl", usage = "input file (if not STDIN)")
    private File inFile;

    @Override
    protected final void setReporterDefaults() {
        this.inStream = null;
        this.inFile = null;
        this.setTextDefaults();
    }

    /**
     * Initialize the defaults for the subclass.
     */
    protected abstract void setTextDefaults();

    @Override
    protected final void validateReporterParms() throws IOException, ParseFailureException {
        // Set up the parameters for the subclass.
        this.validateTextParms();
        try {
            // Validate the input file.
            if (this.inFile == null) {
                log.info("Input will be read from the standard input.");
                this.inStream = new LineReader(System.in);
            } else if (! this.inFile.canRead())
                throw new FileNotFoundException("Input file " + this.inFile + " is not found or unreadable.");
            else {
                log.info("Input will be read from {}.", this.inFile);
                this.inStream = new LineReader(this.inFile);
            }
        } catch (Exception e) {
            // An error occurred: close the input stream and continue the exception.
            if (this.inStream != null)
                this.inStream.close();
            throw e;
        }

    }

    /**
     * Perform parameter validation for the subclass.
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    protected abstract void validateTextParms() throws IOException, ParseFailureException;

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        try {
            this.runPipeline(this.inStream, writer);
        } finally {
            this.inStream.close();
        }
    }

    /**
     * Process the input to produce the output.
     *
     * @param inputStream	text-line input stream
     * @param writer		output print writer
     *
     * @throws Exception
     */
    protected abstract void runPipeline(LineReader inputStream, PrintWriter writer)
            throws Exception;

}
