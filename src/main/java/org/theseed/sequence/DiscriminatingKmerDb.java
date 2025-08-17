/**
 *
 */
package org.theseed.sequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.counters.CountMap;
import org.theseed.genome.Genome;

/**
 * This object builds a database of discriminating kmers.  The client passes in sequences that belong to groups.
 * If a kmer belongs to more than one group, it is put in the common set.  Otherwise, it is mapped to its group.
 *
 * To save memory, use the "finish" method to delete the common set after the database is built.
 *
 * @author Bruce Parrello
 *
 */
public abstract class DiscriminatingKmerDb  {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(DiscriminatingKmerDb.class);
    /** map of discriminating kmers to groups */
    private Map<String, String> kmerMap;
    /** set of common kmers */
    private Set<String> commonSet;
    /** kmer size */
    private int kSize;

    /**
     * Construct a blank, empty discriminating kmer database.
     *
     * @param kmerSize	kmer size
     */
    public DiscriminatingKmerDb(int kmerSize) {
        this.kSize = kmerSize;
        this.kmerMap = new HashMap<String, String>();
        this.commonSet = new HashSet<String>();
    }

    /**
     * Add the kmers of a sequence to the database.
     *
     * @param sequence	sequence to add
     * @param group		associated group ID
     */
    public void addSequence(String sequence, String group) {
        int n = sequence.length() - this.kSize;
        for (int i = 0; i <= n; i++) {
            String kmer = sequence.substring(i, i + this.kSize);
            if (this.isClean(kmer) && ! this.commonSet.contains(kmer)) {
                // Here the kmer is not already known to be common.
                String kGroup = this.kmerMap.get(kmer);
                if (kGroup == null) {
                    // Here the kmer is completely new.  Connect it to the group.
                    this.kmerMap.put(kmer, group);
                } else if (! kGroup.contentEquals(group)) {
                    // Here the kmer is common.  Remove it from the map and denote it is common.
                    this.kmerMap.remove(kmer);
                    this.commonSet.add(kmer);
                }
            }
        }
    }

    /**
     * @return TRUE if the specified sequence has ambiguity characters, else FALSE
     *
     * @param kmer		sequence to check
     */
    protected abstract boolean isClean(String kmer);

    /**
     * Delete the common-kmer set to save memory.
     */
    public void finish() {
        this.commonSet.clear();
    }

    /**
     * @return a count map showing the kmers per group
     */
    public CountMap<String> getGroupCounts() {
        var retVal = new CountMap<String>();
        for (String group : this.kmerMap.values())
            retVal.count(group);
        return retVal;
    }

    /**
     * Count the kmer hits per group against a particular collection of sequences.  This is the basic internal
     * method from which all the various flavors are built.
     *
     * @param sequences		sequences whose hits are to be counted.
     *
     * @return a count map from each group ID to the number of hits found
     */
    protected CountMap<String> countSeqHits(Collection<String> sequences) {
        var retVal = new CountMap<String>();
        KmerSeries kmers = new KmerSeries(sequences, this.kSize);
        for (String kmer : kmers) {
            String group = this.kmerMap.get(kmer);
            if (group != null)
                retVal.count(group);
        }
        return retVal;
    }

    /**
     * Add a genome to the discriminating-kmer database.
     *
     * @param genome	genome to process
     */
    public abstract void addGenome(Genome genome, String groupId);

    /**
     * Count the hits in a DNA sequence.
     *
     * @param contigSequence	a DNA sequence whose kmer hits are to be counted
     *
     * @return a count map of group IDs to hit counts
     */
    public abstract CountMap<String> countHits(String contigSequence);

    /**
     * Erase ALL the kmers to save memory.  After this the database can be reused.
     */
    public void clear() {
        this.kmerMap.clear();
        this.commonSet.clear();
    }

}
