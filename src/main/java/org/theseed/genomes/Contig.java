package org.theseed.genomes;

import org.theseed.locations.Region;

import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This class implements a contig.  It contains the ID, sequence, and genetic code, and is
 * constructible from a JsonObject.
 *
 * @author Bruce Parrello
 *
 */
public class Contig {


    // FIELDS

    private String id;
    private String sequence;
    private int geneticCode;

    /** This enum defines the keys used and their default values.
     */
    private enum ContigKeys implements JsonKey {
        ID("0"),
        DNA(""),
        GENETIC_CODE(11);

        private final Object m_value;

        ContigKeys(final Object value) {
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
     * Create the contig from the incoming JsonObject.  Note the DNA is converted to lower case.
     *
     * @param contigObj	JsonObject read into memory during genome input
     */
    public Contig(JsonObject contigObj) {
        this.id = contigObj.getStringOrDefault(ContigKeys.ID);
        this.sequence = contigObj.getStringOrDefault(ContigKeys.DNA).toLowerCase();
        this.geneticCode = contigObj.getIntegerOrDefault(ContigKeys.GENETIC_CODE);
    }

    /**
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return the sequence
     */
    public String getSequence() {
        return this.sequence;
    }

    /**
     * @return the reverse compliment of the sequence
     */
    public String getRSequence() {
        return reverse(this.sequence);
    }

    /**
     * @return the geneticCode
     */
    public int getGeneticCode() {
        return this.geneticCode;
    }

    /**
     * @return the DNA string for the specified region on this contig
     *
     * @param region	the region whose DNA is desired
     */
    public String getDna(Region region) {
        int left = region.getLeft() - 1;
        if (left < 0) {
            left = 0;
        }
        int right = region.getRight();
        if (right < 0 || right > this.sequence.length()) {
            right = this.sequence.length();
        }
        return this.sequence.substring(left, right);
    }

    /**
     * @return the reverse complement of a DNA string
     *
     * @param dna	the DNA string to reverse (must be lower case)
     */
    public static String reverse(String dna) {
        int n = dna.length();
        StringBuilder retVal = new StringBuilder(n);
        for (int i = n - 1; i >= 0; i--) {
            char rev;
            char norm = dna.charAt(i);
            switch (norm) {
            case 'a' :
                rev = 't';
                break;
            case 'c' :
                rev = 'g';
                break;
            case 'g' :
                rev = 'c';
                break;
            case 't' :
            case 'u' :
                rev = 'a';
                break;
            default :
                rev = 'x';
            }
            retVal.append(rev);
        }
        return retVal.toString();
    }

}
