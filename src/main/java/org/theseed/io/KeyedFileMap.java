/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

/**
 * This object is used to build a keyed, tab-delimited file.  Records are added in order, and
 * then they may be modified at will.  We track both the headers and the file records themselves.
 * Each record is represented in a map with the key field first, followed by one or more
 * data fields.  We support operations to add columns to individual records and to the header
 * list, but if this is done carelessly it can lead to chaos.
 *
 * In the case of duplicates, the last value is kept.
 *
 * @author Bruce Parrello
 *
 */
public class KeyedFileMap {

    // FIELDS
    /** linked hash map containing the records in order, organized by key */
    private LinkedHashMap<String, List<String>> records;
    /** list of header column labels */
    private List<String> headers;
    /** duplicate-key count */
    private int dupCount;
    /** double data type pattern */
    public static final Pattern DOUBLE_PATTERN = Pattern.compile("\\s*[\\-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][\\-+]?\\d+)?\\s*");

    /**
     * Create a new, blank keyed file map.
     *
     * @param keyName	key column name
     */
    public KeyedFileMap(String keyName) {
        this.setup(keyName);
    }

    /**
     * Create a keyed file map from a tab-delimited file.
     *
     * @param inFile	source input file
     * @param keyCol	name of key column
     *
     * @throws IOException
     */
    public KeyedFileMap(File inFile, String keyCol) throws IOException {
        this.setup(keyCol);
        try (TabbedLineReader inStream = new TabbedLineReader(inFile)) {
            // Locate the key column.
            int keyIdx = inStream.findField(keyCol);
            // Build the headers list.
            String[] labels = inStream.getLabels();
            IntStream.range(0, labels.length).filter(i -> (i != keyIdx))
                .forEach(i -> this.headers.add(labels[i]));
            // Loop through the data records, adding them.
            for (TabbedLineReader.Line line : inStream) {
                // Create the record.  We initialize it to empty strings.
                List<String> record = new ArrayList<String>(labels.length);
                // Get the input fields.
                String[] fields = line.getFields();
                // Compute the key.
                String key = "";
                if (fields.length > keyIdx)
                    key = fields[keyIdx];
                // Store the data fields.
                for (int i = 0; i < fields.length; i++) {
                    if (i != keyIdx)
                        record.add(fields[i]);
                }
                for (int i = fields.length; i < labels.length; i++)
                    record.add("");
                this.addRecord(key, record);
            }
        }
    }

    /**
     * Initialize this object's fields.
     *
     * @param keyName	name for the key field
     */
    private void setup(String keyName) {
        this.records = new LinkedHashMap<String, List<String>>();
        this.headers = new ArrayList<String>();
        this.headers.add(keyName);
        this.dupCount = 0;
    }

    /**
     * Add a list of column names for the headers.
     *
     * @param headers	list of header column names to append
     */
    public void addHeaders(List<String> headers) {
        this.headers.addAll(headers);
    }

    /**
     * @return an iterator through the records currently in the file
     */
    public Iterator<Map.Entry<String, List<String>>> iterator() {
        return this.records.entrySet().iterator();
    }

    /**
     * Add a new record to the file.
     *
     * @param key	key value for the record
     * @param data	list of data columns, not including the key
     */
    public void addRecord(String key, List<String> data) {
        List<String> record = new ArrayList<String>(data.size() + 1);
        record.add(key);
        record.addAll(data);
        if (this.records.containsKey(key))
            dupCount++;
        this.records.put(key, record);
    }

    /**
     * Output the file.
     *
     * @param outFile	output file to receive the records
     *
     * @throws IOException
     */
    public void write(File outFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(outFile)) {
            // Write the headers.
            writer.println(StringUtils.join(this.headers, '\t'));
            // Write the data records.
            for (List<String> record : this.records.values())
                writer.println(StringUtils.join(record, '\t'));
        }
    }

    /**
     * @return the list of headers for this keyed file
     */
    public List<String> getHeaders() {
        return this.headers;
    }

    /**
     * @return the record with the specified key, or NULL if it does not exist
     *
     * @param key	key of the desired record
     */
    public List<String> getRecord(String key) {
        return this.records.get(key);
    }

    /**
     * @return the number of records
     */
    public int size() {
        return this.records.size();
    }

    /**
     * @return the number of columns (including the key)
     */
    public int width() {
        return this.headers.size();
    }

    /**
     * @return the column index of the column with the specified name, or -1 if none
     *
     * @param name	desired column name
     */
    public int findColumn(String name) {
        String normalized = name.toLowerCase();
        int retVal = -1;
        int nCols = this.headers.size();
        for (int i = 0; i < nCols && retVal == -1; i++) {
            String colName = this.headers.get(i).toLowerCase();
            if (normalized.contentEquals(colName) ||
                    normalized.contentEquals(StringUtils.substringAfterLast(colName, ".")))
                retVal = i;
        }
        return retVal;
    }

    /**
     * @return the list of data records
     */
    public Collection<List<String>> getRecords() {
        return this.records.values();
    }

    /**
     * This converts the records to floating-point values.  Any value that does not look like
     * a number is converted to NaN.  The key column is converted to all NaN.
     *
     * @return a list of floating-point arrays from the records
     */
    public List<double[]> getRecordNumbers() {
        int width = this.headers.size();
        // Create the output list.
        List<double[]> retVal = new ArrayList<double[]>(this.records.size());
        // Now loop through the records, converting.
        for (List<String> record : this.records.values()) {
            double[] numRecord = new double[width];
            numRecord[0] = Double.NaN;
            for (int i = 1; i < width; i++) {
                String value = record.get(i);
                if (DOUBLE_PATTERN.matcher(value).matches())
                    numRecord[i] = Double.parseDouble(value);
                else
                    numRecord[i] = Double.NaN;
            }
            retVal.add(numRecord);
        }
        return retVal;
    }

    /**
     * Copy the specified map into this one, replacing the current contents.
     * This is a shallow copy, not a full-depth cloning.  The intent is to
     * allow us to build a second file and replace this one with it.
     *
     * @param newMap	map to copy from
     */
    public void shallowCopyFrom(KeyedFileMap newMap) {
        this.headers = newMap.headers;
        this.records = newMap.records;
    }

    /**
     * @return the duplicate-key count
     */
    public int getDupCount() {
        return this.dupCount;
    }

    /**
     * Remove columns to match the indicated headers.  Essentially, only columns that
     * are in the specified set will remain.  The key column is always kept.
     *
     * @param newCols	set of headers to keep
     *
     * @return the number of columns deleted
     */
    public int reduceCols(Set<String> newCols) {
        int retVal = 0;
        // Loop through the columns.  To make the deletes more efficient, we
        // move backwards from the end.
        for (int c = this.headers.size() - 1; c > 0; c--) {
            String headerName = this.headers.get(c);
            if (! newCols.contains(headerName)) {
                // Here we must delete the column.  First, delete the header.
                this.headers.remove(c);
                // Now, delete the data column.
                for (List<String> record : this.records.values())
                    record.remove(c);
                // Count the deleted column.
                retVal++;
            }
        }
        return retVal;
    }

}
