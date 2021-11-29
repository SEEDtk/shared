/**
 *
 */
package org.theseed.reports;

import java.io.PrintWriter;

/**
 * This is a simple base class for reporters used by the BaseReportProcessor subclasses.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseReporterReporter {

    // FIELDS
    /** output print writer */
    private PrintWriter writer;

    /**
     * Initialize the report.
     *
     * @param writer	output print writer
     */
    public void openReport(PrintWriter writer) {
        this.writer = writer;
        this.writeHeader();
    }

    /**
     * Write the report heading.
     */
    protected abstract void writeHeader();

    /**
     * Format a line for output.
     *
     * @param format	output line format string
     * @param args		parameters for the format string
     */
    protected void formatln(String format, Object... args) {
        this.writer.format(format, args);
        this.writer.println();
    }

    /**
     * Write a line of text for output.
     */
    protected void println(String line) {
        this.writer.println(line);
    }

    /**
     * Write a blank line.
     */
    protected void println() {
        this.writer.println();
    }

}
