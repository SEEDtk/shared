package org.theseed.sequence;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import murmur3.MurmurHash3;

public abstract class SequenceKmers implements Iterable<String> {

    // FIELDS

    /** similarity score for identical proteins */
    public static final int INFINITY = 9999;

    /** default seed */
    private static final int SEED = 1842724469;

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

    /**
     * @return a sorted set of integers representing the kmers in this object
     *
     * @param size	number of integers to return
     */
    public int[] hashSet(int size) {
        TreeSet<Integer> buffer = new TreeSet<Integer>();
        for (String kmer : this.kmerSet) {
            int shortHash = hashKmer(kmer);
            if (buffer.size() < size)
                buffer.add(shortHash);
            else if (shortHash < buffer.last()) {
                buffer.remove(buffer.last());
                buffer.add(shortHash);
            }
        }
        int[] retVal = buffer.stream().mapToInt(Integer::intValue).toArray();
        return retVal;
    }

    /**
     * @return the hash code for a kmer string
     *
     * @param kmer	kmer string to hash
     */
    public static int hashKmer(String kmer) {
        int retVal = MurmurHash3.murmurhash3_x86_32(kmer, 0, kmer.length(), SEED);
        if (retVal < 0) retVal = -retVal;
        return retVal;
    }

    /**
     * This computes the Jaccard distance between two signature arrays.  The arrays must
     * both be sorted in ascending order.
     *
     * @param hash1		first signature array
     * @param hash2		second signature array
     *
     * @return the Jaccard distance between the two signature arrays
     */
    public static double signatureDistance(int[] hash1, int[] hash2) {
        int i1 = 0;
        int i2 = 0;
        int found = 0;
        while (i1 < hash1.length && i2 < hash2.length) {
            if (hash1[i1] == hash2[i2]) {
                found++;
                i1++;
                i2++;
            } else if (hash1[i1] < hash2[i2])
                i1++;
            else
                i2++;
        }
        return 1 - ((double) found) / (hash1.length + hash2.length - found);
    }

    /**
     * Remove kmers in this set that are NOT in the specified other set.
     *
     * @param other		other kmer set to compare to this one
     */
    public void retainAll(SequenceKmers other) {
        this.kmerSet.retainAll(other.kmerSet);
    }

    /**
     * Remove kmers in this set that ARE in the specified other set.
     *
     * @param other		other kmer set to compare to this one
     */
    public void removeAll(SequenceKmers other) {
        this.kmerSet.removeAll(other.kmerSet);
    }

    @Override
    public Iterator<String> iterator() {
        return this.kmerSet.iterator();
    }

}
