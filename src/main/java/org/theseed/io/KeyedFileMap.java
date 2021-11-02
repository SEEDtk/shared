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

import org.apache.commons.lang3.StringUtils;

/**
 * This object is used to build a keyed, tab-delimited file.  Records are added in order, and
 * then they may be modified at will.  We track both the headers and the file records themselves.
 * Each record is represented in a map with the key field first, followed by one or more
 * data fields.  We support operations to add columns to individual records and to the header
 * list, but if this is done carelessly it can lead to chaos.
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

    /**
     * Create a new, blank keyed file map.
     *
     * @param keyName	key column name
     */
    public KeyedFileMap(String keyName) {
        this.records = new LinkedHashMap<String, List<String>>();
        this.headers = new ArrayList<String>();
        this.headers.add(keyName);
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
     * @return the number of records
     */
    public int size() {
        return this.records.size();
    }

    /**
     * @return the list of data records
     */
    public Collection<List<String>> getRecords() {
        return this.records.values();
    }

}
