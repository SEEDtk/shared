/**
 *
 */
package org.theseed.subsystems;

import java.io.File;
import java.io.FileFilter;

/**
 * This is a file filter that only accepts directories containing subsystem spreadsheets.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
        boolean retVal = pathname.isDirectory();
        if (retVal && ! pathname.getName().startsWith(".")) {
            File spreadsheetFile = new File(pathname, "spreadsheet");
            retVal = spreadsheetFile.canRead();
        }
        return retVal;
    }

}
