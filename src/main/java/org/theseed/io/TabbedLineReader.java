/**
 *
 */
package org.theseed.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

/**
 * This class reads from a tab-delimited file with headers.  It reads a line at a time, and in each line,
 * the fields can be accessed by name or column position.  Column positions are 1-based.  In addition, a 0
 * indicates the last column and a negative number indicates a column that many positions before the end.
 * An invalid column index or name will cause an ArrayIndexOutOfBoundsException.
 *
 * @author Bruce Parrello
 *
 */
public class TabbedLineReader implements Closeable, AutoCloseable, Iterable<TabbedLineReader.Line>,
    Iterator<TabbedLineReader.Line> {

    /**
     * This nested class represents a single line of data from the tabbed file.  It supports methods to
     * get the values in the line by field name and index.
     *
     */
    public class Line {

        //** FIELDS
        /** array of field contents */
        String[] fields;
        /** original input line */
        String lineText;

        /**
         * Create this object from an input text line.
         *
         * @param inLine	input line read from the stream, without the line-end character
         */
        private Line(String inLine) {
            // Save the input line.
            this.lineText = inLine;
            // Get the number of fields in the line.
            int nFields = labels.length;
            // Normally, this will work.
            this.fields = StringUtils.split(inLine, '\t');
            // If the number of fields is wrong, we have to adjust.
            if (this.fields.length != nFields) {
                // Copy the old array and create a new one of the proper length.
                String[] buffer = this.fields;
                this.fields = new String[nFields];
                // Transfer the strings one by one, padding if necessary.
                for (int i = 0; i < nFields; i++) {
                    if (i < buffer.length) {
                        this.fields[i] = buffer[i];
                    } else {
                        this.fields[i] = "";
                    }
                }
            }
        }

        /**
         * @return the string in the indexed column
         *
         * @param idx	index (0-based) of the column in question, as returned by findField
         */
        public String get(int idx) {
            return this.fields[idx];
        }

        /**
         * @return the number in the indexed column
         *
         * An invalid numeric value throws a RunTimeException.
         *
         * @param idx	index (0-based) of the column in question, as returned by findField
         */
        public int getInt(int idx) {
            int retVal;
            String colValue = this.fields[idx];
            try {
                retVal = Integer.parseInt(colValue);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid value \"" + colValue + "\" in numeric input column.");
            }
            return retVal;
        }

        /**
         * @return the floating-point number in the indexed column
         *
         * An invalid numeric value throws a RunTimeException.
         *
         * @param idx	index (0-based) of the column in question, as returned by findField
         */
        public double getDouble(int idx) {
            double retVal;
            String colValue = this.fields[idx];
            try {
                retVal = Double.parseDouble(colValue);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid value \"" + colValue + "\" in numeric input column.");
            }
            return retVal;
        }

        /**
         * @return the boolean value in the indexed column
         *
         * An empty column or a value of "0" indicates FALSE.  Everything else is TRUE.
         *
         * @param idx	index (0-based) of the column in question, as returned by findField
         */
        public boolean getFlag(int idx) {
            String colValue = StringUtils.deleteWhitespace(this.fields[idx]);
            return (colValue.length() > 0 && ! colValue.contentEquals("0"));
        }

        /**
         * @return the original input line
         */
        public String getAll() {
            return this.lineText;
        }

    }

    //FIELDS
    /** array of field names from the header */
    String[] labels;
    /** underlying reader object */
    BufferedReader reader;
    /** text of next line to return */
    String nextLine;
    /** text of header line */
    String headerLine;
    /** number of data lines read */
    int lineCount;

    /**
     * Open a tabbed-line reader for a file.
     *
     * @param inFile	input file to open
     *
     * @throws IOException
     */
    public TabbedLineReader(File inFile) throws IOException {
        FileReader fileStream = new FileReader(inFile);
        this.reader = new BufferedReader(fileStream);
        this.readHeader();
    }

    /**
     * Open a tabbed-line reader for a headerless file.
     *
     * @param inFile	input file to open
     * @param fields	number of fields in each record
     *
     * @throws IOException
     */
    public TabbedLineReader(File inFile, int fields) throws IOException {
        FileReader fileStream = new FileReader(inFile);
        this.reader = new BufferedReader(fileStream);
        this.clearLabels(fields);
        this.readAhead();
    }

    /**
     * Open a tabbed-line reader for a headerless stream.
     *
     * @param inStream	input stream to open
     * @param fields	number of fields in each record
     *
     * @throws IOException
     */
    public TabbedLineReader(InputStream inStream, int fields) throws IOException {
        InputStreamReader inStreamer = new InputStreamReader(inStream);
        this.reader = new BufferedReader(inStreamer);
        this.clearLabels(fields);
        this.readAhead();
    }
    /**
     * Open a tabbed-line reader for an input stream.
     *
     * @param inStream	input stream to open
     *
     * @throws IOException
     */
    public TabbedLineReader(InputStream inStream) throws IOException {
        InputStreamReader inStreamer = new InputStreamReader(inStream);
        this.reader = new BufferedReader(inStreamer);
        this.readHeader();
    }

    /**
     * Construct a blank, empty tabbed-file reader.
     */
    protected TabbedLineReader() {
    }

    /**
     * Read in the header record and create the array of field labels.
     *
     * @throws IOException
     */
    protected void readHeader() throws IOException {
        try {
            // Denote no lines have been read.
            this.lineCount = 0;
            // Read the header.
            this.headerLine = this.reader.readLine();
            if (headerLine == null) {
                // Here the entire file is empty.  Insure we get EOF on the first read.
                this.nextLine = null;
            } else {
                // Parse the header line into labels.
                this.labels = StringUtils.split(headerLine, '\t');
                // Set up to return the first data line.
                this.readAhead();
            }
        } catch (IOException e) {
            throw new IOException("Error in header of tabbed input file.", e);
        }
    }

    /**
     * Clear the label array.  This is used to create a tabbed input file without headers.
     *
     * @param fields	number of fields to store
     */
    protected void clearLabels(int fields) {
        this.labels = new String[fields];
        Arrays.fill(this.labels, "");
    }

    /**
     * @return the index of a field given a field name.  The name can be the label of the field,
     * or it can be a 1-based numeric index.  An index of 0 indicates the last field.  A negative
     * index indicates a position before the last field.  An invalid name will cause an
     * IOException.
     *
     * @param fieldName		field name/index specifier
     *
     * @throws IOException
     */
    public int findField(String fieldName) throws IOException {
        int retVal;
        // Insure null doesn't screw us up.
        if (fieldName == null) {
            fieldName = "";
            retVal = -1;
        } else {
            try {
                retVal = Integer.parseInt(fieldName);
                // Convert from 1-based to 0-based.
                retVal--;
                // Convert end-of-line values.
                if (retVal < 0) {
                    retVal = this.labels.length + retVal;
                }
            } catch (NumberFormatException e) {
                // If it's not a number, that means we have to search for a name.
                retVal = this.labels.length - 1;
                // First look for a named field.
                boolean found = false;
                while (! found && retVal >= 0) {
                    if (fieldName.contentEquals(this.labels[retVal]) ||
                            fieldName.contentEquals(StringUtils.substringAfterLast(this.labels[retVal], "."))) {
                        found = true;
                    } else {
                        retVal--;
                    }
                }
                if (! found) {
                    retVal = -1;
                }
            }
        }
        // Validate the result.
        if (retVal < 0 || retVal >= this.labels.length) {
            throw new IOException("Invalid field specifier \"" + fieldName + "\" for tabbed file.");
        }
        return retVal;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public Iterator<Line> iterator() {
        return this;
    }

    /**
     * @return TRUE if there is more data in the file, else FALSE
     */
    @Override
    public boolean hasNext() {
        return this.nextLine != null;
    }

    /**
     * @return the next line in the file, or NULL if we are at the end
     */
    @Override
    public Line next() {
        Line retVal;
        if (this.nextLine == null) {
            retVal = null;
        } else {
            retVal = new Line(this.nextLine);
            this.lineCount++;
            this.readAhead();
        }
        return retVal;
    }

    /**
     * Prepare the line buffer with the next line of data.
     */
    protected void readAhead() {
        try {
            this.nextLine = reader.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Error in tabbed input file", e);
        }
    }

    /**
     * @return the number of lines read
     */
    public int linesRead() {
        return this.lineCount;
    }

    /**
     * @return a copy of the header line
     */
    public String header() {
        return this.headerLine;
    }

    /**
     * @return the number of fields in each record
     */
    public int size() {
        return this.labels.length;
    }

}
