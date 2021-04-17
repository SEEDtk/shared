/**
 *
 */
package org.theseed.rna;

import java.io.File;

/**
 * This interface must be implemented by a class that wants to provide parameters to the creation of
 * a BaselineComputer subclass.
 *
 * @author Bruce Parrello
 *
 */
public interface IBaselineParameters {

    /**
     * Here the baseline values are being kept in a tab-delimited file.  The feature ID is in a column named
     * "fid" and the baseline value in a column named "baseline".
     *
     * @return the name the file containing the baseline values
     */
    public File getFile();

    /**
     * Here the baseline values are found in a particular sample.
     *
     * @return the ID string of the sample to use for the baseline
     */
    public String getSample();

    /**
     * @return the RNA seq database
     */
    public RnaData getData();

}
