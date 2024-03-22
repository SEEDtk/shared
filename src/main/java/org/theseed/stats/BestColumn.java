/**
 *
 */
package org.theseed.stats;

/**
 * This is a utility class that tracks the maximum value from a numbered sequence.  It reports back
 * the index number associated with the value and the value itself.
 *
 * @author Bruce Parrello
 *
 */
public class BestColumn {

    // FIELDS
    /** index of best value */
    private int bestIdx;
    /** best value */
    private double bestValue;
    /** number of values reported */
    private int count;

    /**
     * Construct a blank best-value object.
     */
    public BestColumn() {
        this.bestIdx = -1;
        this.bestValue = Double.NEGATIVE_INFINITY;
        this.count = 0;
    }

    /**
     * Record a value.
     *
     * @param i		index of this value
     * @param val	value to check
     */
    public void merge(int i, double val) {
        if (this.count == 0 || val > this.bestValue) {
            this.bestValue = val;
            this.bestIdx = i;
        }
        this.count++;
    }

    /**
     * @return the index of the best value, or -1 if the list was empty.
     */
    public int getBestIdx() {
        return this.bestIdx;
    }

    /**
     * @return the best value
     */
    public double getBestValue() {
        return this.bestValue;
    }

}
