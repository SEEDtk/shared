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

    public DnaKmers(String dna) {
        // Get the reverse complement.
        this.original = dna.toLowerCase();
        String rDna = Contig.reverse(this.original);
        this.sequence = (rDna.compareTo(this.original) < 0 ? rDna : this.original);
        int n = this.sequence.length() - K;
        int cap = 2 * (n < K ? K : n);
        this.kmerSet = new HashSet<String>(cap);
        for (int i = 0; i <= n; i++) {
            kmerSet.add(this.original.substring(i, i + K));
            kmerSet.add(rDna.substring(i, i + K));
        }
    }

    /**
     * Specify a new global sequence kmer size.
     *
     * @param kSize	proposed new kmer size
     */
    public static void setKmerSize(int kSize) {
        K = kSize;
    }

    /**
     * @return the current sequence kmer size
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

}
