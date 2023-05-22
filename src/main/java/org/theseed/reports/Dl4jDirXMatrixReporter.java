/**
 *
 */
package org.theseed.reports;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.theseed.utils.ParseFailureException;

/**
 * This is an intermediate class for all of the XMatrix reports that produce DL4J directories.  Each such directory
 * contains a training.tbl file containing headers, a data.tbl file for the actual data, and a labels.txt file
 * containing the labels.  The content of the label file depends on the subclass.  In addition, for random forest
 * an extra indicator file is produced.
 *
 * @author Bruce Parrello
 *
 */
public abstract class Dl4jDirXMatrixReporter extends XMatrixReporter {

    // FIELDS
    /** output directory name */
    private File outDir;

    /**
     * Construct a DL4J directory reporter.
     *
     * @param processor		controlling command processor
     * @param outDir		name of the output directory
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    public Dl4jDirXMatrixReporter(IParms processor, File outDir) throws ParseFailureException, IOException {
        super(processor, outDir);
    }

    @Override
    protected char getDelim() {
        return '\t';
    }

    @Override
    protected void initialize(String idCol, List<String> featCols, String outCol) throws IOException {
        // Set up the output directory and write the headers.
        this.outDir = this.getOutDir();
        // The training file contains only the headers.
        try (PrintWriter trainer = new PrintWriter(new File(this.outDir, "training.tbl"))) {
            trainer.println(this.getHeaderLine());
        }
        // We also write the headers to the main data file.
        this.writeLine(this.getHeaderLine());
        // Allow the subclass to initialize.
        this.initializeDirectory(idCol, featCols, outCol);
    }

    /**
     * Perform any necessary subclass initialization.
     *
     * @param idCol		name of the ID column
     * @param featCols	list of feature column names
     * @param outCol	name of the output column
     *
     * @throws IOException
     */
    protected abstract void initializeDirectory(String idCol, List<String> featCols, String outCol) throws IOException;

    @Override
    public void processRow(String id, double[] feats, String value) {
        this.writeRow(id, feats, value);
    }

    /**
     * Finish writing the report.
     *
     * @throws IOException
     */
    public void finish() throws IOException {
        // Ask for the label list and create the label file.
        List<String> labels = this.getLabels();
        File labelFile = new File(this.outDir, "labels.txt");
        try (PrintWriter labelStream = new PrintWriter(labelFile)) {
            for (var label : labels)
                labelStream.println(label);
        }
        // Allow the subclass to write additional files.
        this.finishDirectory();
    }

    /**
     * @return the ordered list of output labels
     */
    protected abstract List<String> getLabels();

    /**
     * Write any additional files needed.
     */
    protected abstract void finishDirectory();


}
