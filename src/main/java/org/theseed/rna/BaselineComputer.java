/**
 *
 */
package org.theseed.rna;

import java.io.File;
import java.io.FileNotFoundException;

import org.theseed.basic.ParseFailureException;

/**
 * This is the base class for an object that computes the baseline expression value for a feature in an RNA
 * database.  For example, the baseline can come from a particular sample, as an average of all the values for
 * the feature, or from an external file.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaselineComputer {

    /**
     * @return the baseline value for the specified data row
     *
     * @param row	RNA seq data row for the feature of interest
     */
    public abstract double getBaseline(RnaData.Row row);

    /**
     * Enumeration for the different baseline computation types
     */
    public static enum Type {
        TRIMEAN {
            @Override
            public BaselineComputer create(IBaselineParameters processor) {
                return new TrimeanBaselineComputer();
            }
        }, FILE {
            @Override
            public BaselineComputer create(IBaselineParameters processor) {
                return new FileBaselineComputer(processor.getFile());
            }
        }, SAMPLE {
            @Override
            public BaselineComputer create(IBaselineParameters processor) {
                return new SampleBaselineComputer(processor.getSample(), processor.getData());
            }
        };

        /**
         * Create a baseline computer of this type.
         *
         * @param processor		object containing the parameters for baseline computation
         *
         * @return a baseline computer of the specified type
         */
        public abstract BaselineComputer create(IBaselineParameters processor);

    }

    /**
     * This method performs standard validation for a baseline client, and returns the baseline
     * computation object.
     *
     * @param parms		baseline client
     * @param type		baseline computer type
     *
     * @return the appropriate baseline computer
     *
     * @throws ParseFailureException
     * @throws FileNotFoundException
     */
    public static BaselineComputer validateAndCreate(IBaselineParameters parms, BaselineComputer.Type type)
            throws ParseFailureException, FileNotFoundException {
        switch (type) {
        case FILE:
            File bFile = parms.getFile();
            if (bFile == null)
                throw new ParseFailureException("Baseline value file required for baseline type FILE.");
            else if (! bFile.canRead())
                throw new FileNotFoundException("Baseline value file " + bFile + " not found or unreadable.");
            break;
        case SAMPLE:
            if (parms.getSample() == null)
                throw new ParseFailureException("Baseline sample ID required for baseline type SAMPLE.");
            break;
        default:
            break;
        }
        // Create the computation object.
        BaselineComputer retVal = type.create(parms);
        return retVal;
    }


}
