/**
 *
 */
package org.theseed.reports;

import java.util.Comparator;

/**
 * This is a simple comparator for arrays of strings.  In most cases the arrays will be the same
 * length.
 *
 * @author Bruce Parrello
 *
 */
public class StringTupleSort implements Comparator<String[]> {

    @Override
    public int compare(String[] o1, String[] o2) {
        final int n = Math.min(o1.length, o2.length);
        int retVal = 0;
        for (int i = 0; i < n && retVal == 0; i++)
            retVal = o1[i].compareTo(o2[i]);
        // If we are equal so far, the shortest array sorts first.
        if (retVal == 0)
            retVal = o1.length - o2.length;
        return retVal;
    }

}
