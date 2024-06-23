/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a simple static class for reading and writing string sets to files.
 *
 * @author Bruce Parrello
 *
 */
public class SetFile {

    /**
     * Write a string set to an output file.
     *
     * @param outFile		output file in which to put the string set
     * @param stringSet		set of strings to write to the file
     */
    public static void save(File outFile, Set<String> stringSet) {
        try (var writer = new PrintWriter(outFile)) {
            for (String string : stringSet)
                writer.println(string);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Read a string set from an input file.
     *
     * @param inFile		input file from which to read the string set
     *
     * @return a set of the strings in the file
     */
    public static Set<String> load(File inFile) {
        Set<String> retVal = new HashSet<String>();
        try (var reader = new LineReader(inFile)) {
            for (var line : reader)
                retVal.add(line);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return retVal;
    }
}
