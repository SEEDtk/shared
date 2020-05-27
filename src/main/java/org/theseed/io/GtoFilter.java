/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.FilenameFilter;

/**
 * This is a simple filtering class for returning all the GTOs in a directory.  Note that we have a similar
 * object for getting all the genomes in a directory (GenomeDirectory), but this is useful in situations
 * where we need the file names instead of the actual loaded files.
 *
 * @author Bruce Parrello
 *
 */
public class GtoFilter implements FilenameFilter {

    public static File[] getAll(File directory) {
        return directory.listFiles(new GtoFilter());
    }

    @Override
    public boolean accept(File dir, String name) {
        return (name.endsWith(".gto"));
    }

}
