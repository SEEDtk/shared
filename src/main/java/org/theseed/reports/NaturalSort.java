/**
 *
 */
package org.theseed.reports;

import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;

/**
 * This comparator performs a natural sort of strings that end in digits.
 *
 * @author Bruce Parrello
 *
 */
public class NaturalSort implements Comparator<String> {

    private String[] p = new String[2];
    private String[] n = new String[2];

    @Override
    public int compare(String o1, String o2) {
        this.separate(o1, 0);
        this.separate(o2, 1);
        int retVal = p[0].compareTo(p[1]);
        if (retVal == 0) {
            if (n[0].isEmpty()) {
                if (! n[1].isEmpty())
                    retVal = -1;
        } else if (n[1].isEmpty()) {
            retVal = 1;
        } else
            retVal = Integer.valueOf(n[0]) - Integer.valueOf(n[1]);
        }
        return retVal;
    }

    /**
     * Separate a string into its char and number parts.
     *
     * @param o		string to separate
     * @param idx	array index for storing result
     */
    private void separate(String o, int idx) {
        int i = o.length() - 1;
        while (i >= 0 && Character.isDigit(o.charAt(i))) i--;
        i++;
        // Now "i" points to the first digit in the tail.
        p[idx] = StringUtils.substring(o, 0, i);
        n[idx] = StringUtils.substring(o, i);
    }

}
