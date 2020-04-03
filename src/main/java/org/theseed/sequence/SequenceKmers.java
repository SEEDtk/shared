package org.theseed.sequence;

import java.util.HashSet;

public abstract class SequenceKmers {

    // FIELDS

    /** similarity score for identical proteins */
    public static final int INFINITY = 9999;

    /** initial sequence string */
    protected String sequence;
    /** set of kmers in the sequence */
    protected HashSet<String> kmerSet;


    /**
     * @return the number of kmers in common between two proteins
     *
     * @param other		the sequence-kmers object for the other sequence
     */
    public int similarity(SequenceKmers other) {
        int retVal = 0;
        if (this.sequence.contentEquals(other.sequence)) {
            // Identical proteins.  Use the fake infinity.  This will be
            // higher than the highest possible for the sequence, since the maximum otherwise
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
     * @param other		the sequence-kmers object for the other sequence
     */
    public double distance(SequenceKmers other) {
        double retVal = 1.0;
        if (this.sequence.contentEquals(other.sequence)) {
            // Same sequence.  Return 0 distance.  See the odd similarity computation above.
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
     * @return the kmer count
     */
    public int size() {
        return this.kmerSet.size();
    }

    /** Only the sequence sequence matters to this object. */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.sequence == null) ? 0 : this.sequence.hashCode());
        return result;
    }

    /** Only the sequence matters to this object. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SequenceKmers other = (SequenceKmers) obj;
        if (this.sequence == null) {
            if (other.sequence != null)
                return false;
        } else if (! this.sameSequence(other.sequence))
            return false;
        return true;
    }

    /**
     * @return TRUE if this object's sequence is equal to the other's
     *
     * @param otherSeq		other sequence to compare
     */
    protected boolean sameSequence(String otherSeq) {
        return this.sequence.contentEquals(otherSeq);
    }

}
