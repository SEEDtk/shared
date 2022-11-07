/**
 *
 */
package org.theseed.sequence;

import java.util.HashSet;

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

    /** current kmer size */
    private static int K = 24;

    /** original DNA sequence */
    private String original;

    /**
     * Create an empty DNA kmer hash.
     */
    public DnaKmers() {
        this.original = "";
    }

    /**
     * Create kmers for a single DNA sequence.
     *
     * @param dna	dna sequence to process
     */
    public DnaKmers(String dna) {
        int cap = 8 * (dna.length() < K ? K : dna.length()) / 3 + 1;
        this.kmerSet = new HashSet<String>(cap);
        this.addSequence(dna);
    }

    /**
     * Add the kmers for a new sequence to this one.
     *
     * @param dna		DNA sequence to add
     */
    public void addSequence(String dna) {
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
        K = kSize;
    }

    /**
     * @return the current DNA kmer size
     */
    public static int kmerSize() {
        return K;
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


}
