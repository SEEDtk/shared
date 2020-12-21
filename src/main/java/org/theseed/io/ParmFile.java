/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object manages a parameter file for editing.  Each parameter is keyed by name.  There is a flag to indicate
 * whether or not it is commented out, and fields for the parameter values, the comment, and any comment that
 * precedes the parameter.
 *
 * @author Bruce Parrello
 *
 */
public class ParmFile implements Iterable<ParmDescriptor> {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ParmFile.class);
    /** input line pattern */
    public static final Pattern LINE_PATTERN = Pattern.compile("(# )?--(\\S+) *(?:([^\\t]+))?(?:\\s*\\t#\\s+(.+))?");
    /** parameter map */
    private SortedMap<String, ParmDescriptor> parmMap;

    /**
     * Construct this object from a file.
     *
     * @param inFile	file containing the parameters
     *
     * @throws IOException
     */
    public ParmFile(File inFile) throws IOException {
        // Create the parm map.
        this.parmMap = new TreeMap<String, ParmDescriptor>();
        // Allocate a buffer for the line comments.
        List<String> buffer = new ArrayList<String>(2);
        // Loop through the file.
        try (LineReader inStream = new LineReader(inFile)) {
            for (String line : inStream) {
                Matcher m = LINE_PATTERN.matcher(line);
                if (! m.matches())
                    buffer.add(line);
                else {
                    // Here we have a real parameter.
                    ParmDescriptor desc = new ParmDescriptor(m, buffer);
                    this.parmMap.put(desc.getName(), desc);
                    buffer.clear();
                }
            }
        }
    }

    /**
     * @return the parameter with the given name
     *
     * @param name	name of desired parameter
     */
    public ParmDescriptor get(String name) {
        return this.parmMap.get(name);
    }

    /**
     * Add a parameter to the parm file.
     *
     * @param descriptor	descriptor of new parameter to add
     */
    public void add(ParmDescriptor descriptor) {
        this.parmMap.put(descriptor.getName(), descriptor);
    }

    @Override
    public Iterator<ParmDescriptor> iterator() {
        return this.parmMap.values().iterator();
    }

    /**
     * @return the number of parameters in this file
     */
    public int size() {
        return this.parmMap.size();
    }

    /**
     * Save the parameters to the specified file.
     *
     * @throws IOException
     */
     public void save(File outFile) throws IOException {
         try (PrintWriter writer = new PrintWriter(outFile)) {
             for (ParmDescriptor desc : this) {
                 writer.println(desc.toString());
             }
         }
     }

}
