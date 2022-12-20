/**
 *
 */
package org.theseed.genome;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This class manages a directory of GTO files and allows simple iterator through the genomes.
 * The GTO file names should consist of the genome ID with a suffix of ".gto", but this is
 * not required.  The files will be returned in name order, rather than genome ID order.
 *
 * @author Bruce Parrello
 */
public class GenomeDirectory implements Iterable<Genome> {

    // FIELDS

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(GenomeDirectory.class);

    /** name of this directory */
    private File dirName;

    /** list of the genome IDs */
    private TreeSet<String> genomeIDs;

    /** last file name read (not thread-safe) */
    private File gtoFile;

    /** filter for GTO files */
    private class GtoFilter implements FilenameFilter {

        @Override
        public boolean accept(File arg0, String arg1) {
            return (arg1.endsWith(".gto"));
        }

        /**
         * @return the genome ID portion of the file name
         *
         * @param fileName	the name of a GTO file
         */
        public String genomeId(String fileName) {
            int i = fileName.lastIndexOf('.');
            return fileName.substring(0, i);
        }

    }

    /**
     * Iterator that returns all the genomes in the directory, in order.
     *
     * @author Bruce Parrello
     *
     */
    public class GenomeIterator implements Iterator<Genome> {

        /** current position in the genome ID list */
        private Iterator<String> treePos;

        /** Initialize the iterator through the genome IDs. */
        public GenomeIterator() {
            this.treePos = genomeIDs.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.treePos.hasNext();
        }

        @Override
        public Genome next() {
            String nextID = this.treePos.next();
            Genome retVal = GenomeDirectory.this.getGenome(nextID);
            return retVal;
        }

    }

    /**
     * Construct a genome directory from a specified directory on disk.
     *
     * @param dirName	name of the directory to load
     * @throws IOException
     */
    public GenomeDirectory(String dirName) throws IOException {
        this.dirName = new File(dirName);
        setup();
    }

    /**
     * Initialize this object.  This mostly involves figuring out which genomes
     * are in the directory.
     *
     * @throws IOException
     */
    private void setup() throws IOException {
        // Verify that the directory exists.
        if (! this.dirName.isDirectory())
            throw new FileNotFoundException(dirName + " is not found or not a directory.");
        // Get the list of genome files.
        GtoFilter filter = new GtoFilter();
        String[] genomeFiles = this.dirName.list(filter);
        if (genomeFiles == null)
            throw new IOException("I/O error processing " + dirName + ".");
        // Put the genome IDs in the tree.
        this.genomeIDs = new TreeSet<String>();
        for (String genomeFile : genomeFiles) {
            String genomeId = filter.genomeId(genomeFile);
            this.genomeIDs.add(genomeId);
        }
    }

    /**
     * Construct a new genome directory (file parameter).
     *
     * @param inDir		directory containing the GTOs
     *
     * @throws IOException
     */
    public GenomeDirectory(File inDir) throws IOException {
        this.dirName = inDir;
        setup();
    }

    /**
     * @return a list of the genome IDs
     */
    public Set<String> getGenomeIDs() {
        return this.genomeIDs;
    }


    @Override
    public Iterator<Genome> iterator() {
        return new GenomeIterator();
    }

    /**
     * @return the number of genomes in the directory
     */
    public int size() {
        return this.genomeIDs.size();
    }

    @Override
    public String toString() {
        String retVal = this.dirName + " (" + this.size() + " genomes)";
        return retVal;
    }

    /**
     * This recalls the name of the last genome read, but it is useless in a multi-threaded environment.
     * We keep it for some older commands that do single-threaded iteration.
     *
     * @return the name of the last file read
     */
    public File currFile() {
        return this.gtoFile;
    }

    /**
     * @return the genome with the specified ID
     *
     * @param genomeId
     */
    public Genome getGenome(String genomeId) {
        // Build the genome file name.
        File gFile = this.getGenomeFile(genomeId);
        // Read the genome.  Note we have to percolate some checked exceptions.
        Genome retVal;
        try {
            log.debug("Reading genome from {}.", gFile);
            retVal = new Genome(gFile);
            this.gtoFile = gFile;
        } catch (NumberFormatException | IOException e) {
            throw new RuntimeException("Error processing genomes.", e);
        }
        return retVal;
    }

    /**
     * @return the file name of a genome
     *
     * @param genomeId	ID of the target genome
     */
    private File getGenomeFile(String genomeId) {
        return new File(dirName, genomeId + ".gto");
    }

    /**
     * If the GTOs all have file names consisting of the genome ID with a suffix of ".gto", this
     * method can be used to determine if a genome is already present.
     *
     * @param genomeId	ID of the genome of interest
     *
     * @return TRUE if the genome exists, else FALSE
     */
    public boolean contains(String genomeId) {
        return this.genomeIDs.contains(genomeId);
    }

    /**
     * Store a new genome in this directory.
     *
     * @param genome	genome to store
     */
    public void store(Genome genome) throws IOException {
        File gFile = this.getGenomeFile(genome.getId());
        genome.save(gFile);
        this.genomeIDs.add(genome.getId());
    }

    /**
     * @return the directory name as a string
     */
    public String getName() {
        return this.dirName.toString();
    }

    /**
     * Delete a genome from this directory.
     *
     * @param genomeId	ID of the genome to delete
     *
     * @throws IOException
     */
    public void remove(String genomeId) throws IOException {
        // Compute the genome file name.
        File gFile = this.getGenomeFile(genomeId);
        if (gFile.exists()) {
            // Here the genome exists in this directory.  Delete it and update the ID set.
            FileUtils.forceDelete(gFile);
            this.genomeIDs.remove(genomeId);
        }
    }

}
