/**
 *
 */
package org.theseed.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a sub-command base class that writes multiple output files to a directory.  Support
 * is provided for creating, validating, and clearing the output directory, as well as opening
 * output files.  The output directory is specified using the "-D" parameter.  The client chooses
 * this directory via the "setDefaultOutputDir" method.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -D	output directory name
 *
 * --clear	erase the output directory before processing
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseMultiReportProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(BaseMultiReportProcessor.class);

    /** output directory name */
    @Option(name = "--outDir", aliases = { "-D" }, metaVar = "outDir", usage = "output directory name")
    private File outDir;

    /** if specified, the output directory will be erased before processing */
    @Option(name = "--clear", usage = "if specified, the output directory will be erased before processing")
    private boolean clearFlag;

    @Override
    protected void setDefaults() {
        this.clearFlag = false;
        // As a convenience, we pass the absolute path of the current directory to the output-directory
        // default check.
        File curDir = new File(".").getAbsoluteFile();
        this.outDir = this.setDefaultOutputDir(curDir);
        // Let the client set its own defaults.
        this.setMultiReportDefaults();
    }

    /**
     * Here the client specifies the default output directory.
     *
     * @param curDir	name of the current user directory
     *
     * @return the default directory to use for output
     */
    protected abstract File setDefaultOutputDir(File curDir);

    /**
     * Set the default values for the options.
     */
    protected abstract void setMultiReportDefaults();


    @Override
    protected boolean validateParms() throws IOException, ParseFailureException {
        // Let the client validate the parameters.
        this.validateMultiReportParms();
        // Set up the output directory.
        if (this.outDir.isFile())
            throw new FileNotFoundException("Output directory " + this.outDir + " is a file.");
        else if (! this.outDir.isDirectory()) {
            // Here we must create the directory.
            log.info("Creating output directory {}.", this.outDir);
            FileUtils.forceMkdir(this.outDir);
        } else if (this.clearFlag) {
            // Here the directory exists, but the user wants to clear out the old files.
            log.info("Erasing output directory {}.", this.outDir);
            FileUtils.cleanDirectory(this.outDir);
        } else
            log.info("Output files will be created in directory {}.", this.outDir);
        return true;
    }

    /**
     * Validate the options and parameters for this sub-command.
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    protected abstract void validateMultiReportParms() throws IOException, ParseFailureException;

    @Override
    protected void runCommand() throws Exception {
        this.runMultiReports();

    }

    /**
     * Process the input to produce the output files.
     *
     * @throws Exception
     */
    protected abstract void runMultiReports() throws Exception;

    /**
     * Open a new output file with the specified name.
     *
     * @param fileName	base name of output file
     *
     * @return a print writer for the file
     *
     * @throws IOException
     */
    protected PrintWriter openReport(String fileName) throws IOException {
        PrintWriter retVal = new PrintWriter(new File(this.outDir, fileName));
        return retVal;
    }

    /**
     * Create the name of a file in the output directory.
     *
     * @param name	base name of proposed output file or directory
     *
     * @return a usable name of the proposed output file or directory
     */
    protected File getOutFile(String name) {
        return new File(this.outDir, name);
    }

}
