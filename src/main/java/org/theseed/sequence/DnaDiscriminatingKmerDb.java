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

    @Override
    public void addGenome(Genome genome, String groupId) {
        for (Contig contig : genome.getContigs()) {
            String seq = contig.getSequence().toLowerCase();
            this.addContig(groupId, seq);
        }
    }

    /**
     * Add a contig to the kmer database.  Only the forward strand will be processed.
     * When we count hits, we check the incoming sequence in both directions.
     *
     * @param groupId	ID of the group to which the contig belongs
     * @param seq		DNA sequence of the contig
     */
    public void addContig(String groupId, String seq) {
        this.addSequence(seq, groupId);
    }

    @Override
    public CountMap<String> countHits(String contigSequence) {
        var sequences = new ArrayList<String>(2);
        String normalized = contigSequence.toLowerCase();
        sequences.add(normalized);
        sequences.add(Contig.reverse(normalized));
        return super.countSeqHits(sequences);
    }

    @Override
    protected boolean isClean(String kmer) {
        return DnaKmers.isClean(kmer);
    }

}
