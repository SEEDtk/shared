/**
 *
 */
package org.theseed.counters;

import java.util.Arrays;

/**
 * This class computes more complicated statistics for regression.  The constructor specifies the number
 * of items in the testing set, and the predicted and actual values are passed in from each data point.
 * the values are then sorted and we can return the median error, the quartile points, the interquartile
 * range, and the Tukey trimean.
 *
 * @author Bruce Parrello
 *
 */
public class RegressionStatistics {

    // FIELDS
    /** array of error values */
    private double[] errors;
    /** number of data points stored */
    private int count;
    /** first quartile value */
    private double q1;
    /** true median */
    private double q2;
    /** third quartile value */
    private double q3;

    /**
     * Construct a regression statistics object.
     *
     * @param size		number of data points expected
     */
    public RegressionStatistics(int size) {
        this.errors = new double[size];
        this.count = 0;
    }

    /**
     * Add a data point.
     *
     * @param expected		expected value
     * @param predict		predicted value
     */
    public void add(double expected, double predict) {
        this.add(predict - expected);
    }

    /**
     * Add a single-valued data point.
     *
     * @param error			error value
     */
    public void add(double error) {
        if (this.count >= this.errors.length) {
            // Here we need to make the array bigger.
            int newLength = (this.count * 13) / 10;
            double[] newArray = new double[newLength];
            for (int i = 0; i < this.count; i++)
                newArray[i] = this.errors[i];
            this.errors = newArray;
        }
        this.errors[this.count++] = error;
    }

    /**
     * Denote all points have been added.
     */
    public void finish() {
        // Sort the array.  The most extreme negative error will be first and the most extreme positive
        // error will be last.
        Arrays.sort(this.errors, 0, this.count);
        // Get the midpoint index rounded up and rounded down.
        int floor = (this.count - 1) >> 1;
        int ceil = this.count >> 1;
        // Average the values to get the median.
        this.q2 = (this.errors[floor] + this.errors[ceil]) / 2.0;
        // Get the quartile index rounded up and rounded down.
        floor = (this.count - 2) >> 2;
        ceil = this.count >> 2;
        // Average the values to get the first quartile.
        this.q1 = (this.errors[floor] + this.errors[ceil]) / 2.0;
        // Get the third quartile index rounded up and rounded down.
        int length3 = this.count * 3;
        floor = (length3 - 1) >> 2;
        ceil = (length3 + 1) >> 2;
        // Average the values to get the third quartile.
        this.q3 = (this.errors[floor] + this.errors[ceil]) / 2.0;
    }

    /**
     * @return the median error
     */
    public double getQ2() {
        return this.q2;
    }

    /**
     * @return the lower-quartile error
     */
    public double getQ1() {
        return this.q1;
    }

    /**
     * @return the upper-quartile error
     */
    public double getQ3() {
        return this.q3;
    }

    /**
     * @return the Tukey trimean
     */
    public double trimean() {
        return (this.getQ2() + (this.getQ1() + this.getQ3()) / 2.0) / 2.0;
    }

    /**
     * @return the interquartile range
     */
    public double iqr() {
        return (this.getQ3() - this.getQ1());
    }

    /**
     * @return the mean absolute error in the specified trimmed portion of the error list
     *
     * @param reduce	fraction to trim at each end of the error list
     */
    public double trimmedMean(double reduce) {
        // Compute the region limits.
        int lowIdx = (int) Math.floor(reduce * this.count);
        int highIdx = this.count - 1 - lowIdx;
        double min = this.errors[lowIdx];
        double max = this.errors[highIdx];
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < this.count; i++) {
            double error = this.errors[i];
            if (error <= max && error >= min) {
                sum += Math.abs(error);
                count++;
            }
        }
        return sum / count;
    }

    /**
     * Scale the errors by dividing the specified factor into them.
     *
     * @param scaleFactor	factor to divide into each error value
     */
    public void scale(double scaleFactor) {
        for (int i = 0; i < this.count; i++)
            this.errors[i] /= scaleFactor;
    }

}
