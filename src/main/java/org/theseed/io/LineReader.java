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
    /** next input line */
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
        this.nextLine = this.reader.readLine();
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
        return (this.nextLine != null);
    }

    /**
     * @return the next line in the file
     */
    @Override
    public String next() {
        String retVal = this.nextLine;
        if (retVal != null)
            try {
                this.nextLine = this.reader.readLine();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        return retVal;
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

}
