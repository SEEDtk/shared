/**
 *
 */
package org.theseed.reports;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.theseed.utils.ParseFailureException;

/**
 * This method creates a classifier directory.  Its main difference from the basic regression format is
 * that we compute the labels by collecting the output values.
 *
 * @author Bruce Parrello
 *
 */
public class ClassifierXMatrixReporter extends Dl4jDirXMatrixReporter {

    // FIELDS
    /** set of output label values */
    private Set<String> labels;
    /** negative label name */
    private String negLabel;

    /**
     * Construct a reporter to generate the DL4J XMatrix directory for a classifier.
     *
     * @param processor		controlling command processor
     * @param outDir		output directory name
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    public ClassifierXMatrixReporter(IParms processor, File outDir) throws ParseFailureException, IOException {
        super(processor, outDir);
        // Get the negative label value.
        this.negLabel = processor.getNegLabel();
        if (this.negLabel == null)
            throw new ParseFailureException("A negative-condition label value is required for this type of output.");
        // Create the label set.
        this.labels = new TreeSet<String>();
    }


    @Override
    protected void initializeDirectory(String idCol, List<String> featCols, String outCol) throws IOException {
        // No extra initialization is needed.
    }

    @Override
    public void processRow(String id, double[] feats, String value) {
        // If this value is not an example of the negative label, save it in the label set.
        if (! value.contentEquals(this.negLabel))
            this.labels.add(value);
        // Write the output row.
        this.writeRow(id, feats, value);
    }

    @Override
    protected List<String> getLabels() {
        // The negative label goes first, followed by the others.
        List<String> retVal = new ArrayList<String>(this.labels.size() + 1);
        retVal.add(this.negLabel);
        retVal.addAll(this.labels);
        return retVal;
    }

    @Override
    protected void finishDirectory() {
        // No extra files are needed.
    }

}
