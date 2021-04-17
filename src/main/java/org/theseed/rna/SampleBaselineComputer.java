/**
 *
 */
package org.theseed.rna;

import org.theseed.rna.RnaData.Row;

/**
 * This baseline computer uses the values of a particular sample as the baseline.
 *
 * @author Bruce Parrello
 *
 */
public class SampleBaselineComputer extends BaselineComputer {

    // FIELDS
    /** column index of the target sample */
    private int colIdx;

    /**
     * Construct a sample-based baseline computer.
     *
     * @param sampleId	ID string of the relevant sample
     * @param data		RNA seq database
     */
    public SampleBaselineComputer(String sampleId, RnaData data) {
        this.colIdx = data.getColIdx(sampleId);
        if (this.colIdx < 0)
            throw new IllegalArgumentException("Invalid sample ID " + sampleId + " specified for baseline.");
    }

    @Override
    public double getBaseline(Row row) {
        RnaData.Weight weight = row.getWeight(this.colIdx);
        double retVal = 0.0;
        if (weight != null && weight.isExactHit())
            retVal = weight.getWeight();
        if (Double.isNaN(retVal))
            retVal = 0.0;
        return retVal;
    }

}
