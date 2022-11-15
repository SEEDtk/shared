/**
 *
 */
package org.theseed.counters;

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains a method for computing correlation variance between two series.  Each series is scaled
 * so the minimum is 0 and the maximum is 1. The absolute difference between the scaled results is called the
 * error.  The output of the method is a trimean of the error.
 *
 * @author Bruce Parrello
 *
 */
public class CorrelationVariance {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(CorrelationVariance.class);
    /** statistics object for the last error values passed in */
    private DescriptiveStatistics stats;

    /**
     * This is a utility class that provides scaling information for a series.
     */
    private static class Scaler {

        /** zero value for series (minimum) */
        private double min;
        /** unit value for series (maximum) */
        private double max;
        /** scale factor for series (maximum - minimum) */
        private double scale;

        /**
         * Construct a scaling object for a series.
         *
         * @param series	series to scale
         */
        public Scaler(double[] series) {
            this.min = series[0];
            this.max = series[0];
            Arrays.stream(series).forEach(x -> this.process(x));
            if (this.min == this.max) {
                // If the entire series is a single value, collapse everything to 0.5.
                this.min -= 0.5;
                this.scale = 1.0;
            } else {
                // Compute the scale factor.
                this.scale = (this.max - this.min);
            }
        }

        /**
         * Update the stored minimum and maximum.
         *
         * @param x		new value to check
         */
        private void process(double x) {
            if (x > this.max) this.max = x;
            if (x < this.min) this.min = x;
        }

        /**
         * @return the scaled value of a series element
         *
         * @param x		value to scale
         */
        public double apply(double x) {
            return (x - this.min) / scale;
        }

    }

    /**
     * Construct an empty correlation variance object.
     */
    public CorrelationVariance() {
        this.stats = new DescriptiveStatistics();
    }

    /**
     * Initialize this object to contain the variance information for a pair of result series.
     * The two series must be the same length.
     *
     * @param series1		first series
     * @param series2		second series
     *
     * @return the trimean of the absolute scaled error between the results at each point
     */
    public double variation(double[] series1, double[] series2) {
        // Erase any previous results.
        this.stats.clear();
        // Verify the lengths.
        if (series1.length != series2.length)
            throw new IllegalArgumentException("Cannot correlate two series of different lengths.");
        if (series1.length > 0) {
            // Now we can get serious. Compute scalers for both series.
            Scaler scale1 = new Scaler(series1);
            Scaler scale2 = new Scaler(series2);
            // Feed the error values to the statistics object.
            for (int i = 0; i < series1.length; i++) {
                double error = Math.abs(scale1.apply(series1[i]) - scale2.apply(series2[i]));
                this.stats.addValue(error);
            }
        }
        // Return the trimean.
        return this.getTrimean();
    }

    /**
     * @return the mean average error from the last series pair
     */
    public double getMAE() {
        double retVal;
        if (this.stats.getN() < 1)
            retVal = 0.0;
        else
            retVal = this.stats.getMean();
        return retVal;
    }

    /**
     * @return the standard error deviation from the last series pair
     */
    public double getStandardDeviation() {
        double retVal;
        if (this.stats.getN() < 1)
            retVal = 0.0;
        else
            retVal = this.stats.getStandardDeviation();
        return retVal;
    }

    /**
     * @return the mean squared error from the last series pair
     */
    public double getMSE() {
        double retVal;
        final long n = this.stats.getN();
        if (n < 1)
            retVal = 0.0;
        else {
            double num = this.stats.getSumsq();
            retVal = num / n;
        }
        return retVal;
    }

    /**
     * @return the trimean of the error from the last series pair
     */
    public double getTrimean() {
        double retVal;
        final long n = this.stats.getN();
        if (n < 1)
            retVal = 0.0;
        else if (n < 4)
            retVal = this.stats.getMean();
        else
            retVal = (this.stats.getPercentile(50.0) + (this.stats.getPercentile(25.0) + this.stats.getPercentile(75.0)) / 2.0) / 2.0;
        return retVal;
    }

    /**
     * @return the inter-quartile range of the error from the last series
     */
    public double getIQR() {
        double retVal;
        final long n = this.stats.getN();
        if (n < 2)
            retVal = 0.0;
        else
            retVal = this.stats.getPercentile(75.0) - this.stats.getPercentile(25.0);
        return retVal;
    }

    /**
     * @return the number of data points in the last series pair
     */
    public int size() {
        return (int) this.stats.getN();
    }

}
