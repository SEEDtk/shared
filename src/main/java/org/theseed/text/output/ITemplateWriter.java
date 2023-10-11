/**
 *
 */
package org.theseed.text.output;

import java.io.IOException;

/**
 * This interface must be supported by any object that handles template output.
 * It insures we can process a template output line given the file name, key,
 * and output string.
 */
public interface ITemplateWriter {

    /**
     * Output an expanded template string.
     *
     * @param fileName		input file base name
     * @param key			input line key value
     * @param outString		output expanded template string
     *
     * @throws IOException
     */
    public void write(String fileName, String key, String outString) throws IOException;

   /**
     * Insure all output is written and all I/O resources are freed.
     */
    public void close();

}
