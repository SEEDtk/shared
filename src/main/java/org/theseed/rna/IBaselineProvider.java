/**
 *
 */
package org.theseed.rna;

/**
 * @author Bruce Parrello
 *
 */
public interface IBaselineProvider {

    /**
     * @return the baseline expression value for the specified row
     *
     * @param row	data row for the feature of interest
     */
    public double getBaseline(RnaData.Row row);

}
