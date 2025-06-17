package org.theseed.reports;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

/**
 * This is a simple base class for report writers.  If a call is made to getWriter() or one of its service methods, a
 * PrintWriter will be created and closed automatically.
 *
 * @author Bruce Parrello
 *
 */
public class BaseReporter implements AutoCloseable {

    /** saved output stream */
    private OutputStream outStream;
    /** output stream for the report */
    private PrintWriter writer;

    /**
     * Construct a reporter.
     *
     * @param output	output stream to contain the report
     */
    public BaseReporter(OutputStream output) {
        this.outStream = output;
        this.writer = null;
    }

    /**
     * Write a formatted output line.
     */
    protected void print(String format, Object... args) {
        this.getWriter().format(format, args);
        this.getWriter().println();
    }

    /**
     * Write an unformatted output line.
     */
    protected void println(String line) {
        this.getWriter().println(line);
    }

    /**
     * Write a blank output line.
     */
    protected void println() {
        this.getWriter().println();
    }

    /**
     * @return the writer object
     */
    protected PrintWriter getWriter() {
        if (this.writer == null)
            this.writer = new PrintWriter(this.outStream);
        return writer;
    }

    @Override
    public void close() {
        // Close according to the type of output file.
        if (this.writer != null)
            this.writer.close();
        else {
            try {
                this.outStream.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
