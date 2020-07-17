/**
 *
 */
package org.theseed.genome.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class iterates through all the SEED genome directories in an Organism directory.
 * The genome IDs are returned as strings.
 *
 * @author Bruce Parrello
 *
 */
public class OrganismDirectories implements Iterable<String> {

    // FIELDS
    /** list of genome IDs found */
    private List<String> genomes;

    // CONSTANTS
    private static final Pattern GENOME_PATTERN = Pattern.compile("\\d+\\.\\d+");

    /**
     * Create a new organism directory iterator.
     *
     * @param orgDir	root organism directory
     */
    public OrganismDirectories(File orgDir) {
        // Get all the subdirectories and files.
        File[] subFiles = orgDir.listFiles();
        // Initialize the output list.
        this.genomes = new ArrayList<String>(subFiles.length);
        for (File subFile : subFiles) {
            String genomeId = subFile.getName();
            if (GENOME_PATTERN.matcher(genomeId).matches() && subFile.isDirectory()) {
                genomes.add(genomeId);
            }
        }
        // Sort the genomes alphabetically.
        genomes.sort(null);
    }

    @Override
    public Iterator<String> iterator() {
        return this.genomes.iterator();
    }

    /**
     * @return the number of genomes
     */
    public int size() {
        return this.genomes.size();
    }

}
