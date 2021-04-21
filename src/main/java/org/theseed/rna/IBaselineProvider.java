/**
 *
 */
package org.theseed.rna;

/**
 * This interface is used to describe an object that can return the baseline level for RNA data.
 *
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
