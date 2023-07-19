/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a variation of the tabbed line reader for CSV files.  In addition to the normal
 * splitting, it has to parse out quoted strings.
 *
 * @author Bruce Parrello
 *
 */
public class CsvLineReader extends TabbedLineReader {

    /**
     * Open a CSV line reader for a file.
     *
     * @param inFile	name of the file
     *
     * @throws IOException
     */
    public CsvLineReader(File inFile) throws IOException {
        super(inFile, ',');
    }

    /**
     * Open a CSV reader for a headerless file.
     *
     * @param inFile	name of the file
     * @param fields	number of columns to expect
     *
     * @throws IOException
     */
    public CsvLineReader(File inFile, int fields) throws IOException {
        super(inFile, fields, ',');
    }

    /**
     * Open a CSV reader for a headerless input stream.
     *
     * @param inStream	input stream to open
     * @param fields	number of columns to expect
     *
     * @throws IOException
     */
    public CsvLineReader(InputStream inStream, int fields) throws IOException {
        super(inStream, fields, ',');
    }

    /**
     * Open a CSV reader for an input stream.
     *
     * @param inStream	input stream to open
     *
     * @throws IOException
     */
    public CsvLineReader(InputStream inStream) throws IOException {
        super(inStream, ',');
    }

    /**
     *
     * This is an override for the line splitter that handles quotes properly.
     *
     * @param line	line to split
     *
     * @return an array of the fields in the line
     */
    @Override
    protected String[] splitLine(String line) {
        // We will build the fields in a list.  First we need to estimate the size.
        String[] labels = this.getLabels();
        int width = (labels != null ? labels.length : line.length() / 5);
        List<String> retVal = new ArrayList<String>(width);
        final int n = line.length();
        StringBuffer buffer = new StringBuffer(n);
        // Position at the first character of the string.
        int i = 0;
        // This will be TRUE if the current field is quoted.
        boolean quoted;
        if (n > 0 && line.charAt(0) == '"') {
            quoted = true;
            i++;
        } else
            quoted = false;
        // Loop through the line's characters.
        while (i < n) {
            final char c = line.charAt(i);
            switch (c) {
            case ',' :
                if (quoted) {
                    buffer.append(c);
                    i++;
                } else {
                    // Here we have a new field.
                    this.endField(retVal, buffer);
                    buffer.setLength(0);
                    i++;
                    if (i < n && line.charAt(i) == '"') {
                        quoted = true;
                        i++;
                    } else
                        quoted = false;
                }
                break;
            case '"' :
                if (quoted) {
                    // Here we may need to end the quotes.
                    i++;
                    if (i < n && quoted && line.charAt(i) == '"') {
                        // Doubled quotes are a single, internal quote.
                        buffer.append('"');
                        i++;
                    } else
                        quoted = false;
                } else {
                    // Internal quote in an unquoted string.
                    buffer.append('"');
                    i++;
                }
                break;
             default :
                buffer.append(c);
                i++;
             }
        }
        // Finish the residual and output the fields.
        this.endField(retVal, buffer);
        return retVal.stream().toArray(String[]::new);
    }

        /**
         * Terminate the current buffer and add it to the field list.
         *
         * @param fields	list of field strings
         * @param buffer	buffer containing current field
         */
        private void endField(List<String> fields, StringBuffer buffer) {
            fields.add(buffer.toString());
        }

}

