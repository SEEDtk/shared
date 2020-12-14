/**
 *
 */
package org.theseed.utils;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * THis is a utility class that manages a comma-delimited list of floating-point numbers input as a string from the
 * command line (similar to IntegerList).  There are methods to parse out the numbers and to display them in a more
 * user-friendly manner.
 *
 * @author Bruce Parrello
 *
 */
public class FloatList implements Iterable<Double> {

    // FIELDS
    /** array of the numbers found */
    double[] values;
    /** current position in the array */
    int pos;

    /**
     * Construct a number list from a comma-delimited string.
     *
     * @param inString	comma-delimited list of flkoating-point expressed as a string
     */
    public FloatList(String inString) {
        this.values = Arrays.stream(StringUtils.split(inString, ',')).mapToDouble(Double::parseDouble).toArray();
        this.pos = 0;
    }

    /**
     * Construct a number list from an array.
     *
     * @param integers	array of numbers to use
     */
    public FloatList(double... doubles) {
        this.values = ArrayUtils.clone(doubles);
        this.pos = 0;
    }

    /**
     * Construct a number list using a single value.
     *
     * @param value		value with which to fill the list
     * @param size		number of copies to put in the list
     */
    public FloatList(double value, int size) {
        this.values = new double[size];
        for (int i = 0; i < size; i++)
            this.values[i] = value;
        this.pos = 0;
    }

    /**
     * Construct an empty floating-point list.
     */
    public FloatList() {
        this.values = new double[0];
        this.pos = 0;
    }

    /**
     * @return the number of numbers in the list
     */
    public int size() {
        return this.values.length;
    }

    /**
     * @return TRUE if the number list is empty
     */
    public boolean isEmpty() {
        return this.values.length == 0;
    }

    /**
     * Store a default value if the list is empty.
     *
     * @param defValue	value to store if the list is empty
     */
    public void setDefault(double defValue) {
        if (this.values.length == 0)
            this.values = new double[] { defValue };
    }

    /**
     * @return the number at the specified location, or 0 if the location is invalid
     *
     * @param i		index of the desired value
     */
    public double get(int i) {
        return ((i < 0 || i >= this.values.length) ? 0 : this.values[i]);
    }

    /**
     * @return the first number in the list
     */
    public double first() {
        this.pos = 1;
        return this.values[0];
    }

    /**
     * @return the next number in the list
     */
    public double next() {
        return this.get(this.pos++);
    }

    /**
     * @return 	the next number in the list unless we are at the end, in which
     * 			case we return the last
     */
    public double softNext() {
        return (this.hasNext() ? this.next() : this.last());
    }

    /**
     * @return the last number in the list
     */
    public double last() {
        return this.get(this.values.length - 1);
    }

    /**
     * Reset the list to the beginning.
     */
    public void reset() {
        this.pos = 0;
    }

    /**
     * @return TRUE if there are more numbers in the list
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

    /**
     * @return the underlying array of doubles
     */
    public double[] getValues() {
        return this.values;
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
    public Iterator<Double> iterator() {
        return this.new Iter();
    }

    /** Integer iterator for this class */
    public class Iter implements Iterator<Double> {

        public Iter() {
            FloatList.this.reset();
        }

        @Override
        public boolean hasNext() {
            return FloatList.this.hasNext();
        }

        @Override
        public Double next() {
            return FloatList.this.next();
        }


    }

    /**
     * @return an array of the values, expanded to the specified size
     *
     * @param size	size of the array to return
     */
    public double[] getValues(int size) {
        double[] retVal = new double[size];
        this.reset();
        for (int i = 0; i < size; i++)
            retVal[i] = this.softNext();
        return retVal;
    }

}
