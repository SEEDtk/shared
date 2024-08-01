/**
 *
 */
package org.theseed.io;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            this.fields = TabbedLineReader.this.splitLine(inLine);
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
         * @return the number in the indexed column, or 0 if the column is empty
         *
         * An invalid numeric value throws a RunTimeException.
         *
         * @param idx	index (0-based) of the column in question, as returned by findField
         */
        public int getInt(int idx) {
            int retVal = 0;
            String colValue = this.fields[idx];
            if (colValue != null && ! colValue.isEmpty()) {
                try {
                    retVal = Integer.parseInt(colValue);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid value \"" + colValue + "\" in numeric input column.");
                }
            }
            return retVal;
        }

        /**
         * @return the floating-point number in the indexed column
         *
         * An invalid numeric value throws a RunTimeException.  An empty value returns NaN.
         *
         * @param idx	index (0-based) of the column in question, as returned by findField
         */
        public double getDouble(int idx) {
            double retVal;
            String colValue = this.fields[idx];
            if (colValue.isEmpty())
                retVal = Double.NaN;
            else try {
                retVal = Double.parseDouble(colValue);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid value \"" + colValue + "\" in numeric input column.");
            }
            return retVal;
        }

        /**
         * @return a (possibly invalid) floating-point number in the indexed column
         *
         * An invalid numeric value or an empty value returns NaN.
         *
         * @param idx	index (0-based) of the column in question, as returned by findField
         */
        public double getDoubleSafe(int idx) {
            double retVal;
            String colValue = this.fields[idx];
            if (colValue.isEmpty())
                retVal = Double.NaN;
            else try {
                retVal = Double.parseDouble(colValue);
            } catch (NumberFormatException e) {
                retVal = Double.NaN;
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
         * @return the boolean value in the indexed column
         *
         * "1", "Y", "Yes", or "true" indicates TRUE.  Everything else is FALSE.
         *
         * @param idx			index (0-based) of the column in question, as returned by findField
         */
        public boolean getFancyFlag(int idx) {
            String colValue = StringUtils.deleteWhitespace(this.fields[idx]).toLowerCase();
            return TabbedLineReader.TRUE_VALUES.contains(colValue);
        }

        /**
         * @return TRUE if the column is empty, else FALSE
         */
        public boolean isEmpty(int idx) {
            return StringUtils.deleteWhitespace(this.fields[idx]).isEmpty();
        }

        /**
         * @return the original input line (without the line-end character)
         */
        public String getAll() {
            return this.lineText;
        }

        /**
         * @return all the fields of the line
         */
        public String[] getFields() {
            return this.fields;
        }

        /**
         * @return the original text of the line
         */
        @Override
        public String toString() {
            return StringUtils.join(this.fields, '\t');
        }

    }

    //FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TabbedLineReader.class);
    /** array of field names from the header */
    private String[] labels;
    /** underlying stream of lines */
    private Iterator<String> reader;
    /** underlying input stream */
    private InputStream stream;
    /** text of next line to return */
    private String nextLine;
    /** text of header line */
    private String headerLine;
    /** number of data lines read */
    private int lineCount;
    /** delimiter to use (usually tab) */
    private char delim;
    /** list of fancy TRUE values */
    private static final Set<String> TRUE_VALUES = Stream.of("1", "true", "yes", "y", "t").collect(Collectors.toSet());

    /**
     * Open a tabbed-line reader for a file.
     *
     * @param inFile	input file to open
     *
     * @throws IOException
     */
    public TabbedLineReader(File inFile) throws IOException {
        this.openFile(inFile, '\t');
        this.readHeader();
    }

    /**
     * Open a tabbed-line reader for a file with a nonstandard delimiter.
     *
     * @param inFile	input file to open
     * @param delimiter	delimiter to use
     *
     * @throws IOException
     */
    public TabbedLineReader(File inFile, char delimiter) throws IOException {
        this.openFile(inFile, delimiter);
        this.readHeader();
    }

    /**
     * Open a file for tabbed line reading.
     *
     * @param inFile	input file to open
     * @param delimiter	delimiter to use
     *
     * @throws IOException
     */
    private void openFile(File inFile, char delimiter) throws IOException {
        this.delim = delimiter;
        this.stream = new FileInputStream(inFile);
        this.reader = new LineReader(this.stream);
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
        this.openFile(inFile, '\t');
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
        this.openFile(inStream, '\t');
        this.clearLabels(fields);
        this.readAhead();
    }

    /**
     * Open a tabbed-line reader for a headerless stream with a non-standard delimiter
     *
     * @param inStream	input stream to open
     * @param fields	number of fields in each record
     * @param delimiter	delimiter to use
     *
     * @throws IOException
     */
    public TabbedLineReader(InputStream inStream, int fields, char delimiter) throws IOException {
        this.openFile(inStream, delimiter);
        this.clearLabels(fields);
        this.readAhead();
    }
    /**
     * Open an input stream for tabbed line reading.
     *
     * @param inStream	input stream to read
     *
     * @throws IOException
     */
    private void openFile(InputStream inStream, char delimiter) throws IOException {
        this.delim = delimiter;
        this.stream = inStream;
        this.reader = new LineReader(inStream);
    }

    /**
     * Open a tabbed-line reader for an input stream with a non-standard delimiter.
     *
     * @param inStream	input stream to open
     * @param delimiter	delimiter to use
     *
     * @throws IOException
     */
    public TabbedLineReader(InputStream inStream, char delimiter) throws IOException {
        this.openFile(inStream, delimiter);
        this.readHeader();
    }

    /**
     * Open a tabbed-line reader for an input stream.
     *
     * @param inStream	input stream to open
     *
     * @throws IOException
     */
    public TabbedLineReader(InputStream inStream) throws IOException {
        this.openFile(inStream, '\t');
        this.readHeader();
    }

    /**
     * Open a tabbed line reader to read a file, or optionally the standard input.
     *
     * @param inFile	file to read, or NULL to read from the standard input
     *
     * @return the open tabbed line reader
     *
     * @throws IOException
     */
    public static TabbedLineReader openInput(File inFile) throws IOException {
        TabbedLineReader retVal;
        if (inFile == null) {
            log.info("Input will be taken from the standard input.");
            retVal = new TabbedLineReader(System.in);
        } else if (! inFile.canRead())
            throw new FileNotFoundException("Input file " + inFile + " is not found or is unreadable.");
        else {
            log.info("Input will be read from {}.", inFile);
            retVal = new TabbedLineReader(inFile);
        }
        return retVal;
    }


    /**
     * Open a tabbed line reader to read a list of strings.
     *
     * @param strings	list of strings, including the header row
     */
    public TabbedLineReader(List<String> strings) {
        this.stream = null;
        this.reader = strings.iterator();
        this.delim = '\t';
        this.readHeader();
    }

    /**
     * Open a tabbed-line reader for a headerless file with an alternate delimiter
     *
     * @param inFile	input file to open
     * @param fields	number of fields in each record
     * @param delimiter	delimiter to use
     *
     * @throws IOException
     */
    public TabbedLineReader(File inFile, int fields, char delimiter) throws IOException {
        this.openFile(inFile, delimiter);
        this.clearLabels(fields);
        this.readAhead();
    }

    /**
     * Read in the header record and create the array of field labels.
     *
     * @throws IOException
     */
    protected void readHeader() {
        // Denote no lines have been read.
        this.lineCount = 0;
        // Read the header.
        if (! this.reader.hasNext()) {
            // Here the entire file is empty.  Insure we get EOF on the first read.
            this.nextLine = null;
            this.labels = new String[0];
        } else {
            this.headerLine = this.reader.next();
            // Parse the header line into labels and normalize them to lower case.
            this.labels = this.splitLine(headerLine);
            // Set up to return the first data line.
            this.readAhead();
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
     * @return the label array
     */
    public String[] getLabels() {
        return this.labels;
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
                retVal = findColumn(fieldName);
            }
        }
        // Validate the result.
        if (retVal < 0 || retVal >= this.labels.length) {
            throw new IOException("Invalid field specifier \"" + fieldName + "\" for tabbed file.");
        }
        return retVal;
    }

    /**
     * @return the index of the field with the specified name
     *
     * @param fieldName		name of the field to find (not case-sensitive)
     */
    public int findColumn(String fieldName) {
        // Convert the field to lower case so the search is case-insensitive.
        String normalized = fieldName.toLowerCase();
        int retVal = this.labels.length - 1;
        // Look for the named field.
        boolean found = false;
        while (! found && retVal >= 0) {
            String label = this.labels[retVal].toLowerCase();
            if (normalized.contentEquals(label) ||
                    normalized.contentEquals(StringUtils.substringAfterLast(label, "."))) {
                found = true;
            } else {
                retVal--;
            }
        }
        if (! found) {
            retVal = -1;
        }
        return retVal;
    }

    @Override
    public void close() {
        try {
            if (this.stream != null)
                this.stream.close();
        } catch (Exception e) {
            // Just ignore an error in close.
        }
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
     * Prepare the line buffer with the next line of data.  Note we skip blank lines.
     */
    protected void readAhead() {
        if (! reader.hasNext()) {
            this.nextLine = null;
        } else {
            this.nextLine = reader.next();
            while (this.nextLine != null && this.nextLine.isEmpty() && reader.hasNext())
                this.nextLine = reader.next();
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

    /**
     * Read a map from a tab-delimited file with headers.  The first column specified is the key, the
     * second is the data.
     *
     * @param inFile	input file to read
     * @param kColumn	index (1-based) or name of the key column to read
     * @param vCcolumn	index (1-based) or name of the value column to read
     *
     * @return a map of the values in the specified columns of the file
     *
     * @throws IOException
     */
    public static Map<String, String> readMap(File inFile, String kColumn, String vColumn) throws IOException {
        Map<String, String> retVal = new HashMap<String, String>();
        try (TabbedLineReader inStream = new TabbedLineReader(inFile)) {
            int kCol = inStream.findField(kColumn);
            int vCol = inStream.findField(vColumn);
            for (Line line : inStream)
                retVal.put(line.get(kCol), line.get(vCol));
        }
        return retVal;
     }

    /**
     * Read a string set from a tab-delimited file with headers.  The strings to be put in the set are
     * taken from the specified column.
     *
     * @param inFile	input file to read
     * @param column	index (1-based) or name of the column to read
     *
     * @return a set of the values in the specified column of the file
     *
     * @throws IOException
     */
    public static Set<String> readSet(File inFile, String column) throws IOException {
        Set<String> retVal = new HashSet<String>();
        try (TabbedLineReader reader = new TabbedLineReader(inFile)) {
            reader.readIntoSet(column, retVal);
        }
        return retVal;
    }

    /**
     * Read a string set from a tab-delimited stream with headers.  The strings to be put in the set are
     * taken from the specified column.
     *
     * @param inStream	input stream to read
     * @param column	index (1-based) or name of the column to read
     *
     * @return a set of the values in the specified column of the file
     *
     * @throws IOException
     */
    public static Set<String> readSet(InputStream inStream, String column) throws IOException {
        Set<String> retVal = new HashSet<String>();
        try (TabbedLineReader reader = new TabbedLineReader(inStream)) {
            reader.readIntoSet(column, retVal);
        }
        return retVal;
    }

    /**
     * Read a string set from this file.  The strings to be put in the set are
     * taken from the specified column.  This process exhausts the file.
     *
     * @param inStream	input stream to read
     * @param column	index (1-based) or name of the column to read
     *
     * @return a set of the values in the specified column of the file
     *
     * @throws IOException
     */
    private void readIntoSet(String column, Set<String> retVal) throws IOException {
        int idx = this.findField(column);
        for (TabbedLineReader.Line line : this) {
            String value = line.get(idx);
            retVal.add(value);
        }
    }

    /**
     * Read a single column into a list.
     *
     * @param inFile	file to read
     * @param string	index (1-based) or name of the column to read
     *
     * @return a list of the strings found in the specified column
     *
     * @throws IOException
     */
    public static List<String> readColumn(File inFile, String string) throws IOException {
        List<String> retVal = new ArrayList<String>(100);
        try (TabbedLineReader inStream = new TabbedLineReader(inFile)) {
            int inCol = inStream.findField(string);
            for (TabbedLineReader.Line line : inStream)
                retVal.add(line.get(inCol));
        }
        return retVal;
    }

    /**
     * @return a stream through the lines of this file
     */
    public Stream<Line> stream() {
        Stream<Line> retVal = StreamSupport.stream(this.spliterator(), false);
        return retVal;
    }

    /**
     * Split a line into fields.
     *
     * @param line	line to split
     *
     * @return an array of the field strings
     */
    protected String[] splitLine(String line) {
        return StringUtils.splitPreserveAllTokens(line, TabbedLineReader.this.delim);
    }

}
