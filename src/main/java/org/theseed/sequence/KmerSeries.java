/**
 *
 */
package org.theseed.sequence;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * This is an iterable for retrieving all the kmers in a sequence.
 *
 * @author Bruce Parrello
 *
 */
public class KmerSeries implements Iterable<String> {

    // FIELDS
    /** list of DNA sequences */
    private Collection<String> sequences;
    /** kmer size */
    private final int kmerSize;

    /**
     * Construct a kmer series for a collection of sequences.
     *
     * @param seqs		sequences to iterate through
     * @param kSize		desired kmer size
     */
    public KmerSeries(Collection<String> seqs, int kSize) {
        this.kmerSize = kSize;
        this.sequences = seqs;
    }

    /**
     * Construct a kmer series for a single sequence.
     *
     * @param seq		sequence to iterate through
     * @param kSize		kmer size
     */
    public KmerSeries(String seq, int kSize) {
        this.kmerSize = kSize;
        this.sequences = List.of(seq);
    }

    /**
     * Construct a kmer series for a collection of sequences.
     *
     * @param seqs		collection of sequences to iterate through
     * @param kSize		desired kmer size
     */
    public static KmerSeries init(Collection<Sequence> seqs, int kSize) {
        Collection<String> strings = seqs.stream().map(x -> x.getSequence()).collect(Collectors.toList());
        return new KmerSeries(strings, kSize);
    }

    /**
     * This is the iterator class.
     */
    protected class Iter implements Iterator<String> {

        /** iterator through the string list */
        private Iterator<String> seqIter;
        /** current sequence */
        private String current;
        /** position of last kmer in the current sequence */
        private int limit;
        /** current position in sequence */
        private int pos;

        /**
         * Construct the iterator.
         */
        protected Iter() {
            this.seqIter = KmerSeries.this.sequences.iterator();
            this.current = null;
            this.limit = -1;
            this.pos = 0;
            this.readAhead();
        }

        /**
         * Position on the next valid kmer.
         */
        private void readAhead() {
            while (this.pos > this.limit && this.seqIter.hasNext()) {
                this.current = this.seqIter.next();
                this.pos = 0;
                this.limit = this.current.length() - KmerSeries.this.kmerSize;
            }
            // Here either the iterator is at the end, or we are positioned
            // on a valid kmer.
        }

        @Override
        public boolean hasNext() {
            return (this.pos <= this.limit);
        }

        @Override
        public String next() {
            // Insure we have room in the current sequence.
            if (this.pos > this.limit)
                throw new NoSuchElementException("Attempt to iterate past last kmer.");
            // Now we know we have a kmer.
            String retVal = this.current.substring(this.pos, this.pos + KmerSeries.this.kmerSize);
            // Position on the next kmer.
            this.pos++;
            this.readAhead();
            return retVal;
        }

    }

    @Override
    public Iterator<String> iterator() {
        return this.new Iter();
    }

    /**
     * @return the number of kmers in this series
     */
    public int size() {
        int retVal = this.sequences.stream().mapToInt(x -> x.length() - this.kmerSize + 1)
                .filter(i -> i > 0).sum();
        return retVal;
    }

}
