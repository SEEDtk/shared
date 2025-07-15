/**
 *
 */
package org.theseed.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;

/**
 * This is a variant of the basic command processor that reads a tab-delimited file on the standard input.
 * It allows a file name to be specified for the input if necessary.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseInputProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(BaseInputProcessor.class);
    /** input stream */
    private TabbedLineReader inStream;

    // COMMAND-LINE OPTIONS

    /** input file (if not STDIN) */
    @Option(name = "--input", aliases = { "-i" }, usage = "input file (if not STDIN)")
    private File inFile;

    @Override
    protected final void setDefaults() {
        this.inFile = null;
        this.setReaderDefaults();
    }

    /**
     * Set the default values of command-line options.
     */
    protected abstract void setReaderDefaults();

    @Override
    protected final void validateParms() throws IOException, ParseFailureException {
        this.validateReaderParms();
        if (this.inFile == null) {
            log.info("Input will be read from the standard input.");
            this.inStream = new TabbedLineReader(System.in);
        } else if (! this.inFile.canRead())
            throw new FileNotFoundException("Input file " + this.inFile + " is not found or unreadable.");
        else {
            log.info("Input will be read from {}.", this.inFile);
            this.inStream = new TabbedLineReader(this.inFile);
        }
        try {
            this.validateReaderInput(this.inStream);
        } catch (IOException e) {
            this.inStream.close();
            throw new IOException(e);
        }
    }

    /**
     * Validate the command-line options and parameters.  This is called BEFORE validateReaderInput.
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    protected abstract void validateReaderParms() throws IOException, ParseFailureException;

    /**
     * Validate the input stream.  This is called AFTER validateReaderParms, and allows the client to verify
     * the fields present in the input.
     *
     * @param reader	input stream
     *
     * @throws IOException
     */
    protected abstract void validateReaderInput(TabbedLineReader reader) throws IOException;


    @Override
    protected final void runCommand() throws Exception {
        try {
            this.runReader(this.inStream);
        } finally {
            // Insure the output file is closed.
            this.inStream.close();
        }
    }

    /**
     * Execute the command and produce the report.
     *
     *  @param writer	print writer to receive the report
     *
     *  @throws Exception
     */
    protected abstract void runReader(TabbedLineReader reader) throws Exception;

}
