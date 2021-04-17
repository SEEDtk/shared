/**
 *
 */
package org.theseed.rna;

/**
 * This version of the expression converter computes the mean expression value for each row, and then it returns 0, 1, or -1
 * depending on whether the value is close to the mean, greater than twice the mean, or less than half the mean.
 *
 * @author Bruce Parrello
 *
 */
public class TriageExpressionConverter extends ExpressionConverter {

    // FIELDS
    /** minimum value for a +1 result */
    private double highLimit;
    /** maximum value for a -1 result */
    private double lowLimit;
    /** baseline expression value provider */
    private IBaselineProvider baselineComputer;

    /**
     * Construct the triage expression converter.  We get from the controlling processor the algorithm for
     * computing the baseline.
     */
    public TriageExpressionConverter(IBaselineProvider processor) {
        this.baselineComputer = processor;
    }

    @Override
    protected void processRow() {
        double baseline = this.baselineComputer.getBaseline(this.getRow());
        this.highLimit = 2.0 * baseline;
        this.lowLimit = baseline / 2.0;
    }

    @Override
    public double convert(double rawValue) {
        double retVal = 0.0;
        if (rawValue >= this.highLimit)
            retVal = 1.0;
        else if (rawValue <= this.lowLimit)
            retVal = -1.0;
        return retVal;
    }

}
