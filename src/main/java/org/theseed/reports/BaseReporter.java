package org.theseed.reports;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * This is a simple base class for report writers.
 *
 * @author Bruce Parrello
 *
 */
public class BaseReporter implements Closeable, AutoCloseable {

    /** output stream for the report */
    private PrintWriter writer;

    /**
     * Construct a reporter.
     *
     * @param output	output stream to contain the report
     */
    public BaseReporter(OutputStream output) {
        this.writer = new PrintWriter(output);
    }

    /**
     * Write a formatted output line.
     */
    protected void print(String format, Object... args) {
        this.writer.format(format, args);
        this.writer.println();
    }

    /**
     * Write an unformatted output line.
     */
    protected void println(String line) {
        this.writer.println(line);
    }

    /**
     * Write a blank output line.
     */
    protected void println() {
        this.writer.println();
    }

    /**
     * @return the writer object
     */
    protected PrintWriter getWriter() {
        return writer;
    }

    @Override
    public void close() {
        this.writer.close();
    }

}
