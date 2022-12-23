/**
 *
 */
package org.theseed.sequence;

import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.theseed.genome.Contig;

/**
 * This package manages kmers for a DNA sequence.  Unlike a protein sequence, we need to store kmers for
 * both strands.  As a result, this object keeps an original copy of the sequence and also a strand-independent
 * copy.  The strand-independent copy is the lexically smaller of the sequence itself and its reverse
 * complement.  The independent copy is stored in the superclass; the original is available only at this
 * level.  The independent string is used to compute the hash code and to compare sequences.
 *
 * @author Bruce Parrello
 *
 */
public class DnaKmers extends SequenceKmers {

    // FIELDS

    /** default kmer size */
    private static int defaultK = 24;
    /** kmer size */
    private final int K;
    /** original DNA sequence */
    private String original;

    /**
     * Create kmers for a single DNA sequence.
     *
     * @param dna	dna sequence to process
     */
    public DnaKmers(String dna) {
        this.K = defaultK;
        setup(dna);
    }

    /**
     * Create kmers for a single DNA sequence with a specified kmer size.
     *
     * @param dna		dna sequence to process
     * @param kSize		kmer size to use
     */
    public DnaKmers(String dna, int kSize) {
        this.K = kSize;
        setup(dna);
    }

    /**
     * Initialize the kmers.
     *
     * @param dna	DNA sequence to process
     */
    private void setup(String dna) {
        int cap = 8 * (dna.length() < K ? K : dna.length()) / 3 + 1;
        this.kmerSet = new HashSet<String>(cap);
        // Get the reverse complement.
        this.original = dna.toLowerCase();
        String rDna = Contig.reverse(this.original);
        // Sort the two sequences and save the lowest.  This is so that sequence compares always work.
        this.sequence = (rDna.compareTo(this.original) < 0 ? rDna : this.original);
        final int n = dna.length() - K;
        for (int i = 0; i <= n; i++) {
            this.kmerSet.add(this.original.substring(i, i + K));
            this.kmerSet.add(rDna.substring(i, i + K));
        }
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
     * @return the original DNA sequence
     */
    public String getDna() {
        return this.original;
    }

    /**
     * @return the length of the original DNA sequence
     */
    public int getLen() {
        return this.original.length();
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
        return StringUtils.containsOnly(kmer, "acgtu");
    }


}
