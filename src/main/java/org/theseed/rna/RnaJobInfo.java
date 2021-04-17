/**
 *
 */
package org.theseed.rna;

/**
 * This class contains the information about a sample we need to process it for various RnaSeqBaseProcessor clients.
 */
public class RnaJobInfo {

    /** column index where we can find the job's weight */
    private int colIdx;
    /** production amount */
    private double production;
    /** optical density */
    private double growth;

    /**
     * Extract the information we need to process a particular sample.
     *
     * @param database	parent RNA seq database
     * @param job		job descriptor
     */
    public RnaJobInfo(RnaData database, RnaData.JobData job) {
        this.colIdx = database.getColIdx(job.getName());
        this.production = job.getProduction();
        this.growth = job.getOpticalDensity();
    }

    /**
     * @return the sample's expression data for a feature
     *
     * @param featureRow	RNA seq database row for the feature
     */
    public double getExpression(RnaData.Row featureRow) {
        RnaData.Weight weight = featureRow.getWeight(this.colIdx);
        double retVal = 0.0;
        if (weight != null) {
            retVal = weight.getWeight();
            if (! Double.isFinite(retVal))
                retVal = 0.0;
        }
        return retVal;
    }

    /**
     * @return TRUE if the sample's expression data is valid for a feature
     *
     * @param featureRow	RNA seq database row for the feature
     */
    public boolean isValid(RnaData.Row featureRow) {
        RnaData.Weight weight = featureRow.getWeight(this.colIdx);
        boolean retVal = (weight != null);
        if (retVal)
            retVal = weight.isExactHit() && Double.isFinite(weight.getWeight());
        return retVal;
    }

    /**
     * @return the production amount
     */
    public double getProduction() {
        return this.production;
    }

    /**
     * @return the optical density (growth)
     */
    public double getGrowth() {
        return this.growth;
    }

}
