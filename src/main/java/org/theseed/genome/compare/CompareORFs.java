/**
 *
 */
package org.theseed.genome.compare;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Contig;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.genome.GenomeDirectory;
import org.theseed.locations.Location;
import org.theseed.sequence.MD5Hex;

/**
 * This class contains useful genome-comparison utilities, and is a base class for classes used to compare genomes on an ORF-by-ORF basis.
 *
 * @author Bruce Parrello
 *
 */
public abstract class CompareORFs {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(CompareORFs.class);
    /** comparator for sorting */
    protected Comparator<Feature> orfSorter;
    /** contig ID map from old-genome contig IDs to new-genome contig IDs */
    private Map<String, String> contigIdMap;
    /** MD5 computer */
    private MD5Hex md5Computer;

    /**
     * @return a map of genome sequence MD5s to genome files
     *
     * @param genomeDir		directory of the genome files
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public Map<String, File> getMd5GenomeMap(File genomeDir) throws IOException {

        GenomeDirectory genomes = new GenomeDirectory(genomeDir);
        Map<String, File> retVal = new HashMap<String, File>();
        for (Genome refGenome : genomes) {
            log.info("Scanning {}.", refGenome);
            // Get the MD5 for all the contig IDs.
            String key = this.md5Computer.sequenceMD5(refGenome);
            // Map the file name to the MD5.
            retVal.put(key, genomes.currFile());
        }
        return retVal;
    }

    /**
     * Comparator for sorting Features by contig, end point, strand.
     */
    public class OrfSorter implements Comparator<Feature> {

        @Override
        public int compare(Feature arg0, Feature arg1) {
            Location loc0 = arg0.getLocation();
            Location loc1 = arg1.getLocation();
            int retVal = loc0.getContigId().compareTo(loc1.getContigId());
            if (retVal == 0) {
                retVal = loc0.getEnd() - loc1.getEnd();
                if (retVal == 0)
                    retVal = loc0.getDir() - loc1.getDir();
            }
            return retVal;
        }
    }

    /**
     * Construct a blank genome comparison processor.
     *
     * @throws NoSuchAlgorithmException
     */
    public CompareORFs() throws NoSuchAlgorithmException {
        this.orfSorter = new OrfSorter();
        this.contigIdMap = new HashMap<String, String>();
        this.md5Computer = new MD5Hex();
    }

    /**
     * @return the sort comparison between the ORFs containing the two features
     *
     * @param f1	first feature
     * @param f2	second feature
     */
    public int orfCompare(Feature f1, Feature f2) {
        return this.orfSorter.compare(f1, f2);
    }

    /**
     * @return the next feature, or NULL at the end
     *
     * @param iter	iterator of interest
     */
    public Feature next(Iterator<Feature> iter) {
        return (iter.hasNext() ? iter.next() : null);
    }

    /**
     * @return TRUE if the genomes have identical contigs, else FALSE.
     *
     * If they have identical contigs, we will store an ID mapping for orf-sorting purposes.
     *
     * @param newGenome		new genome to check
     * @param oldGenome		old genome to compare it against
     *
     * @throws UnsupportedEncodingException
     */
    public boolean checkGenomes(Genome newGenome, Genome oldGenome) throws UnsupportedEncodingException {
        // Determine if the genomes can be compared.
        boolean retVal = (newGenome.getContigCount() == oldGenome.getContigCount());
        if (retVal) {
            // Here it is likely we can pull it off. Get a map from MD5s to new-genome contig IDs.
            Map<String, String> md5Map = new HashMap<String, String>(newGenome.getContigCount());
            for (Contig contig : newGenome.getContigs())
                md5Map.put(this.md5Computer.sequenceMD5(contig.getSequence()), contig.getId());
            // Loop through the old-genome contigs.
            for (Contig oldContig : oldGenome.getContigs()) {
                // Get the MD5 of this contig.
                String oldMd5 = this.md5Computer.sequenceMD5(oldContig.getSequence());
                String newContigId = md5Map.get(oldMd5);
                if (newContigId == null)
                    retVal = false;
                else
                    this.contigIdMap.put(oldContig.getId(), newContigId);
            }
            if (retVal) {
                // We're still okay.  Do we need to update feature locations?
                boolean updateNeeded = this.contigIdMap.entrySet().stream().anyMatch(x -> ! x.getKey().contentEquals(x.getValue()));
                if (updateNeeded) {
                    // Yes.  Go for it.
                    for (Feature peg : oldGenome.getPegs()) {
                        Location loc = peg.getLocation();
                        String oldContig = loc.getContigId();
                        String newContig = this.contigIdMap.get(oldContig);
                        if (newContig == null) {
                            log.error("Invalid contig ID {} in {}.", oldContig, peg.getId());
                            retVal = false;
                        }
                        loc.setContigId(newContig);
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * Get all the protein features of the genome sorted by ORF.
     *
     * @param genome	genome whose features are to be processed
     *
     * @return a sorted set of all the pegs in the genome
     */
    public SortedSet<Feature> sortFeatures(Genome genome) {
        SortedSet<Feature> retVal = new TreeSet<Feature>(this.orfSorter);
        retVal.addAll(genome.getPegs());
        return retVal;
    }

    /**
     * Process features that occupy the same ORF in both genomes.
     *
     * @param oldFeature	feature in the old genome
     * @param newFeature	feature in the new genome
     */
    abstract protected void both(Feature oldFeature, Feature newFeature);

    /**
     * Process a feature that occupies the ORF in the new genome that corresponds to an empty
     * ORF in the old genome.
     *
     * @param newFeature	feature in the new genome
     */
    abstract protected void newOnly(Feature newFeature);

    /**
     * Process a feature that occupies the ORF in the old genome that corresponds to an empty
     * ORF in the new genome.
     *
     * @param oldFeature	feature in the old genome
     */
    abstract protected void oldOnly(Feature oldFeature);

    /**
     * Initialize the data structures for a comparison.
     */
    abstract protected void initCompareData();

    /**
     * Compare the two genomes and store the statistics in this object.
     *
     * @param newGenome		new genome
     * @param compareGenome	original genome with different annotations
     *
     * @return TRUE if successful, FALSE if the genomes cannot be compared.
     *
     * @throws UnsupportedEncodingException
     */
    public boolean compare(Genome newGenome, Genome oldGenome) throws UnsupportedEncodingException {
        boolean retVal = checkGenomes(newGenome, oldGenome);
        if (retVal) {
            // The genomes are comparable. Load all their features into a sorted set.
            SortedSet<Feature> newOrfs = this.sortFeatures(newGenome);
            SortedSet<Feature> oldOrfs = this.sortFeatures(oldGenome);
            // Clear the counters (if needed).
            this.initCompareData();
            // Get iterators through the sets.
            Iterator<Feature> newIter = newOrfs.iterator();
            Iterator<Feature> oldIter = oldOrfs.iterator();
            Feature oldFeature = next(oldIter);
            Feature newFeature = next(newIter);
            while (oldFeature != null && newFeature != null) {
                int comp = orfCompare(oldFeature, newFeature);
                if (comp < 0) {
                    // Old feature is an orphan.
                    this.oldOnly(oldFeature);
                    oldFeature = next(oldIter);
                } else if (comp > 0) {
                    // New feature is an orphan.
                    this.newOnly(newFeature);
                    newFeature = next(newIter);
                } else {
                    this.both(oldFeature, newFeature);
                    // Advance both features.
                    oldFeature = next(oldIter);
                    newFeature = next(newIter);
                }
            }
            // Run out both iterators.
            while (newIter.hasNext()) {
                this.newOnly(newIter.next());
            }
            while (oldIter.hasNext()) {
                this.oldOnly(oldIter.next());
            }
        }
        return retVal;
    }


}
