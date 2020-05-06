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
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;

/**
 * This is a simple, iterable line reader than can be directly created from a file or a stream.
 *
 * @author Bruce Parrello
 *
 */
public class LineReader implements Iterable<String>, Iterator<String>, Closeable, AutoCloseable {

    /** underlying buffered reader */
    private BufferedReader reader;
    /** TRUE if end-of-file has been read */
    private boolean eof;
    /** next line to produce */
    private String nextLine;

    /**
     * Create a line reader for the specified input file.
     *
     * @param inputFile		input file to read
     *
     * @throws IOException
     */
    public LineReader(File inputFile) throws IOException {
        Reader streamReader = new FileReader(inputFile);
        setup(streamReader);
    }

    /**
     * Create a line reader for the specified input stream.
     *
     * @param inputStream	input stream to read
     *
     * @throws IOException
     */
    public LineReader(InputStream inputStream) throws IOException {
        Reader streamReader = new InputStreamReader(inputStream);
        setup(streamReader);
    }

    /**
     * Initialize this file for reading.
     * @param streamReader
     *
     * @throws IOException
     */
    private void setup(Reader streamReader) throws IOException {
        this.reader = new BufferedReader(streamReader);
        this.eof = false;
        this.nextLine = null;
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }

    /**
     * @return TRUE if another line is available
     */
    @Override
    public boolean hasNext() {
        boolean retVal = false;
        if (this.nextLine != null) {
            // Here we have a next line and it has not been consumed.
            retVal = true;
        } else if (! this.eof) {
            // Here we need to check for a next line.
            this.readAhead();
            if (this.nextLine == null) {
                this.eof = true;
            } else {
                retVal = true;
            }
        }
        return retVal;
    }

    /**
     * Get the next line of input into the next-line buffer.
     */
    private void readAhead() {
        try {
            this.nextLine = this.reader.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    /**
     * @return the next line in the file
     */
    @Override
    public String next() {
        String retVal = this.nextLine;
        if (retVal != null) {
            // Denote the next line has been consumed.
            this.nextLine = null;
        } else if (! this.eof) {
            // Here we do not have an available next line, but there may be another one.  We read it and consume it
            // in one operation.
            this.readAhead();
            retVal = this.nextLine;
            this.nextLine = null;
        }
        return retVal;
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

}
