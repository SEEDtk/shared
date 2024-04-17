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
import org.theseed.io.TabbedLineReader;

/**
 * This is a subclass of the base reporting processor that handles an input tab-delimited
 * file and an output report, an extremely common case.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BasePipeProcessor extends BaseReportProcessor {

    // FIELDS
    /** input tab-delimited file */
    private TabbedLineReader inStream;

    // COMMAND-LINE OPTIONS

    /** input file (if not STDIN) */
    @Option(name = "--input", aliases = { "-i" }, metaVar = "inFile.tbl", usage = "input file (if not STDIN)")
    private File inFile;

    @Override
    protected final void setReporterDefaults() {
        this.inStream = null;
        this.inFile = null;
        this.setPipeDefaults();
    }

    /**
     * Initialize the defaults for the subclass.
     */
    protected abstract void setPipeDefaults();

    @Override
    protected final void validateReporterParms() throws IOException, ParseFailureException {
        // Set up the parameters for the subclass.
        this.validatePipeParms();
        try {
            // Validate the input file.
            if (this.inFile == null) {
                log.info("Input will be read from the standard input.");
                this.inStream = new TabbedLineReader(System.in);
            } else if (! this.inFile.canRead())
                throw new FileNotFoundException("Input file " + this.inFile + " is not found or unreadable.");
            else {
                log.info("Input will be read from {}.", this.inFile);
                this.inStream = new TabbedLineReader(this.inFile);
            }
            // Here the subclass can query fields and do initial reading.
            this.validatePipeInput(this.inStream);
        } catch (Exception e) {
            // An error occurred: close the input stream and continue the exception.
            if (this.inStream != null)
                this.inStream.close();
            throw e;
        }

    }

    /**
     * Process key information in the input stream.  This is always called after "validatePipeParms".
     *
     * @param inputStream	open input stream
     *
     * @throws IOException
     */
    protected abstract void validatePipeInput(TabbedLineReader inputStream) throws IOException;

    /**
     * Perform parameter validation for the subclass.  This is always called before "validatePipeInput".
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    protected abstract void validatePipeParms() throws IOException, ParseFailureException;

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
     * @param inputStream	tab-delimited input stream
     * @param writer		output print writer
     *
     * @throws Exception
     */
    protected abstract void runPipeline(TabbedLineReader inputStream, PrintWriter writer)
            throws Exception;

}
