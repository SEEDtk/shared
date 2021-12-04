/**
 *
 */
package org.theseed.reports;

/**
 * This object tracks the minimum and maximum of a range and uses the information to normalize values
 * between 0 and 1.
 *
 * The values initially presented must represent the entire range of possible inputs.  If we attempt
 * to normalize a value outside the range, the results will be unexpected.
 *
 * @author Bruce Parrello
 *
 */
public class RangeNormalizer {

    // FIELDS
    /** minimum value found so far */
    private double min;
    /** maximum value found so far */
    private double max;
    /** total range of value */
    private double range;
    /** number of values absorbed */
    private int count;

    /**
     * Initialize the range normalizer.
     */
    public RangeNormalizer() {
        this.range = 0.0;
        this.count = 0;
    }

    /**
     * Add a value.
     *
     * @param value		value to add to this object
     */
    public void addElement(double value) {
        if (this.count == 0) {
            this.min = value;
            this.max = value;
        } else if (value < this.min) {
            this.min = value;
            this.range = this.max - value;
        } else if (value > this.max) {
            this.max = value;
            this.range = value - this.min;
        }
        this.count++;
    }

    /**
     * @return a normalized value
     *
     * @param value		value to normalize
     */
    public double normalize(double value) {
        double retVal = 0.0;
        if (this.range > 0.0)
            retVal = (value - this.min) / this.range;
        return retVal;
    }

    /**
     * @return a normalized difference between two values
     *
     * @param val1		first value
     * @param val2		second value
     */
    public double difference(double val1, double val2) {
        double retVal = 0.0;
        if (this.range > 0.0)
            retVal = (val1 - val2) / this.range;
        return retVal;
    }

    /**
     * @return the minimum value
     */
    public double getMin() {
        return this.min;
    }

    /**
     * @return the maximum value
     */
    public double getMax() {
        return this.max;
    }

    /**
     * @return the total width of the range
     */
    public double getRange() {
        return this.range;
    }

    /**
     * @return the number of values processed
     */
    public int getCount() {
        return this.count;
    }

}
