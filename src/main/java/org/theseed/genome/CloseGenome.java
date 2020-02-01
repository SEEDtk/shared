/**
 *
 */
package org.theseed.genome;

import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This structure represents a genome close to another genome.  A list of these is used in provided in a GTO
 * to assist with annotation and quality control.
 *
 * @author Bruce Parrello
 *
 */
public class CloseGenome implements Comparable<CloseGenome> {

    // FIELDS

    /** ID of the close genome */
    String genomeId;
    /* name of the close genome */
    String genomeName;
    /** measurement of closeness; higher means closer */
    double closeness;
    /** analysis method */
    String method;

    /** This enum defines the keys used and their default values.
     */
    public static enum CloseGenomeKeys implements JsonKey {
        GENOME(""),
        GENOME_NAME("<unknown>"),
        CLOSENESS_MEASURE(0.0),
        ANALYSIS_METHOD("{unknown}");

        private final Object m_value;

        CloseGenomeKeys(final Object value) {
            this.m_value = value;
        }

        /** This is the string used as a key in the incoming JsonObject map.
         */
        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        /** This is the default value used when the key is not found.
         */
        @Override
        public Object getValue() {
            return this.m_value;
        }

    }

    /**
     * Create this close-genome descriptor from its JSON object.
     *
     * @param closeGenome	JSON object read in for this close-genome descriptor
     */
    public CloseGenome(JsonObject closeGenome) {
        this.genomeId = closeGenome.getStringOrDefault(CloseGenomeKeys.GENOME);
        this.genomeName = closeGenome.getStringOrDefault(CloseGenomeKeys.GENOME_NAME);
        this.closeness = closeGenome.getDoubleOrDefault(CloseGenomeKeys.CLOSENESS_MEASURE);
        this.method = closeGenome.getStringOrDefault(CloseGenomeKeys.ANALYSIS_METHOD);
    }

    /**
     * Compare two close-genome objects on distance.  The natural sort order is
     * closest to furthest within each method.  So a closeness of 100 compares LESS
     * than a closeness of 50.  If two genomes are equally close via the same method,
     * we punt and order by genome ID.  This is necessary so that we only return 0
     * for objects that represent the same genome and method.  Note also that if
     * we have the same genome ID and method, we return 0 even if the closeness is
     * different. When this happens, it's an error.
     */
    @Override
    public int compareTo(CloseGenome other) {
        int retVal = this.method.compareTo(other.method);
        if (retVal == 0) {
            int comp = this.genomeId.compareTo(other.genomeId);
            if (comp == 0)
                retVal = 0;
            else {
                retVal = Double.compare(other.closeness, this.closeness);
                if (retVal == 0)
                    retVal = comp;
            }
        }
        return retVal;
    }



    /**
     * @return the ID of the close genome
     */
    public String getGenomeId() {
        return genomeId;
    }

    /**
     * @return the name of the close genome
     */
    public String getGenomeName() {
        return genomeName;
    }

    /**
     * @return the closeness measure
     */
    public double getCloseness() {
        return closeness;
    }

    /**
     * @return the analysis method used to compute the closeness
     */
    public String getMethod() {
        return method;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((genomeId == null) ? 0 : genomeId.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CloseGenome)) {
            return false;
        }
        CloseGenome other = (CloseGenome) obj;
        if (genomeId == null) {
            if (other.genomeId != null) {
                return false;
            }
        } else if (!genomeId.equals(other.genomeId)) {
            return false;
        }
        if (method == null) {
            if (other.method != null) {
                return false;
            }
        } else if (!method.equals(other.method)) {
            return false;
        }
        return true;
    }

    /**
     * @return a JsonObject for this close genome descriptor
     */
    public JsonObject toJson() {
        JsonObject retVal = new JsonObject();
        retVal.put(CloseGenomeKeys.ANALYSIS_METHOD.getKey(), this.method);
        retVal.put(CloseGenomeKeys.CLOSENESS_MEASURE.getKey(), this.closeness);
        retVal.put(CloseGenomeKeys.GENOME.getKey(), this.genomeId);
        retVal.put(CloseGenomeKeys.GENOME_NAME.getKey(), this.genomeName);
        return retVal;
    }



}
