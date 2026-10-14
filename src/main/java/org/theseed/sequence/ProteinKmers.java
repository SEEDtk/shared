/**
 *
 */
package org.theseed.sequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
    /** list of additional protein sequences (usually empty) */
    private List<String> additionalProteins;

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
        this.kmerSet = new HashSet<>(cap);
        for (int i = 0; i <= n; i++)
            kmerSet.add(this.sequence.substring(i, i + K));
        this.additionalProteins = null;
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

    /**
     * Merge the kmers from another ProteinKmers object into this one.
     * 
     * @param other    the other ProteinKmers object whose kmers are to be merged into this one
     */
    public void merge(ProteinKmers other) {
        this.kmerSet.addAll(other.kmerSet);
        // Make sure we have all the protein sequences recorded.
        if (this.additionalProteins == null)
            this.additionalProteins = new ArrayList<>(2);
        if (other.additionalProteins != null)
            this.additionalProteins.addAll(other.additionalProteins);
        this.additionalProteins.add(other.sequence);
    }

    /**
     * This method returns the total protein sequence for this ProteinKmers object. Normally, this
     * is just the primary sequence, but if additional proteins have been merged in, it may be a 
     * comma-separated string of multiple protein sequences.
     * 
     * @return a concatenated protein string
     */
    public String getFullProteinString() {
        if (this.additionalProteins == null || this.additionalProteins.isEmpty())
            return this.sequence;
        StringBuilder sb = new StringBuilder(this.sequence);
        for (String prot : this.additionalProteins)
            sb.append(',').append(prot);
        return sb.toString();
    }

    /**
     * @see org.theseed.sequence.SequenceKmers#getK()
     */

    @Override
    public int getK() {
        return this.K;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProteinKmers other = (ProteinKmers) obj;
        String[] thisProteins = this.getFullProteinString().split(",");
        String[] otherProteins = other.getFullProteinString().split(",");
        if (thisProteins.length != otherProteins.length)
            return false;
        if (thisProteins.length == 1)
            return otherProteins[0].equals(thisProteins[0]);
        boolean retVal = true;
        for (int i = 0; i < thisProteins.length && retVal; i++) {
            retVal = false;
            for (int j = 0; j < otherProteins.length && ! retVal; j++)
                if (thisProteins[i].equals(otherProteins[j]))
                    retVal = true;
        }
        return retVal;
    }

    @Override
    public int hashCode() {
        return this.getFullProteinString().hashCode();
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
