package org.theseed.sequence;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import murmur3.MurmurHash3;

public abstract class SequenceKmers implements Iterable<String> {

    // FIELDS

    /** similarity score for identical proteins */
    public static final int INFINITY = Integer.MAX_VALUE;

    /** default seed */
    private static final int SEED = 1842724469;

    /** initial sequence string */
    protected String sequence;
    /** set of kmers in the sequence */
    protected Set<String> kmerSet;

    /**
     * @return the number of kmers in common between two proteins
     *
     * @param other		the sequence-kmers object for the other sequence
     */
    public int similarity(SequenceKmers other) {
        int retVal = 0;
        if (this.sequence.contentEquals(other.sequence)) {
            // Identical sequences.  Use the fake infinity.  This will be
            // higher than the highest possible for the sequence, since the maximum otherwise
            // is length - K.  We need to do this so that sequences with long X runs are
            // equal to themselves.
            retVal = INFINITY;
        } else {
            long count = this.kmerSet.stream().filter(x -> other.contains(x)).count();
            retVal = (int) count;
        }
        return retVal;
    }

    /**
     * @return the kmer size
     */
    public abstract int getK();

    /**
     * @return the Jaccard distance between two proteins
     *
     * @param other		the sequence-kmers object for the other sequence
     */
    public double distance(SequenceKmers other) {
        int sim = this.similarity(other);
        return distance(sim, this, other);
    }

    /**
     * Convert a similarity into a distance.
     *
     * @param sim		number of kmers shared between two sequences
     * @param curr		kmers for the first sequence
     * @param other		kmers for the second sequence
     *
     * @return the distance between the two sequences
     */
    public static double distance(int sim, SequenceKmers curr, SequenceKmers other) {
        double retVal = 1.0;
        if (sim == ProteinKmers.INFINITY)
            retVal = 0.0;
        else if (sim > 0) {
            double union = (curr.size() + other.size() - sim);
            retVal -= sim / union;
        }
        return retVal;
    }

    /**
     * @return the kmer count
     */
    public int size() {
        return this.kmerSet.size();
    }

    /** Only the sequence matters to this object. */
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

    /**
     * @return TRUE if the specified kmer is in this set, else FALSE
     *
     * @param kmer		kmer to test
     */
    public boolean contains(String kmer) {
        return this.kmerSet.contains(kmer);
    }

    @Override
    public Iterator<String> iterator() {
        return this.kmerSet.iterator();
    }

}
