/**
 *
 */
package org.theseed.sequence;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * This is an input stream for FASTA files.  It uses an underlying scanner to
 * read the file.
 */
public class FastaInputStream implements Iterable<Sequence>, AutoCloseable,
        Iterator<Sequence> {

    private static final Pattern DELIMITER = Pattern.compile("\\r?\\n>");
    private static final Pattern END_LINE = Pattern.compile("\\s*\\r?\\n");

    // FIELDS
    /** underlying scanner for reading input */
    Scanner inputSource = null;

    /**
     * This is a general file filter that only accepts FASTA files.
     */
    public static class Fasta implements FileFilter {

        /**
         * This is the set of permissible filename extensions.
         */
        private static final Set<String> FASTA_ENDINGS = Set.of("fasta", "fa", "fna", "faa");

        @Override
        public boolean accept(File pathname) {
            String ending = StringUtils.substringAfterLast(pathname.getName(), ".");
            return FASTA_ENDINGS.contains(ending);
        }

    }

    /**
     * Prepare to read FASTA from an input stream.
     */
    public FastaInputStream(InputStream inStream) {
        this.inputSource = new Scanner(inStream);
        setup();
    }

    /**
     * Setup the scanner to start reading.
     */
    private void setup() {
        this.inputSource.useDelimiter(DELIMITER);
        // Because the delimiter includes the ">" symbol, when we read in the middle of the
        // file, we will be positioned past the symbol at the start of each record. We have
        // to skip the initial symbol as a result.  We only do this if the file is nonempty
        // to start.
        if (this.inputSource.hasNext())
            this.inputSource.skip(">");
    }

    /**
     * Prepare to read FASTA from a file.
     *
     * @throws FileNotFoundException
     */
    public FastaInputStream(File inFile) throws FileNotFoundException {
        this.inputSource = new Scanner(inFile);
        setup();
    }

    @Override
    public Iterator<Sequence> iterator() {
        return this;
    }

    @Override
    public void close() {
        if (this.inputSource != null) {
            this.inputSource.close();
            this.inputSource = null;
        }

    }

    /**
     * @return TRUE iff there are more sequences in this stream.
     */
    @Override
    public boolean hasNext() {
        return (this.inputSource == null ? false : this.inputSource.hasNext());
    }

    /**
     * @return the next sequence in this stream.
     */
    @Override
    public Sequence next() {
        Sequence retVal = null;
        try {
            // Get the FASTA record.
            String sequenceData = this.inputSource.next();
            // Split it into lines.
            String[] lines = END_LINE.split(sequenceData);
            // Parse the header to get the label and comment.
            String[] header = StringUtils.split(lines[0], " ", 2);
            String label = header[0];
            String comment = (header.length < 2 ? "" : header[1]);
            // Estimate the size of the sequence.
            int sizeEstimate = (lines.length == 2 ? lines[1].length() : lines.length * 64);
            StringBuilder sequence = new StringBuilder(sizeEstimate);
            for (int i = 1; i < lines.length; i++) sequence.append(lines[i]);
            // Build the sequence object.
            retVal = new Sequence(label, comment, sequence.toString());
        } catch (NullPointerException e) {
            // The file is already closed.  We will return NULL.
        }
        return retVal;
    }

    /**
     * @return a list of all the sequences in a FASTA file
     *
     * @param fastaFile FASTA file to read
     *
     * @throws IOException
     */
    public static List<Sequence> readAll(File fastaFile) throws IOException {
        List<Sequence> retVal = new ArrayList<Sequence>(100);
        try (FastaInputStream fastaStream = new FastaInputStream(fastaFile)) {
            for (Sequence fastaRecord : fastaStream) {
                retVal.add(fastaRecord);
            }
        }
        return retVal;
    }

}
