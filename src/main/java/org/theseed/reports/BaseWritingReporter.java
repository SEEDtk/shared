/**
 *
 */
package org.theseed.reports;

import java.io.PrintWriter;

/**
 * This is a base class for reports produced by BaseReportProcessor instances.  It registers the print writer
 * and handles access to it.
 *
 * @author Bruce Parrello
 *
 */
public class BaseWritingReporter {

    // FIELDS
    /** output writer */
    protected PrintWriter writer;

    /**
     * Construct a report writer.
     *
     * @param writer	print writer to receive the report output
     */
    public BaseWritingReporter(PrintWriter writer) {
        this.writer = writer;
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

}
