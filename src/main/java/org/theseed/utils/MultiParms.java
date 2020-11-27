/**
 *
 */
package org.theseed.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class reads a parameter files and converts them into String arrays.
 *
 * In the simple case, each line of the file should consist of a command-line option
 * (with the preceding hyphen or hyphens), one or more spaces, and then the parameter
 * value (if any).  Anything after a pound sign (#) in the value area is treated as a
 * comment.  A pound sign in the first column also counts as a comment.
 *
 * In the more complex case, multiple parameter configurations can be specified in a single file.
 * For this, the multiple values should be delimited by a comma followed by a single space.
 * Again, a pound sign indicates a comment.  An instance of this object will be passed back that
 * functions as an iterator which transmits the individual option arrays.
 *
 *
 *
 * @author Bruce Parrello
 *
 */
public class MultiParms implements Iterator<List<String>> {

    // FIELDS

    /** array of boolean options */
    private ArrayList<String> switches;
    /** TRUE if we are done iterating */
    private boolean done;
    /** map of variable parameters to current values */
    private HashMap<String, String> varMap;

    // All of these arrays run in parallel

    /** array of option names */
    private ArrayList<String> parmNames;
    /** array of permissible value lists */
    private ArrayList<String[]> parmValues;
    /** array of current positions; this is the next position to return, and we increment afterward */
    private int[] positions;

    /**
     * Construct an iterator through the parameter combinations encoded in a parameter file.
     *
     * @param inFile	input file containing the parameters, with multiple options comma-delimited
     *
     * @throws IOException
     */
    public MultiParms(File inFile) throws IOException {
        // Initialize the fields.
        this.parmNames = new ArrayList<String>(20);
        this.parmValues = new ArrayList<String[]>(20);
        this.switches = new ArrayList<String>(20);
        this.varMap = new HashMap<String, String>(20);
        // Get a scanner for the file.
        Scanner inStream = new Scanner(inFile);
        try {
            // Now we loop through the list, storing the string lists and the option names.
            while (inStream.hasNext()) {
                // Get the option name.
                String option = inStream.next();
                // Get the option value.  This may be an empty string.  Nextline gives us the
                // separating whitespace as well as the rest of the line, so we will need to
                // strip it later.
                String value = inStream.nextLine();
                // Only proceed if this is not a comment.
                if (option.charAt(0) != '#') {
                    // Verify that we have a real option.
                    if (option.charAt(0) != '-')
                        throw new IllegalArgumentException("Positional parameters are not allowed in multi-parameter files.");
                    // We need to check the value.  Strip off the comment (if any), and
                    // trim the leading and trailing whitespace.
                    value = StringUtils.trimToEmpty(StringUtils.substringBefore(value, "#"));
                    // If the value is empty, this is a switch.
                    if (value.isEmpty()) {
                        this.switches.add(option);
                    } else {
                        // Here we have an option with one or more values.
                        String[] possibilities = StringUtils.splitByWholeSeparator(value, ", ");
                        this.parmNames.add(option);
                        this.parmValues.add(possibilities);
                        // If this is a varying parameter, remember its name.
                        if (possibilities.length > 1)
                            this.varMap.put(option, "");
                    }
                }
            }
        } finally {
            inStream.close();
        }
        // Now initialize the current position.
        this.done = false;
        this.positions = new int[this.parmNames.size()];
        Arrays.fill(this.positions, 0);
    }

    /**
     * Replace a single-valued parameter with another single value.
     *
     * @param name		parameter name
     * @param value		new parameter value
     */
    public void replace(String name, String value) {
        if (this.varMap.containsKey(name))
            throw new IllegalArgumentException("Attempt to modify varying parameter " + name + ".");
        int idx = this.parmNames.indexOf(name);
        String[] values = new String[] { value };
        if (idx >= 0) {
            // Here we are doing a straight replace.
            this.parmValues.set(idx, values);
        } else {
            // Here we are adding a new parameter.
            this.parmNames.add(name);
            this.parmValues.add(values);
            this.positions = ArrayUtils.add(this.positions, 0);
        }
    }

     @Override
    public boolean hasNext() {
        return ! this.done;
    }

    @Override
    public List<String> next() {
        List<String> retVal = null;
        if (! this.done) {
            retVal = new ArrayList<String>(this.parmNames.size() * 2 + this.switches.size() + 5);
            // Accumulate the current values, remembering the varying ones.
            for (int i = 0; i < this.parmNames.size(); i++) {
                String option = this.parmNames.get(i);
                String value = this.parmValues.get(i)[this.positions[i]];
                retVal.add(option);
                retVal.add(value);
                if (this.varMap.containsKey(option))
                    this.varMap.put(option, value);
            }
            // Add the switches.
            retVal.addAll(this.switches);
            // Now we position for the next round.
            this.done = true;
            int currentIdx = this.parmNames.size() - 1;
            while (this.done && currentIdx >= 0) {
                this.positions[currentIdx]++;
                if (this.positions[currentIdx] >= parmValues.get(currentIdx).length) {
                    this.positions[currentIdx] = 0;
                    currentIdx--;
                } else {
                    this.done = false;
                }
            }
        }
        return retVal;
    }

    /**
     * @return the number of strings expected from this iterator
     */
    public int size() {
        return this.parmNames.size() * 2 + this.switches.size();
    }

    /**
     * @return a map of the varying-option names to their current values
     */
    public HashMap<String,String> getVariables() {
        return this.varMap;
    }

    /**
     * @return a representation of this iteration
     */
    @Override
    public String toString() {
        ArrayList<String> retVal = new ArrayList<String>(20);
        // To insure the option names are in the correct order, we iterate through
        // the parmNames array.
        for (String option : parmNames)
            if (this.varMap.containsKey(option))
                retVal.add(option + " " + this.varMap.get(option));
        return StringUtils.join(retVal, "; ");
    }

    /**
     * @return an array of the varying-option names
     */
    public String[] getOptions() {
        // Copy the option names in presentation order into an output array.
        String[] retVal = new String[this.varMap.size()];
        int i = 0;
        for (String option : parmNames)
            if (this.varMap.containsKey(option)) {
                retVal[i] = option;
                i++;
            }
        return retVal;
    }



}
