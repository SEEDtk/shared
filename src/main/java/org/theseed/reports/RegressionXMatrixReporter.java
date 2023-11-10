/**
 *
 */
package org.theseed.reports;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.theseed.basic.ParseFailureException;

/**
 * This reporter generates a DL4J Xmatrix directory for a regression model.  In this case, the labels file contains
 * only the value header.
 *
 * @author Bruce Parrello
 *
 */
public class RegressionXMatrixReporter extends Dl4jDirXMatrixReporter {

    // FIELDS
    /** output label name */
    private String outLabel;

    /**
     * Construct an xmatrix reporter for a regression directory.
     *
     * @param processor		controlling command processor
     * @param outDir		output directory name
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    public RegressionXMatrixReporter(IParms processor, File outDir) throws ParseFailureException, IOException {
        super(processor, outDir);
    }

    @Override
    protected void initializeDirectory(String idCol, List<String> featCols, String outCol) throws IOException {
        // Save the output column name.
        this.outLabel = outCol;
    }

    @Override
    protected List<String> getLabels() {
        return List.of(this.outLabel);
    }

    @Override
    protected void finishDirectory() {
        // No extra files needed.
    }

}
