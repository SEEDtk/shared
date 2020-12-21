/**
 *
 */
package org.theseed.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.theseed.counters.CountMap;

/**
 * This is a wrapper class that produces an output stream balanced by class.  When writing a record,
 * the client specifies a class value and then the remainder of the line.  In unbalanced mode, the
 * record is written with the class in the first column and the rest of the line following, after a
 * tab.  In balanced mode, the records are held in memory, and then a randomly-chosen subset is
 * output, so that no class has more than a certain proportion of records more than the smallest class.
 * The proportion is called the "fuzz factor".  A fuzz factor of 1.5 means no class has more than 50%
 * more records than the smallest class.  A fuzz factor of 0 means no balancing is performed; otherwise
 * the factor must be between 1.0 and 2.0.
 *
 * @author Bruce Parrello
 *
 */
public class BalancedOutputStream implements Closeable, AutoCloseable, ILabeledOutputStream {

    // FIELDS
    /** proportion of the smallest class's size to be allowed for output */
    private double fuzzFactor;
    /** hash mapping each class to a list of data lines */
    private Map<String, Shuffler<String>> classLines;
    /** number of lines in each class */
    private CountMap<String> classCounts;
    /** target output stream */
    private PrintStream outputStream;
    /** random number generator */
    private Random randStream;
    /** TRUE if we opened the stream */
    private boolean openFlag;
    /** number of records buffered */
    private int bufferCount;
    /** number of buffered records written */
    private int outputCount;

    /** maximum number of records to buffer */
    private static int BUFFER_MAX = 100000;

    /**
     * Construct a new balanced output stream.
     *
     * @param fuzz			fuzz factor, or 0 for pass-through mode
     * @param outStream		output print stream
     */
    public BalancedOutputStream(double fuzz, PrintStream outStream) {
        setup(fuzz, outStream);
        this.openFlag = false;
    }


    /**
     * Construct a new balanced output stream on a file.
     *
     * @param fuzz			fuzz factor, or 0 for pass-through mode
     * @param outFile		output file to write
     *
     * @throws IOException
     */
    public BalancedOutputStream(double fuzz, File outFile) throws IOException {
        PrintStream outStream = new PrintStream(outFile);
        this.openFlag = true;
        setup(fuzz, outStream);
    }
    /**
     * Initialize this object.
     *
     * @param fuzz		fuzz factor, or 0 for pass-through mode
     * @param outStream	output print stream
     */
    private void setup(double fuzz, PrintStream outStream) {
        this.outputStream = outStream;
        this.fuzzFactor = fuzz;
        this.classLines = new HashMap<String, Shuffler<String>>();
        this.classCounts = new CountMap<String>();
        this.randStream = new Random();
        this.bufferCount = 0;
        this.outputCount = 0;
    }

    /**
     * Write a line directly to output.  This is useful for headers.
     *
     * @param label		text for the label column
     * @param text		text for the rest of the line
     */
    public void writeImmediate(String label, String text) {
        this.outputStream.format("%s\t%s%n", label, text);
    }

    /**
     * Queue a line for output.
     *
     * @param label		class label for this line
     * @param line		data for the line
     */
    public void write(String label, String line) {
        if (this.fuzzFactor == 0) {
            writeImmediate(label, line);
            this.outputCount++;
        } else {
            // Insure we have a queue for this class.
            Shuffler<String> queue;
            if (! this.classLines.containsKey(label)) {
                queue = new Shuffler<String>(20000);
                this.classLines.put(label, queue);
            } else {
                queue = this.classLines.get(label);
            }
            // Add this line to it and count it.
            queue.add(line);
            this.classCounts.count(label);
            this.bufferCount++;
            // If we've buffered too much, flush the system.
            if (this.bufferCount >= BUFFER_MAX) {
                this.writeAll();
                this.reset();
            }
        }

    }

    /** Clear all the buffered lines from memory. */
    private void reset() {
        this.classCounts.deleteAll();
        this.classLines.clear();
        this.bufferCount = 0;
    }

    /** class to track the status of a label's output */
    private class Status {
        /** interval for emitting extra lines */
        protected int interval;
        /** iterator for getting the next line */
        protected Iterator<String> iter;
    }


    /**
     * Close this stream to flush out all the queued lines.
     */
    @Override
    public void close() {
        writeAll();
        this.outputStream.flush();
        // Close the stream if needed.
        if (this.openFlag) this.outputStream.close();
    }


    /**
     * Write everything currently accumulated in memory.
     */
    private void writeAll() {
        // Get the classes in order of most to least frequent.  This list serves as
        // a key to our various status and progress arrays.  All of those arrays
        // are indexed in parallel to this list.
        List<CountMap<String>.Count> labelCounts = this.classCounts.sortedCounts();
        // Only proceed if there is at least one label.
        int nLabels = labelCounts.size();
        if (nLabels > 0) {
            // Get the size of the smallest class.
            int smallest = labelCounts.get(nLabels - 1).getCount();
            // Compute the maximum allowable class output.
            int max = (int) (smallest * this.fuzzFactor);
            // For each class, determine how far apart to space the extra occurrences.
            Status[] statusArray = new Status[nLabels];
            for (int i = 0; i < nLabels; i++) {
                int classCount = labelCounts.get(i).getCount();
                // Compute how far apart each extra record from this class
                // should be spaced and generate the limited iterator.
                int outCount = Math.min(max, classCount);
                Status myStatus = new Status();
                myStatus.interval = outCount / (outCount - smallest + 1);
                Shuffler<String> classList = this.classLines.get(labelCounts.get(i).getKey());
                myStatus.iter = classList.limitedIter(outCount);
                statusArray[i] = myStatus;
                // Shuffle this class's lines.
                for (int j = outCount - 1; j > 1; j--) {
                    int k = randStream.nextInt(j);
                    String buffer = classList.get(j);
                    classList.set(j, classList.get(k));
                    classList.set(k, buffer);
                }
            }
            // Now all the lines are in the proper order and we know how often to emit them.
            // Get some fresh iterators and loop through the groups to emit.
            for (int grp = 0; grp < smallest; grp++) {
                // Loop through the classes.
                for (int i = 0; i < nLabels; i++) {
                    // Determine the number of lines to output for this group.
                    int outCount = (grp % statusArray[i].interval == 0 ? 2 : 1);
                    // Output the lines.
                    while (outCount > 0 && statusArray[i].iter.hasNext()) {
                        this.writeImmediate(labelCounts.get(i).getKey(), statusArray[i].iter.next());
                        outCount--;
                        this.outputCount++;
                    }
                }
            }
        }
    }

    /**
     * Specify a new maximum number of buffered lines.
     *
     * @param newMax	maximum number of lines to keep in memory
     */
    public static void setBufferMax(int newMax) {
        BalancedOutputStream.BUFFER_MAX = newMax;
    }

    /**
     * @return the output count
     */
    public int getOutputCount() {
        // Insure everything is written.
        this.writeAll();
        this.reset();
        return this.outputCount;
    }

}
