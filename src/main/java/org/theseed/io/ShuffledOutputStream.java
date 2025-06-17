/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Iterator;

import org.theseed.stats.Shuffler;

/**
 * This is a variation of BalancedOutputStream for the case where there are two classes.  In this case,
 * the fuzz factor can be any number greater than or equal to 1.0.  The label is placed at the start
 * of the line just as in BalancedOutputStream.
 *
 * @author Bruce Parrello
 *
 */
public class ShuffledOutputStream implements AutoCloseable, ILabeledOutputStream {

    // FIELDS
    /** label for the small class */
    private String smallLabel;
    /** label for the large class */
    private String largeLabel;
    /** buffer for small-class lines */
    private Shuffler<String> smallBuffer;
    /** buffer for large-class lines */
    private Shuffler<String> largeBuffer;
    /** fuzz factor for ratio of small-class lines to large-class lines */
    private double fuzz;
    /** output writer */
    private PrintStream outStream;
    /** TRUE if we opened the output stream internally */
    private boolean openFlag;
    /** maximum number of lines to buffer */
    private static int BUFFER_MAX = 100000;
    /** number of lines in the buffers */
    private int lineCount;

    /**
     * Construct a shuffled output stream on top of an existing output stream.
     *
     * @param fuzzFactor	ratio of large-class lines to small-class lines
     * @param smallLbl		label for the small class
     * @param largeLbl		label for the large class
     * @param stream		stream to receive the output
     */
    public ShuffledOutputStream(double fuzzFactor, String smallLbl, String largeLbl, PrintStream stream) {
        this.outStream = stream;
        this.openFlag = false;
        this.setup(fuzzFactor, smallLbl, largeLbl);
    }

    /**
     * Construct a shuffled output stream for a file.
     *
     * @param fuzzFactor	ratio of large-class lines to small-class lines
     * @param smallLbl		label for the small class
     * @param largeLbl		label for the large class
     * @param outFile		file to receive the output
     */
    public ShuffledOutputStream(double fuzzFactor, String smallLbl, String largeLbl, File outFile) {
        try {
            this.outStream = new PrintStream(outFile);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
        this.openFlag = true;
        this.setup(fuzzFactor, smallLbl, largeLbl);
    }

    /**
     * Initialize this object for output.
     *
     * @param fuzzFactor	ratio of large-class lines to small-class lines
     * @param smallLbl		label for the small class
     * @param largeLbl		label for the large class
     */
    private void setup(double fuzzFactor, String smallLbl, String largeLbl) {
        this.fuzz = fuzzFactor;
        this.smallLabel = smallLbl;
        this.largeLabel = largeLbl;
        // Compute the expected number of small-class and large-class lines, then create the line buffers.
        int smallLines = BUFFER_MAX / (int) (fuzzFactor + 1.0);
        int largeLines = BUFFER_MAX - smallLines;
        this.smallBuffer = new Shuffler<String>(smallLines);
        this.largeBuffer = new Shuffler<String>(largeLines);
        // Denote the buffers are empty.
        this.lineCount = 0;
    }

    @Override
    public void close() {
        // Write all the buffered lines.
        this.writeAll();
        // Flush the output and close the file if we opened it.
        this.outStream.flush();
        if (this.openFlag) this.outStream.close();
    }

    /**
     * Write a line immediately.  This is useful for headers.
     *
     * @param label		label to put at the front of the line
     * @param text		body text of the line
     */
    public void writeImmediate(String label, String text) {
        this.outStream.format("%s\t%s%n", label, text);
    }

    /**
     * Queue a line for writing.
     *
     * @param label		label to put at the front of the line
     * @param text		body text of the line
     */
    public void write(String label, String text) {
        // Insure there is room in the buffer.
        if (this.lineCount >= BUFFER_MAX) {
            this.writeAll();
        }
        // Store the line.
        if (label.contentEquals(this.smallLabel))
            this.smallBuffer.add(text);
        else if (label.contentEquals(this.largeLabel))
            this.largeBuffer.add(text);
        else
            throw new IllegalArgumentException("Invalid label \"" + label + "\" in output stream.");
        this.lineCount++;
    }

    /**
     * Write all the lines accumulated to this point.
     */
    private void writeAll() {
        // These counters track how many lines of each type we've written.
        double smallCount = 0.0;
        double largeCount = 0.0;
        // Scramble both buffers.
        this.smallBuffer.shuffle(this.smallBuffer.size());
        this.largeBuffer.shuffle(this.largeBuffer.size());
        // Get an iterator through the large buffer.
        Iterator<String> largeIter = this.largeBuffer.iterator();
        // Loop through the small-class lines.
        for (String smallLine : this.smallBuffer) {
            this.writeImmediate(this.smallLabel, smallLine);
            smallCount++;
            while ((largeCount + 1) / smallCount <= this.fuzz && largeIter.hasNext()) {
                this.writeImmediate(this.largeLabel, largeIter.next());
                largeCount++;
            }
        }
        // Finish off the large-class lines.
        while (largeIter.hasNext())
            this.writeImmediate(this.largeLabel, largeIter.next());
        // Clear the buffers and counters.
        this.smallBuffer.clear();
        this.largeBuffer.clear();
        this.lineCount = 0;
    }

    /**
     * Update the maximum number of lines to buffer.
     *
     * @param newValue		proposed new buffering limit
     */
    public static void setBufferMax(int newValue) {
        ShuffledOutputStream.BUFFER_MAX = newValue;
    }

}
