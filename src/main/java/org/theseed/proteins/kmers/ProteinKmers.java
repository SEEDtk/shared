/**
 *
 */
package org.theseed.proteins.kmers;

import java.util.HashSet;

/**
 * This package manages the set of protein kmers in a protein string.
 *
 * @author Bruce Parrello
 *
 */
public class ProteinKmers {

    /** current kmer size */
    private static int K = 8;

    /** similarity score for identical proteins */
    public final static int INFINITY = 9999;

    // FIELDS
    /** initial protein string */
    private String protein;
    /** set of kmers in the protein */
    private HashSet<String> kmerSet;

    /**
     * Generate a protein kmer set for a specified protein.
     */
    public ProteinKmers(String protein) {
        this.protein = protein.toUpperCase();
        int n = this.protein.length() - K;
        int cap = (n < K ? K : n);
        this.kmerSet = new HashSet<String>(cap);
        for (int i = 0; i <= n; i++) {
            kmerSet.add(this.protein.substring(i, i + K));
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
     * @return the number of kmers in common between two proteins
     *
     * @param other		the protein-kmers object for the other protein
     */
    public int similarity(ProteinKmers other) {
        int retVal = 0;
        if (this.protein.contentEquals(other.protein)) {
            // Identical proteins.  Use the fake infinity.  This will be
            // higher than the highest possible for the protein, since the maximum otherwise
            // is length - K.  We need to do this so that proteins with long X runs are
            // equal to themselves.
            retVal = ProteinKmers.INFINITY;
        } else {
            for (String kmer : other.kmerSet) {
                if (this.kmerSet.contains(kmer)) {
                    retVal++;
                }
            }
        }
        return retVal;
    }

    /**
     * @return the Jaccard distance between two proteins
     *
     * @param other		the protein-kmers object for the other protein
     */
    public double distance(ProteinKmers other) {
        double retVal = 1.0;
        if (this.protein.contentEquals(other.protein)) {
            // Same protein.  Return 0 distance.  See the odd similarity computation above.
            retVal = 0.0;
        } else {
            double similarity = this.similarity(other);
            if (similarity > 0) {
                double union = (this.kmerSet.size() + other.kmerSet.size()) - similarity;
                retVal = 1.0 - similarity / union;
            }
        }
        return retVal;
    }

    /**
     * @return the protein
     */
    public String getProtein() {
        return this.protein;
    }

    /**
     * @return the kmer count
     */
    public int size() {
        return this.kmerSet.size();
    }

    /** Only the protein sequence matters to this object. */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.protein == null) ? 0 : this.protein.hashCode());
        return result;
    }

    /** Only the protein sequence matters to this object. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProteinKmers other = (ProteinKmers) obj;
        if (this.protein == null) {
            if (other.protein != null)
                return false;
        } else if (!this.protein.equals(other.protein))
            return false;
        return true;
    }



}
