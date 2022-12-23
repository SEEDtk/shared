/**
 *
 */
package org.theseed.sequence;

import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

/**
 * This package manages the set of sequence kmers in a protein string.
 *
 * @author Bruce Parrello
 *
 */
public class ProteinKmers extends SequenceKmers {

    /** current kmer size */
    private final int K;
    /** default kmer size */
    private static int defaultK = 8;

    /**
     * Generate a sequence kmer set for a specified protein.
     *
     * @param protein	protein sequence to parse
     */
    public ProteinKmers(String protein) {
        this.K = defaultK;
        setup(protein);
    }

    /**
     * Generate a sequence kmer set for a specified protein with a specified kmer size.
     *
     * @param protein	protein sequence to parse
     * @param kSize		kmer size to use
     */
    public ProteinKmers(String protein, int kSize) {
        this.K = kSize;
        setup(protein);
    }

    /**
     * Initialize the kmer set for this sequence.
     *
     * @param protein	protein sequence to process
     */
    private void setup(String protein) {
        this.sequence = protein.toUpperCase();
        int n = this.sequence.length() - K;
        int cap = (n < K ? K : n) * 4 / 3 + 1;
        this.kmerSet = new HashSet<String>(cap);
        for (int i = 0; i <= n; i++)
            kmerSet.add(this.sequence.substring(i, i + K));
    }

    /**
     * Specify a new global protein kmer size.
     *
     * @param kSize	proposed new kmer size
     */
    public static void setKmerSize(int kSize) {
        defaultK = kSize;
    }

    /**
     * @return the current default protein kmer size
     */
    public static int kmerSize() {
        return defaultK;
    }

    /**
     * @return the protein sequence
     */
    public String getProtein() {
        return this.sequence;
    }

    @Override
    public int getK() {
        return this.K;
    }

    /**
     * @return TRUE if the specified kmer has no ambiguity characters, else FALSE
     *
     * @param kmer	kmer to check
     */
    public static boolean isClean(String kmer) {
        return StringUtils.containsOnly(kmer, "ARNDCQEGHILKMFPSTWYVUO");
    }


}
