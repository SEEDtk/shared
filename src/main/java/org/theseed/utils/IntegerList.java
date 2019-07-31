/**
 *
 */
package org.theseed.utils;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

/**
 * THis is a utility class that manages a comma-delimited list of numbers input as a string from the
 * command line.  There are methods to parse out the numbers and to display them in a more user-friendly
 * manner.
 *
 * @author Bruce Parrello
 *
 */
public class IntegerList implements Iterable<Integer> {

    // FIELDS
    /** array of the integers found */
    int[] values;
    /** current position in the array */
    int pos;

    /**
     * Construct an integer list from a comma-delimited string.
     *
     * @param inString	comma-delimited list of integers expressed as a string
     */
    public IntegerList(String inString) {
        this.values = Arrays.stream(StringUtils.split(inString, ',')).mapToInt(Integer::parseInt).toArray();
        this.pos = 0;
    }

    /**
     * Construct an empty integer list.
     */
    public IntegerList() {
        this.values = new int[0];
        this.pos = 0;
    }

    /**
     * @return the number of integers in the list
     */
    public int size() {
        return this.values.length;
    }

    /**
     * @return TRUE if the integer list is empty
     */
    public boolean isEmpty() {
        return this.values.length == 0;
    }

    /**
     * Store a default value if the list is empty.
     *
     * @param defValue	value to store if the list is empty
     */
    public void setDefault(int defValue) {
        if (this.values.length == 0)
            this.values = new int[] { defValue };
    }

    /**
     * @return the integer at the specified location, or 0 if the location is invalid
     *
     * @param i		index of the desired value
     */
    public int get(int i) {
        return ((i < 0 || i >= this.values.length) ? 0 : this.values[i]);
    }

    /**
     * @return the first integer in the list
     */
    public int first() {
        this.pos = 1;
        return this.values[0];
    }

    /**
     * @return the next integer in the list
     */
    public int next() {
        return this.get(this.pos++);
    }

    /**
     * @return 	the next integer in the list unless we are at the end, in which
     * 			case we return the last
     */
    public int softNext() {
    	return (this.hasNext() ? this.next() : this.last());
    }

    /**
     * @return the last integer in the list
     */
    public int last() {
        return this.get(this.values.length - 1);
    }

    /**
     * Reset the list to the beginning.
     */
    public void reset() {
        this.pos = 0;
    }

    /**
     * @return TRUE if there are more integers in the list
     */
    public boolean hasNext() {
        return this.pos < this.values.length;
    }

    /**
     * @return the list in its original input form
     */
    public String original() {
        return StringUtils.join(this.values, ',');
    }



    @Override
    public String toString() {
        StringBuilder retVal = new StringBuilder(this.values.length * 8);
        if (this.values.length >= 1)
            retVal.append(this.values[0]);
        if (this.values.length >= 2) {
            for (int i = 1; i < this.values.length; i++) {
                retVal.append(", ");
                retVal.append(this.values[i]);
            }
        }
        return retVal.toString();
    }

    @Override
    public Iterator<Integer> iterator() {
        return this.new Iter();
    }

    /** Integer iterator for this class */
    public class Iter implements Iterator<Integer> {

        public Iter() {
            IntegerList.this.reset();
        }

        @Override
        public boolean hasNext() {
            return IntegerList.this.hasNext();
        }

        @Override
        public Integer next() {
            return IntegerList.this.next();
        }


    }

}
