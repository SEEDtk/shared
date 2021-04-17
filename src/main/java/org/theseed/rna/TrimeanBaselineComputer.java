/**
 *
 */
package org.theseed.rna;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.theseed.rna.RnaData.Row;

/**
 * This is the simplest baseline computer, and requires no external parameters.  It takes the trimean of all
 * the expression values for a feature and uses it for the baseline.
 *
 * @author Bruce Parrello
 *
 */
public class TrimeanBaselineComputer extends BaselineComputer {

    @Override
    public double getBaseline(Row row) {
        DescriptiveStatistics stats = RnaData.getStats(row);
        double retVal = ((stats.getPercentile(25) + stats.getPercentile(75)) / 2.0 + stats.getPercentile(50)) / 2.0;
        return retVal;
    }

}
