/**
 *
 */
package org.theseed.io.template.output;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This template writer saves the template output in a hash that can later
 * be interrogated by the $include directive.
 */
public class TemplateHashWriter implements ITemplateWriter {

    // FIELDS
    /** master hash -- fileName -> key -> string */
    private Map<String, Map<String, List<String>>> masterHash;

    /**
     * Construct a template hash writer.
     */
    public TemplateHashWriter() {
        // We expect few file names, so we use a tree map at the high level.
        this.masterHash = new TreeMap<String, Map<String, List<String>>>();
    }

    @Override
    public void write(String fileName, String key, String outString) throws IOException {
        // Get the sub-hash for this file.
        Map<String, List<String>> subHash = this.masterHash.computeIfAbsent(key, x -> new HashMap<String, List<String>>());
        // Get the string list for this key.
        List<String> valueList = subHash.computeIfAbsent(key, x -> new ArrayList<String>(2));
        // Store the template string, using the provided key.
        valueList.add(outString);
    }

    /**
     * @return the template strings for the specifed file name and key, or an empty list if none exist
     *
     * @param fileName	name of the input file for the desired string
     * @param key		key value of the desired string
     */
    public List<String> getStrings(String fileName, String key) {
        Map<String, List<String>> subHash = this.masterHash.get(fileName);
        List<String> retVal;
        if (subHash == null)
            retVal = Collections.emptyList();
        else
            retVal = subHash.getOrDefault(key, Collections.emptyList());
        return retVal;
    }

    @Override
    public void close() {
        // This is an in-memory structure.  No action is needed.
    }

}
