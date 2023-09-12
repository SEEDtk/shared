/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;

/**
 * This class provides simple methods for processing a marker file; that is, a file containing
 * a single value.  It can read and write the value without opening or closing the file.
 *
 * @author Bruce Parrello
 *
 */
public class MarkerFile {

    /**
     * Create a marker file with the specified string value.
     *
     * @param name		name of the file
     * @param value		value to store in the file
     */
    public static void write(File name, String value) {
        try (PrintStream outStream = new PrintStream(name)) {
            outStream.println(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a marker file with the specified integer value.
     *
     * @param name		name of the file
     * @param value		value to store in the file
     */
    public static void write(File name, int value) {
        MarkerFile.write(name, Integer.toString(value));
    }

    /**
     * @return the string value from a marker file.
     *
     * @param name		name of the file
     */
    public static String read(File name) {
        String retVal = "";
        try (LineReader reader = new LineReader(name)) {
            if (reader.hasNext())
                retVal = reader.next();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return retVal;
    }

    /**
     * @return the string value from a marker file, or an empty string if the file does not exist
     *
     * @param name		name of the file
     */
    public static String readSafe(File name) {
        String retVal;
        if (name.canRead())
            retVal = read(name);
        else
            retVal = "";
        return retVal;
    }

    /**
     * @return an integer value from a marker file.
     *
     * @param name		name of the file
     */
    public static int readInt(File name) {
        int retVal = Integer.valueOf(MarkerFile.read(name));
        return retVal;
    }

}
