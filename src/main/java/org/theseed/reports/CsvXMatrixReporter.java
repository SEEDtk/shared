/**
 *
 */
package org.theseed.reports;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.theseed.utils.ParseFailureException;

/**
 * This is a very simple reporter designed to create the standard Python CSV files for machine learning.
 * Most of the work is done by the base class, but we specify a comma as the delimiter.
 *
 * @author Bruce Parrello
 *
 */
public class CsvXMatrixReporter extends XMatrixReporter {

    public CsvXMatrixReporter(IParms processor, File outDir) throws ParseFailureException, IOException {
        super(processor, outDir);
    }

    @Override
    protected char getDelim() {
        return ',';
    }

    @Override
    protected void initialize(String idCol, List<String> featCols, String outCol) throws IOException {
        // Insure the output file is set up.
        this.openOutFile();
    }

    @Override
    public void processRow(String id, double[] feats, String value) {
        // Write out the row using our delimiter.
        this.writeRow(id, feats, value);
    }

    @Override
    public void finish() throws IOException {
    }

}
