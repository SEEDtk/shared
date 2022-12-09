/**
 *
 */
package org.theseed.sequence;

import java.util.ArrayList;
import java.util.Collection;
import org.theseed.counters.CountMap;
import org.theseed.genome.Contig;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.proteins.DnaTranslator;

/**
 * This is a discriminating kmer database for protein kmers.  The incoming DNA sequence is translated six times-- once
 * per frame for each strand.
 *
 * @author Bruce Parrello
 *
 */
public class ProteinDiscriminatingKmerDb extends DiscriminatingKmerDb {

    public ProteinDiscriminatingKmerDb(int kmerSize) {
        super(kmerSize);
    }

    /**
     * Add a genome to a protein discriminating kmer database.  All of the pegs will be processed as protein sequences.
     *
     * @param genome	genome to add
     */
    public void addGenome(Genome genome) {
        String genomeId = genome.getId();
        for (Feature feat : genome.getPegs()) {
            String prot = feat.getProteinTranslation();
            this.addSequence(prot, genomeId);
        }
    }

    /**
     * Count the protein hits in a DNA sequence.
     *
     * @param contigSequence	a DNA sequence whose kmer hits are to be counted
     * @param gc				genetic code for protein translation
     *
     * @return a count map of group IDs to hit counts
     */
    public CountMap<String> countHits(String contigSequence, int gc) {
        // Create a translator for this genetic code.
        DnaTranslator xlate = new DnaTranslator(gc);
        // Get the reverse compliment.
        String revSeq = Contig.reverse(contigSequence);
        // Get the sequence length.  The translator simply ignores fragments at the end.
        final int seqLen = contigSequence.length();
        // Translate the three frames.
        Collection<String> seqs = new ArrayList<String>(6);
        for (int i = 1; i <= 3; i++) {
            seqs.add(xlate.translate(contigSequence, i, seqLen));
            seqs.add(xlate.translate(revSeq, i, seqLen));
        }
        // Return the counts.
        return super.countHits(seqs);
    }

}
