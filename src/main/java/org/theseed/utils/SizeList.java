/**
 *
 */
package org.theseed.utils;

/**
 * This simple class returns a list of integers given a minimum, a maximum, and a step.
 * The maximum is always included even if it is not a multiple of the step size from the
 * minimum.
 *
 * @author Bruce Parrello
 *
 */
public class SizeList {

    /**
     * @return an array of sizes ranging from the minimum to the maximum
     *
     * @param min	first size to return
     * @param max	last size to return
     * @param step	step between sizes
     */
    public static int[] getSizes(int min, int max, int step) {
        // Compute the number of stepped sizes that will be returned.  Note that this
        // does not include the minimum.
        int dim = (max - min) / step;
        // Add space for the maximum if it is not on a step boundary.
        if (min + dim * step < max) dim++;
        // Create the return array.
        int[] retVal = new int[dim + 1];
        // Fill in the array.  If the maximum is not on a step boundary, the last
        // element will be wrong.
        retVal[0] = min;
        for (int i = 1; i <= dim; i++) retVal[i] = retVal[i-1] + step;
        // Insure the last element is correct.
        if (retVal[dim] != max) retVal[dim] = max;
        return retVal;
    }

}
