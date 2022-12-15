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

    // FIELDS
    /** genetic code to use */
    private int geneticCode;
    /** DNA translator for that code */
    private DnaTranslator xlate;


    public ProteinDiscriminatingKmerDb(int kmerSize) {
        super(kmerSize);
        this.setGeneticCode(11);
    }

    @Override
    public void addGenome(Genome genome) {
        String genomeId = genome.getId();
        for (Feature feat : genome.getPegs()) {
            String prot = feat.getProteinTranslation();
            this.addSequence(prot, genomeId);
        }
    }

    @Override
    public CountMap<String> countHits(String contigSequence) {
        // Get the reverse compliment.
        String revSeq = Contig.reverse(contigSequence);
        // Get the sequence length.  The translator simply ignores fragments at the end.
        final int seqLen = contigSequence.length();
        // Translate the three frames.
        Collection<String> seqs = new ArrayList<String>(6);
        for (int i = 1; i <= 3; i++) {
            seqs.add(this.xlate.translate(contigSequence, i, seqLen));
            seqs.add(this.xlate.translate(revSeq, i, seqLen));
        }
        // Return the counts.
        return super.countSeqHits(seqs);
    }

    /**
     * @return the genetic code for protein translation
     */
    public int getGeneticCode() {
        return this.geneticCode;
    }

    /**
     * Specify a new genetic code for protein translation.
     *
     * @param gc 	the geneticCode to set
     */
    public void setGeneticCode(int gc) {
        this.geneticCode = gc;
        this.xlate = new DnaTranslator(gc);
    }

}
