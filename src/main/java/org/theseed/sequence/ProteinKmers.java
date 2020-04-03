/**
 *
 */
package org.theseed.sequence;

import java.util.HashSet;

/**
 * This package manages the set of sequence kmers in a protein string.
 *
 * @author Bruce Parrello
 *
 */
public class ProteinKmers extends SequenceKmers {

    /** current kmer size */
    private static int K = 8;

    /**
     * Generate a sequence kmer set for a specified protein.
     */
    public ProteinKmers(String protein) {
        this.sequence = protein.toUpperCase();
        int n = this.sequence.length() - K;
        int cap = (n < K ? K : n);
        this.kmerSet = new HashSet<String>(cap);
        for (int i = 0; i <= n; i++) {
            kmerSet.add(this.sequence.substring(i, i + K));
        }
    }

    /**
     * Specify a new global protein kmer size.
     *
     * @param kSize	proposed new kmer size
     */
    public static void setKmerSize(int kSize) {
        K = kSize;
    }

    /**
     * @return the current protein kmer size
     */
    public static int kmerSize() {
        return K;
    }

    /**
     * @return the protein sequence
     */
    public String getProtein() {
        return this.sequence;
    }



}
