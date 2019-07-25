/**
 *
 */
package org.theseed.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

/**
 * This class reads a parameter file and converts it into a String array.  Each line
 * of the file should consist of a command-line option (with the preceding hyphen or
 * hyphens), one or more spaces, and then the parameter value (if any).
 *
 * @author Bruce Parrello
 *
 */
public class Parms {

    /**
     * @return 	a list of strings that can be fed to a command parser to interpret the
     * 			command-line options found in the specified file
     *
     * @param inFile	input file containing the parameters
     *
     * @throws IOException
     */
    public static List<String> fromFile(File inFile) throws IOException {
        // Get a scanner for the file.
        Scanner inStream = new Scanner(inFile);
        // Build the string array in here.
        ArrayList<String> retVal = new ArrayList<String>(20);
        while (inStream.hasNext()) {
            // Get the option name.
            String option = inStream.next();
            // Get the option value.  This may be an empty string.  Nextline gives us the
            // separating whitespace as well as the rest of the line, so we strip it.
            String value = StringUtils.strip(inStream.nextLine());
            // Now add the option.  If the value is nonempty, add that, too.
            retVal.add(option);
            if (! value.isEmpty()) retVal.add(value);
        }
        inStream.close();
        // Convert the buffer to an array and pass it back.
        return retVal;
    }

}
