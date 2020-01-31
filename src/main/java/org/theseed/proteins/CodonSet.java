/**
 *
 */
package org.theseed.proteins;

import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

/**
 * This is a very simple object that represents an immutable set of codons.
 * It provides methods to find out if a codon is in the set.
 *
 * @author Bruce Parrello
 *
 */
public class CodonSet {

    // FIELDS
    private HashSet<String> codonMap;

    /**
     * Create a codon set with the specified codons.
     */
    public CodonSet(String... codons) {
        this.codonMap = new HashSet<String>(codons.length);
        for (String codon : codons)
            this.codonMap.add(codon);
    }

    /**
     * @return TRUE if the codon is in the set
     *
     * @param codon		codon to check
     */
    public boolean contains(String codon) {
        return this.codonMap.contains(codon);
    }

    /**
     * @return TRUE if the specified codon is in the specified location of the specified sequence
     *
     * @param seq		sequence to check
     * @param pos		position (1-based) to check in the sequence
     */
    public boolean contains(String seq, int pos) {
        return this.codonMap.contains(StringUtils.substring(seq, pos - 1, pos + 2));
    }

}
