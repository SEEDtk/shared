/**
 *
 */
package org.theseed.sequence;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * This class writes a set of sequences to a FASTA file.  It uses an underlying PrintWriter
 * to do the output.
 *
 * @author Bruce Parrello
 */
public class FastaOutputStream implements Closeable, AutoCloseable, Flushable {

    // FIELDS
    PrintWriter writer = null;

    /**
     * Open an output stream for FASTA output.
     */
    public FastaOutputStream(OutputStream outStream) {
        writer = new PrintWriter(outStream);
    }

    /**
     * Open a file for FASTA output.
     *
     * @throws FileNotFoundException
     */
    public FastaOutputStream(File outFile) throws FileNotFoundException {
        writer = new PrintWriter(outFile);
    }

    /**
     * Write a sequence to the output stream.
     *
     * @throws IOException
     */
    public void write(Sequence seq) throws IOException {
        // Write the label and comment.
        String comment = seq.getComment();
        String header;
        if (comment != null && ! comment.isEmpty()) {
            header = String.format(">%s %s", seq.getLabel(), comment);
        } else {
            header = String.format(">%s", seq.getLabel());
        }
        writer.println(header);
        // Write the sequence in chunks.
        String sequence = seq.getSequence();
        if (! sequence.isEmpty()) {
            int begin = 0, end = 60;
            while (end < sequence.length()) {
                writer.println(sequence.substring(begin, end));
                begin = end;
                end += 60;
            }
            writer.println(sequence.substring(begin));
        }
    }

    /**
     * Write a collection of sequences to the output stream.
     *
     * @param sequences	collection of sequences to write
     *
     * @throws IOException
     */
    public void write(Collection<? extends Sequence> sequences) throws IOException {
        for (Sequence seq : sequences) {
            this.write(seq);
        }
    }

    @Override
    public void close() {
        if (writer != null) {
            writer.close();
        }

    }

    @Override
    public void flush() throws IOException {
        if (writer != null) {
            writer.flush();
        }

    }

}
