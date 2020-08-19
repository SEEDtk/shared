/**
 *
 */
package org.theseed.genome.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.theseed.io.TabbedLineReader;

/**
 * This class manages a coreSEED organism directory and provides useful utilities for accessing genome data.
 *
 * @author Bruce Parrello
 *
 */
public class CoreUtilities {

    // FIELDS
    /** if TRUE, status messages will be written to STDERR */
    private boolean debug;
    /** base organism directory */
    private File orgDir;
    /** cache of genome sequences */
    private Map<String, PegList> genomeMap;

    // CONSTANTS

    /** path-and-name suffix to convert a genome ID to the complete path to the assigned-functions file */
    private static final String FUNCTION_FILE_SUFFIX = File.separator + "assigned_functions";

    /** path-and-name suffix to convert a genome ID to the complete path to the deleted-pegs file */
    private static final String DELETED_PEGS_FILE_SUFFIX = File.separator + "Features" + File.separator + "peg" + File.separator + "deleted.features";

    /** path-and-name suffix to convert a genome ID to the complete path to the peg FASTA file */
    private static final String PEG_FILE_SUFFIX = File.separator + "Features" + File.separator + "peg" + File.separator + "fasta";

    /** genome ID extraction pattern */
    private static final Pattern GENOME_ID_PATTERN = Pattern.compile("fig\\|(\\d+\\.\\d+)\\.\\w+\\.\\d+");

    /**
     * Connect this object to an organism directory and initialize the genome cache.
     *
     * @param debugFlag		TRUE if status messages should be written to STDERR, else FALSE
     * @param organismDir	target organism directory
     */
    public CoreUtilities(boolean debugFlag, File organismDir) {
        this.debug = debugFlag;
        this.orgDir = organismDir;
        this.genomeMap = new HashMap<String, PegList>(1500);
    }

    /**
     * Return the list of pegs in a genome.  A cache is maintained of genomes already found.
     *
     * @param orgDir	CoreSEED organism directory
     * @param genomeId	ID of genomes whose pegs are desired
     * @param gMap		hash of genome IDs to cached peg lists
     *
     * @return a PegList object for the identified genome, or NULL if the genome does not exist
     *
     * @throws IOException
     */
    public PegList getGenomePegs(String genomeId) throws IOException {
        PegList retVal = this.genomeMap.get(genomeId);
        if (retVal == null) {
            // Here we have to read the genome in.
            retVal = readGenomeSequences(genomeId);
            // Cache the genome in case it comes up again.
            this.genomeMap.put(genomeId, retVal);
        }
        return retVal;
    }

    /**
     * Get a table of all the protein sequences for the specified genome.
     *
     * @param genomeId	ID of the relevant genome
     *
     * @return a PegList of all the protein sequences
     *
     * @throws IOException
     */
    public PegList readGenomeSequences(String genomeId) throws IOException {
        PegList retVal = null;
        File pegFile = new File(this.orgDir, genomeId + PEG_FILE_SUFFIX);
        if (! pegFile.isFile()) {
            if (this.debug) System.err.println("Could not find sequences for genome " + genomeId + ".");
        } else {
            // Here the genome exists.
            if (this.debug) System.err.println("Reading sequences for genome " + genomeId + ".");
            retVal = new PegList(pegFile);
        }
        return retVal;
    }

    /**
     * @return a map of peg IDs to functions for a genome
     *
     * @param the genome ID
     *
     * @throws IOException
     */
    public Map<String, String> getGenomeFunctions(String genomeId) throws IOException {
        Map<String, String> retVal = new HashMap<String, String>(6000);
        // This set will hold the deleted features.
        Set<String> deletedPegs = new HashSet<String>(100);
        File deleteFile = new File(this.orgDir, genomeId + DELETED_PEGS_FILE_SUFFIX);
        if (deleteFile.exists()) {
            if (this.debug) System.err.println("Reading deleted pegs for " + genomeId + ".");
            try (TabbedLineReader deleteReader = new TabbedLineReader(deleteFile, 1)) {
                for (TabbedLineReader.Line line : deleteReader) {
                    String peg = line.get(0);
                    deletedPegs.add(peg);
                }
            }
        }
        // Now, pull in all the un-deleted pegs, and map each peg to its function.  Because we are
        // storing the pegs in a map, only the last function will be kept, which is desired behavior.
        File functionFile = new File(this.orgDir, genomeId + FUNCTION_FILE_SUFFIX);
        try (TabbedLineReader functionReader = new TabbedLineReader(functionFile, 2)) {
            if (this.debug) System.err.println("Reading assigned functions for " + genomeId + ".");
            for (TabbedLineReader.Line line : functionReader) {
                String peg = line.get(0);
                if (peg.contains("peg") && ! deletedPegs.contains(peg)) {
                    retVal.put(peg, line.get(1));
                }
            }
        }
        return retVal;
    }

    /**
     * @return an object for iterating through all the genomes
     */
    public Iterable<String> getGenomes() {
        if (this.debug) System.err.println("Reading genomes from " + this.orgDir + ".");
        OrganismDirectories retVal = new OrganismDirectories(this.orgDir);
        if (this.debug) System.err.println(retVal.size() + " genomes found.");
        return retVal;
    }

    /**
     * @return the genome ID for a feature ID
     *
     * @param fid	the feature ID from which the genome ID is to be computed
     */
    public static String genomeOf(String fid) {
        Matcher matcher = GENOME_ID_PATTERN.matcher(fid);
        String retVal = null;
        if (matcher.matches()) {
            retVal = matcher.group(1);
        }
        return retVal;
    }

}
