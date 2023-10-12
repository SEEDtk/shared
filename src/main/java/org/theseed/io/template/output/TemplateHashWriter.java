/**
 *
 */
package org.theseed.io.template.output;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This template writer saves the template output in a hash that can later
 * be interrogated by the $include directive.
 */
public class TemplateHashWriter implements ITemplateWriter {

    // FIELDS
    /** master hash -- fileName -> key -> string */
    private Map<String, Map<String, String>> masterHash;

    /**
     * Construct a template hash writer.
     */
    public TemplateHashWriter() {
        // We expect few file names, so we use a tree map at the high level.
        this.masterHash = new TreeMap<String, Map<String, String>>();
    }

    @Override
    public void write(String fileName, String key, String outString) throws IOException {
        // Get the sub-hash for this file.
        Map<String, String> subHash = this.masterHash.computeIfAbsent(key, x -> new HashMap<String, String>());
        // Store the template string, using the provided key.
        subHash.put(key, outString);
    }

    /**
     * @return the template string for the specifed file name and key, or the empty string if none exists
     *
     * @param fileName	name of the input file for the desired string
     * @param key		key value of the desired string
     */
    public String getString(String fileName, String key) {
        Map<String, String> subHash = this.masterHash.get(fileName);
        String retVal;
        if (subHash == null)
            retVal = "";
        else
            retVal = subHash.getOrDefault(key, "");
        return retVal;
    }

    @Override
    public void close() {
        // This is an in-memory structure.  No action is needed.
    }

}
