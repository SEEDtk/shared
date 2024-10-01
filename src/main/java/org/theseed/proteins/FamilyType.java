/**
 *
 */
package org.theseed.proteins;

import org.apache.commons.lang3.StringUtils;
import org.theseed.genome.Feature;

/**
 * This enumeration represents a protein family type. Each type provides a method for getting a family ID from
 * a genome feature.
 *
 * @author Bruce Parrello
 *
 */
public enum FamilyType {
    /** global protein family */
    PGFAM {
        @Override
        public String getFamily(Feature feat) {
            String retVal = feat.getPgfam();
            return fixFamId(retVal);
        }
    },
    /** local protein family */
    PLFAM {
        @Override
        public String getFamily(Feature feat) {
            String retVal = feat.getPlfam();
            return fixFamId(retVal);
        }
    };

    /**
     * Convert an invalid family ID to NULL.
     *
     * @param famId		family ID to examine
     *
     * @return the incoming family ID, or NULL if it is blank or empty
     */
    public static String fixFamId(String famId) {
        if (StringUtils.isBlank(famId))
            famId = null;
        return famId;
    }

    /**
     * Extract the protein family ID of this type.
     *
     * @param feat		feature whose protein family is desired
     *
     * @return the protein family ID, or NULL if there is none
     */
    public abstract String getFamily(Feature feat);

}
