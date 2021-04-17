/**
 *
 */
package org.theseed.rna;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * This converter reduces the expression value to a standard score, that is x -> (x - mean) / sdev.
 *
 * @author Bruce Parrello
 *
 */
public class StandardScoreExpressionConverter extends ExpressionConverter {

    // FIELDS
    /** mean value */
    private double mean;
    /** standard deviation */
    private double sdev;

    @Override
    protected void processRow() {
        DescriptiveStatistics stats = RnaData.getStats(this.getRow());
        this.mean = stats.getMean();
        this.sdev = stats.getStandardDeviation();
    }

    @Override
    public double convert(double rawValue) {
        double retVal = 0.0;
        if (this.sdev > 0.0)
            retVal = (rawValue - this.mean) / this.sdev;
        return retVal;
    }

}
