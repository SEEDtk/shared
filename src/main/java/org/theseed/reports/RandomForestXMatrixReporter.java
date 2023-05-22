/**
 *
 */
package org.theseed.reports;

import java.io.File;
import java.io.IOException;

import org.theseed.io.MarkerFile;
import org.theseed.utils.ParseFailureException;

/**
 * This reporter produces a DL4J XMatrix directory.  This differs from the standard classifier in that there
 * is an extra indicator file.
 *
 * @author Bruce Parrello
 *
 */
public class RandomForestXMatrixReporter extends ClassifierXMatrixReporter {

    public RandomForestXMatrixReporter(IParms processor, File outDir) throws ParseFailureException, IOException {
        super(processor, outDir);
        File dFile = new File(outDir, "decider.txt");
        MarkerFile.write(dFile, "Random Forest");
    }

}
