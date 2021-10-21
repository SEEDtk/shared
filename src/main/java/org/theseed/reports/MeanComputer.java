/**
 *
 */
package org.theseed.reports;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is the base class for computing the mean of a list of observations.  Each subclass supports a different algorithm for
 * excluding outliers.
 *
 *  * @author Bruce Parrello
 *
 */
public abstract class MeanComputer {

    /**
     * This enum defines the types of mean computations supported.
     */
    public static enum Type {
        SIGMA2 {
            public MeanComputer create() { return new MeanComputer.Sigma(2); }
        },
        SIGMA1 {
            public MeanComputer create() { return new MeanComputer.Sigma(1); }
        },
        MIDDLE {
            public MeanComputer create() { return new MeanComputer.Middle(); }
        },
        TRIMEAN {
            public MeanComputer create() { return new MeanComputer.Trimean(); }
        },
        MAX {
            public MeanComputer create() { return new MeanComputer.Max(); }
        };

        public abstract MeanComputer create();
    }

    /**
     * Compute an error-corrected mean for a set of numbers.
     *
     * @param nums			list of input numbers
     * @param goodLevels	bitmap of good values in the input numbers
     *
     * @return the bias-corrected mean
     */
    public abstract double goodMean(List<Double> nums, BitSet goodLevels);

    /**
     * Compute an error-corrected mean for a set of good numbers.
     *
     * @param nums			list of input numbers
     *
     * @return the bias-corrected mean
     */
    public double goodMean(List<Double> nums) {
        return goodMean(nums, null);
    }

    /**
     * @return a list of the good values in the specified list
     *
     * @param nums			list of input numbers
     * @param goodLevels	bitmap of good values in the input numbers, or NULL if all are good
     */
    public static List<Double> getGood(List<Double> nums, BitSet goodLevels) {
        List<Double> retVal = nums;
        if (goodLevels != null)
            retVal = IntStream.range(0, nums.size()).filter(i -> goodLevels.get(i))
                    .mapToObj(i -> nums.get(i)).collect(Collectors.toList());
        return retVal;
    }

    /**
     * This class computes the trimean, that is, the weighted average of the median and the upper
     * and lower quartiles.
     */
    public static class Trimean extends MeanComputer {

        @Override
        public double goodMean(List<Double> nums, BitSet goodLevels) {
            double retVal = 0.0;
            nums = getGood(nums, goodLevels);
            if (nums.size() == 1)
                retVal = nums.get(0);
            else if (nums.size() == 2)
                retVal = (nums.get(0) + nums.get(1)) / 2.0;
            else if (nums.size() > 2) {
                double[] sorted = nums.stream().mapToDouble(x -> x).sorted().toArray();
                // Sort the array.  The most extreme negative error will be first and the most extreme positive
                // error will be last.
                Arrays.sort(sorted, 0, sorted.length);
                // Get the midpoint index rounded up and rounded down.
                int floor = (sorted.length - 1) >> 1;
                int ceil = sorted.length >> 1;
                if (floor < 0 || ceil < 0)
                    System.err.println("Error case.");
                // Average the values to get the median.
                double q2 = (sorted[floor] + sorted[ceil]) / 2.0;
                // Get the quartile index rounded up and rounded down.
                floor = (sorted.length - 2) >> 2;
                ceil = sorted.length >> 2;
                // Average the values to get the first quartile.
                double q1 = (sorted[floor] + sorted[ceil]) / 2.0;
                // Get the third quartile index rounded up and rounded down.
                int length3 = sorted.length * 3;
                floor = (length3 - 1) >> 2;
                ceil = (length3 + 1) >> 2;
                // Average the values to get the third quartile.
                double q3 = (sorted[floor] + sorted[ceil]) / 2.0;
                // Compute the trimean.
                retVal = q2 / 2.0 + (q1 + q3) / 4.0;
            }
            return retVal;
        }

    }


    /**
     * This subclass removes the minimum and maximum values from the incoming number list,
     * and computes the mean from the remainder.
     */
    public static class Middle extends MeanComputer {

        @Override
        public double goodMean(List<Double> nums, BitSet goodLevels) {
            double retVal = 0.0;
            nums = getGood(nums, goodLevels);
            if (nums.size() == 1)
                retVal = nums.get(0);
            else if (nums.size() == 2)
                retVal = (nums.get(0) + nums.get(1)) / 2.0;
            else {
                double min = nums.get(0);
                double max = nums.get(0);
                retVal = nums.get(0);
                for (int i = 1; i < nums.size(); i++) {
                    double num = nums.get(i);
                    retVal += num;
                    if (num > max) max = num;
                    if (num < min) min = num;
                }
                if (max != min) {
                    retVal -= (max + min);
                    retVal /= (nums.size() - 2);
                }
            }
            return retVal;
        }

    }

    /**
     * This subclass removes values more than N standard deviations away from the mean, where N is
     * a construction parameter.
     */
    public static class Sigma extends MeanComputer {

        // FIELDS
        /** standard deviation distance for outliers */
        private final double nSigma;

        /**
         * Construct a sigma-based mean computer.
         *
         * @param n		number of standard deviations beyond which a value is considered an outlier
         */
        public Sigma(double n) {
            this.nSigma = n;
        }

        /**
         * Compute an error-corrected mean for a set of numbers.  The raw mean and standard deviation are computed.
         * Values outside the N-sigma range are thrown out, and the error-corrected mean is computed from the
         * result.
         *
         * @param nums		list of input numbers
         *
         * @return the bias-corrected mean
         */
        @Override
        public double goodMean(List<Double> nums, BitSet goodLevels) {
            double retVal = 0.0;
            nums = getGood(nums, goodLevels);
            if (nums.size() == 1)
                retVal = nums.get(0);
            else if (nums.size() > 1) {
                double sum = 0.0;
                double sqSum = 0.0;
                for (double val : nums) {
                    sum += val;
                    sqSum += val * val;
                }
                double mean = sum / nums.size();
                double stdv = Math.sqrt((sqSum - mean * mean) / nums.size());
                double min = mean - this.nSigma * stdv;
                double max = mean + this.nSigma * stdv;
                // Now compute the mean for the values inside the 6-sigma range.
                int count = 0;
                for (double val : nums) {
                    if (val >= min && val <= max) {
                        retVal += val;
                        count++;
                    }
                }
                retVal /= (double) count;
            }
            return retVal;
        }

    }

    /**
     * This subclass simply takes the maximum value as the mean.
     */
    public static class Max extends MeanComputer {

        @Override
        public double goodMean(List<Double> nums, BitSet goodLevels) {
            double retVal = 0.0;
            Optional<Double> result = getGood(nums, goodLevels).stream().max(Comparator.naturalOrder());
            if (result.isPresent())
                retVal = result.get();
            return retVal;
        }
    }

}
