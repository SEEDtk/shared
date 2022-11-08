/**
 *
 */
package org.theseed.sequence;

import java.util.HashSet;

/**
 * This class manages kmers for an RNA sequence.  Unlike DNA, RNA is not reversible.  We also allow multiple
 * sequences to be added.
 *
 * @author Bruce Parrello
 *
 */
public class RnaKmers extends SequenceKmers {

    // FIELDS

    /** default kmer size */
    private static int defaultK = 24;
    /** kmer size */
    private final int K;

    /**
     * Create an empty RNA kmer hash.
     *
     * @param kSize		kmer size
     */
    public RnaKmers(int kSize) {
        this.sequence = "";
        this.kmerSet = new HashSet<String>();
        this.K = defaultK;
    }

    /**
     * Create kmers for a single RNA sequence.
     *
     * @param rna	rna sequence to process
     */
    public RnaKmers(String rna) {
        this.K = defaultK;
        setup(rna);
    }

    /**
     * Create kmers for a single DNA sequence with a specified kmer size.
     *
     * @param dna		dna sequence to process
     * @param kSize		kmer size to use
     */
    public RnaKmers(String rna, int kSize) {
        this.K = kSize;
        setup(rna);
    }

    /**
     * Initialize the kmers.
     *
     * @param rna	DNA sequence to process
     */
    private void setup(String rna) {
        this.sequence = "";
        int cap = 4 * (rna.length() < K ? K : rna.length()) / 3 + 1;
        this.kmerSet = new HashSet<String>(cap);
        this.addSequence(rna);
    }

    /**
     * Add the kmers for a new sequence to this one.
     *
     * @param rna		RNA sequence to add
     */
    public void addSequence(String rna) {
        // Save the sequence if it's longer than the one we have.  This means we won't always get an exact equality
        // if there are ambiguity runs unless there is only one incoming sequence.
        String normalized = rna.toLowerCase();
        if (rna.length() > this.sequence.length())
            this.sequence = normalized;
        final int n = rna.length() - K;
        for (int i = 0; i <= n; i++)
            this.kmerSet.add(normalized.substring(i, i + K));
    }

    /**
     * Specify a new global DNA kmer size.
     *
     * @param kSize	proposed new kmer size
     */
    public static void setKmerSize(int kSize) {
        defaultK = kSize;
    }

    /**
     * @return the current DNA kmer size
     */
    public static int kmerSize() {
        return defaultK;
    }

    /**
     * @return the original RNA sequence
     */
    public String getRna() {
        return this.sequence;
    }

    /**
     * @return the length of the original RNA sequence
     */
    public int getLen() {
        return this.sequence.length();
    }

    @Override
    public int getK() {
        return this.K;
    }

}
