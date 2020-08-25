/**
 *
 */
package org.theseed.sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class counts character occurrences in sequences.  The characters will be visible ASCII characters (between
 * 0x20 and 0x5F).  It is presumed the input is upper-cased.  A hyphen (-) has special meaning.
 *
 * @author Bruce Parrello
 *
 */
public class CharCounter {

    // FIELDS
    /** character counts */
    private int[] counts;
    /** total number of characters counted */
    private int totalChars;
    /** maximum character value */
    private static char MAX_CHAR = 0x5F;
    /** minimum character value */
    private static char MIN_CHAR = 0x20;
    /** total number of countable characters */
    private static int ARRAY_SIZE = MAX_CHAR + 1 - MIN_CHAR;

    /**
     * Create the character counter.
     */
    public CharCounter() {
        // Initialize the counters.
        this.counts = new int[ARRAY_SIZE];
        clear();
    }

    /**
     * Compute the counts for a specific position in a sequence list.
     *
     * @param seqList	collection of prepared sequences to process
     * @param pos		position (0-based) to count
     */
    public List<Count> countSequences(Collection<Sequence> seqList, int pos) {
        this.clear();
        seqList.stream().forEach(x -> this.count(x, pos));
        return this.getResults();
    }

    /**
     * This subclass describes a character count.  It sorts from highest count to lowest,
     * and then lexically by target character.
     */
    public static class Count implements Comparable<Count> {

        // FIELDS
        /** character being counted */
        private char target;
        /** number of occurrences */
        private int count;

        /**
         * Create a count record for the specified array position.
         *
         * @param parent	parent character counter
         * @param idx		position in the count array
         */
        private Count(CharCounter parent, int idx) {
            this.target = (char) (MIN_CHAR + idx);
            this.count = parent.counts[idx];
        }

        @Override
        public int compareTo(Count o) {
            int retVal = (o.count - this.count);
            if (retVal == 0)
                retVal = (this.target - o.target);
            return retVal;
        }

        /**
         * @return the character being counted
         */
        public char getTarget() {
            return this.target;
        }

        /**
         * @return the occurrence count
         */
        public int getCount() {
            return this.count;
        }

    }

    /**
     * Prepare a list of sequences for processing.  This involves converting all
     * of them to upper case.
     *
     * @param seqList	list of sequences of interest
     */
    public static void prepare(Collection<Sequence> seqList) {
        seqList.stream().forEach(x -> x.setSequence(x.getSequence().toUpperCase()));
    }

    /**
     * Erase all the counts.
     */
    public void clear() {
        Arrays.fill(this.counts, 0);
        this.totalChars = 0;
    }

    /**
     * Count a character.
     *
     * @param sequence		sequence containing the character to count
     * @param pos			character position to count (0-based)
     */
    public void count(Sequence seq, int pos) {
        try {
            String sequence = seq.getSequence();
            char target = (pos >= sequence.length() ? '-' : sequence.charAt(pos));
            this.counts[target - MIN_CHAR]++;
            this.totalChars++;
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Invalid character at position " + pos + " in sequence " + seq.getLabel() + ".");
        }
    }

    /**
     * @return the most common non-hyphen character
     */
    public char getMostCommon() {
        int bestI = 0;
        int bestCount = this.counts[0];
        for (int i = 1; i < this.counts.length; i++) {
            if (this.counts[i] > bestCount  && i != ('-' - MIN_CHAR)) {
                bestI = i;
                bestCount = this.counts[i];
            }
        }
        return (char) (bestI + MIN_CHAR);
    }

    /**
     * @return a sorted list of the counts
     */
    public List<Count> getResults() {
        List<Count> retVal = new ArrayList<Count>(10);
        for (int i = 0; i < ARRAY_SIZE; i++) {
            if (this.counts[i] > 0)
                retVal.add(new Count(this, i));
        }
        Collections.sort(retVal);
        return retVal;
    }

    /**
     * @return the total number of characters counted
     */
    protected int getTotalChars() {
        return this.totalChars;
    }

}
