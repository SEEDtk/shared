/**
 *
 */
package org.theseed.reports;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.utils.ParseFailureException;

/**
 * This object produces input data for a machine learning program.  It supports DL4J classifier, DL4J random
 * forest, DL4J regression, and python CSV.  For the DL4J formats, the output is a directory.  For the
 * other formats, the output is a file.
 *
 * @author Bruce Parrello
 *
 */
public abstract class XMatrixReporter implements AutoCloseable {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(XMatrixReporter.class);
    /** controlling command processor */
    private IParms command;
    /** output directory or file */
    private File outLoc;
    /** full array of header column names */
    private String[] headers;
    /** main data file output writer */
    private PrintWriter writer;
    /** data line output buffer */
    private StringBuffer buffer;
    /** delimiter to use for output rows in the main file */
    private char delim;

    /**
     * This interface defines the parameters a command processor needs to support in order to generate
     * XMatrix output.
     */
    public static interface IParms {

        /**
         * @return TRUE if the output directory should be erased before processing
         */
        public boolean getClearFlag();

        /**
         * @return the name of the negative label for DL4J classifiers and random-forest
         */
        public String getNegLabel();

    }

    /**
     * This enum defines the types of XMatrix output.
     */
    public static enum Type {
        /** DL4J classification directory */
        CLASSIFIER {
            @Override
            public XMatrixReporter create(IParms processor, File outDir) throws ParseFailureException, IOException {
                return new ClassifierXMatrixReporter(processor, outDir);
            }
        },
        /** DL4J regression directory */
        REGRESSION {
            @Override
            public XMatrixReporter create(IParms processor, File outDir) throws ParseFailureException, IOException {
                return new RegressionXMatrixReporter(processor, outDir);
            }
        },
        /** DL4J random-forest directory */
        RANDOM_FOREST {
            @Override
            public XMatrixReporter create(IParms processor, File outDir) throws ParseFailureException, IOException {
                return new RandomForestXMatrixReporter(processor, outDir);
            }
        },
        /** comma-separated value file */
        CSV {
            @Override
            public XMatrixReporter create(IParms processor, File outDir) throws ParseFailureException, IOException {
                return new CsvXMatrixReporter(processor, outDir);
            }
        };

        /**
         * Create a new XMatrix report object.
         *
         * @param processor		controlling command processor
         * @param outDir		output directory or file
         *
         * @return an XMatrix reporting object of the appropriate type
         *
         * @throws ParseFailureException
         * @throws IOException
         */
        public abstract XMatrixReporter create(IParms processor, File outDir) throws ParseFailureException, IOException;

    }

    /**
     * Construct a new XMatrix reporting object.
     *
     * @param processor		controlling command processor
     * @param outDir		output directory or file
     */
    public XMatrixReporter(IParms processor, File outDir) throws ParseFailureException, IOException {
        this.command = processor;
        this.outLoc = outDir;
        this.writer = null;
        this.delim = this.getDelim();
    }

    /**
     * @return the output directory for this report
     *
     * @throws IOException
     */
    protected File getOutDir() throws IOException {
        if (! this.outLoc.isDirectory()) {
            log.info("Creating output directory {}.", this.outLoc);
            FileUtils.forceMkdir(this.outLoc);
        } else if (this.command.getClearFlag()) {
            log.info("Erasing output directory {}.", this.outLoc);
            FileUtils.cleanDirectory(this.outLoc);
        } else
            log.info("Output will be created in directory {}.", this.outLoc);
        // Set up the main output file.
        this.openOutFile();
        // Return the directory.
        return this.outLoc;
    }

    /**
     * Set up the output writer for the main output file of this report
     *
     * @throws IOException
     */
    protected void openOutFile() throws IOException {
        if (this.outLoc.isDirectory()) {
            // We use "data.tbl" if we output to a directory.
            this.writer = new PrintWriter(new File(this.outLoc, "data.tbl"));
        } else
            this.writer = new PrintWriter(this.outLoc);
    }

    /**
     * Specify the header column names for the main output.
     *
     * @param idCol		name of the ID column
     * @param featCols	list of feature column names
     * @param outCol	name of the output column
     *
     * @throws IOException
     */
    public void setHeaders(String idCol, List<String> featCols, String outCol) throws IOException {
        final int n = featCols.size();
        this.headers = new String[n + 2];
        this.headers[0] = idCol;
        for (int i = 0; i < n; i++)
            this.headers[i+1] = featCols.get(i);
        this.headers[n + 1] = outCol;
        // Allocate an output buffer for this line.
        this.buffer = new StringBuffer(n * 15 + idCol.length() + outCol.length() + 10);
        // Allow the subclass to process the headers.
        this.initialize(idCol, featCols, outCol);
    }

    /**
     * @return the delimiter to use for data rows
     */
    protected abstract char getDelim();

    /**
     * Initialize the XMatrix output.
     *
     * @param idCol		ID column name
     * @param featCols	list of feature column names
     * @param outCol	value column name
     *
     * @throws IOException
     */
    protected abstract void initialize(String idCol, List<String> featCols, String outCol) throws IOException;

    /**
     * Process a row of data.
     *
     * @param id		ID for this data row
     * @param feats		array of feature values
     * @param value		output value, in string form
     */
    public abstract void processRow(String id, double[] feats, String value);

    /**
     * Finish writing the report.
     *
     * @throws IOException
     */
    public abstract void finish() throws IOException;

    /**
     * @return the full header line
     */
    protected String getHeaderLine() {
        return StringUtils.join(this.headers, '\t');
    }

    /**
     * Write a row of data to the main output file.
     *
     * @param id		ID for this data row
     * @param feats		array of feature values
     * @param value		output value, in string form
     */
    protected void writeRow(String id, double[] feats, String value) {
        // Empty the output buffer.
        this.buffer.setLength(0);
        // Add the ID column.
        this.buffer.append(id);
        // Add the feature values.
        Arrays.stream(feats).forEach(x -> this.buffer.append(this.delim).append(x));
        // Add the output value.
        this.buffer.append(this.delim).append(value);
        // Write the buffer.
        this.writeLine(this.buffer.toString());
    }

    /**
     * Write a row of data to the main output file.
     *
     * @param id		ID for this data row
     * @param feats		array of feature values
     * @param value		output value, as a number
     */
    protected void writeRow(String id, double[] feats, double value) {
        this.writeRow(id, feats, Double.toString(value));
    }

    /**
     * Write a line of output to the main output file.
     *
     * @param line		line to write
     */
    protected void writeLine(String line) {
        this.writer.println(line);
    }

    /**
     * Insure the main output file is closed.
     */
    @Override
    public void close() {
        if (this.writer != null)
            this.writer.close();
        this.writer = null;
    }

}
