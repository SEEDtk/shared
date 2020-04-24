/**
 *
 */
package org.theseed.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

/**
 * This is a utility class for managing parameter lists.  It provides methods to load
 * them from a file and fluent methods for building a list.
 *
 * @author Bruce Parrello
 *
 */
public class Parms {

    // FIELDS
    /** map of parameters to values */
    private SortedMap<String, String> binary;
    /** set of options */
    private SortedSet<String> unary;

    /**
     * This is a special comparator that sorts single-hyphen parameters in front of double-hyphen ones.
     */
    private class Compare implements Comparator<String>, Cloneable {

        @Override
        public int compare(String o1, String o2) {
            int retVal = o1.compareTo(o2);
            if (o1.charAt(1) == '-' || o1.length() > 2) {
                if (o2.charAt(1) != '-' && o2.length() == 2)
                    retVal = 1;
            } else if (o2.charAt(1) != '-' || o2.length() > 2)
                retVal = -1;
            return retVal;
        }

    }

    /**
     * @return 	a list of strings that can be fed to a command parser to interpret the
     * 			command-line options found in the specified file
     *
     * @param inFile	input file containing the parameters
     *
     * @throws IOException
     */
    public static List<String> fromFile(File inFile) throws IOException {
        Parms parms = new Parms(inFile);
        return parms.get();
    }

    /**
     * Construct a blank parameter list.
     */
    public Parms() {
        init();
        setDefaults();
    }

    /**
     * Initialize the object fields.
     */
    private void init() {
        Comparator<String> compare = new Compare();
        this.binary = new TreeMap<String, String>(compare);
        this.unary = new TreeSet<String>(compare);
    }

    /**
     * Read a parameter list from a file.
     *
     * @param inFile	file containing the parameters
     *
     * @throws IOException
     */
    public Parms(File inFile) throws IOException {
        init();
        setDefaults();
        readFromFile(inFile);
    }

    /**
     * Set the default values of parameters.
     */
    protected void setDefaults() { }

    /**
     * Read a parameter list from a file.
     *
     * @param inFile	file containing the parameters
     *
     * @throws IOException
     */
    protected void readFromFile(File inFile) throws IOException {
        // Get a scanner for the file.
        try (Scanner inStream = new Scanner(inFile)) {
            while (inStream.hasNext()) {
                // Get the option name.
                String option = inStream.next();
                // Get the option value.  This may be an empty string.  Nextline gives us the
                // separating whitespace as well as the rest of the line, so we will need to
                // strip it.
                String value = inStream.nextLine();
                // Only proceed if this is not a comment.
                if (option.charAt(0) != '#') {
                    // We need to check the value.  Strip off the comment (if any), and
                    // trim the leading and trailing whitespace.
                    value = StringUtils.trimToEmpty(StringUtils.substringBefore(value,  "#"));
                    // If the value is nonempty, we have a value parameter.
                    if (! value.isEmpty())
                        this.set(option, value);
                    else
                        this.set(option);
                }
            }
        }
    }

    /**
     * Add an option parameter.
     *
     * @param name	parameter name
     */
    public Parms set(String option) {
        this.unary.add(option);
        return this;
    }

    /**
     * Add a string parameter.
     *
     * @param name		parameter name
     * @param value		parameter value
     */
    public Parms set(String option, String value) {
        this.binary.put(option, value);
        return this;
    }

    /**
     * Add a floating-point parameter.
     *
     * @param name		parameter name
     * @param value		parameter value
     */
    public Parms set(String option, double value) {
        this.binary.put(option, Double.toString(value));
        return this;
    }

    /**
     * Add an integer parameter.
     *
     * @param name		parameter name
     * @param value		parameter value
     */
    public Parms set(String option, int value) {
        this.binary.put(option, Integer.toString(value));
        return this;
    }

    /**
     * @return the parameters as a string list.
     */
    public List<String> get() {
        List<String> retVal = new ArrayList<String>(this.binary.size() * 2 + this.unary.size());
        for (String option : this.unary)
            retVal.add(option);
        for (Map.Entry<String, String> parm : this.binary.entrySet()) {
            retVal.add(parm.getKey());
            retVal.add(parm.getValue());
        }
        return retVal;
    }

    /**
     * @return a string representation of the parameters
     */
    @Override
    public String toString() {
        StringBuilder retVal = new StringBuilder(this.binary.size() * 30 + this.unary.size() * 10);
        if (! this.unary.isEmpty()) {
            retVal.append(StringUtils.join(this.unary, ' '));
            if (! this.binary.isEmpty())
                retVal.append(' ');
        }
        if (! this.binary.isEmpty()) {
            for (Map.Entry<String, String> parm : this.binary.entrySet()) {
                retVal.append(parm.getKey());
                retVal.append(' ');
                String value = parm.getValue();
                if (StringUtils.containsAny(value, ' ', '<', '>', '|')) {
                    // Here we have an internal special character so we have to quote the string.
                    value = "\"" + StringUtils.replace(value, "\"", "\\\"") + "\"";
                }
                retVal.append(value);
                retVal.append(' ');
            }
            // Remove the trailing space.
            retVal.deleteCharAt(retVal.length() - 1);
        }
        return retVal.toString();
    }

    /**
     * @return a safe copy of this object
     */
    @Override
    public Parms clone() {
        Parms retVal = new Parms();
        copyValues(retVal);
        return retVal;
    }

    /**
     * @param retVal
     */
    protected void copyValues(Parms retVal) {
        for (Map.Entry<String,String> entry : this.binary.entrySet())
            retVal.binary.put(entry.getKey(), entry.getValue());
        for (String parm : this.unary)
            retVal.unary.add(parm);
    }

}
