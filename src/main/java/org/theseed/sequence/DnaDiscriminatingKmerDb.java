/**
 *
 */
package org.theseed.sequence;

import java.util.ArrayList;

import org.theseed.counters.CountMap;
import org.theseed.genome.Contig;
import org.theseed.genome.Genome;

/**
 * This is a discriminating-kmer database for DNA kmers.  For each sequence, we count hits on both strands.
 *
 * @author Bruce Parrello
 *
 */
public class DnaDiscriminatingKmerDb extends DiscriminatingKmerDb {

    public DnaDiscriminatingKmerDb(int kmerSize) {
        super(kmerSize);
    }

    /**
     * Add a genome to the discriminating-kmer database.  Both strands of each contig will be added.
     *
     * @param genome	genome to process
     */
    public void addGenome(Genome genome) {
        String genomeId = genome.getId();
        for (Contig contig : genome.getContigs()) {
            String seq = contig.getSequence();
            this.addSequence(seq, genomeId);
            this.addSequence(Contig.reverse(seq), genomeId);
        }
    }

    /**
     * Count the hits in a DNA sequence.
     *
     * @param contigSequence	DNA sequence whose hits are to be counted
     *
     * @return a map of group IDs to hit counts
     */
    public CountMap<String> countHits(String contigSequence) {
        var sequences = new ArrayList<String>(2);
        sequences.add(contigSequence);
        sequences.add(Contig.reverse(contigSequence));
        return super.countHits(sequences);
    }

}
