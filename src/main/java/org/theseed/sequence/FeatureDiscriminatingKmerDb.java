/**
 *
 */
package org.theseed.sequence;

import org.theseed.genome.Feature;
import org.theseed.genome.Genome;

/**
 * This is a DNA discriminating kmer database, but only sequences in CDS and RNA features are included.
 * This helps to insure that the kmers found are in areas that are more tightly conserved.
 *
 * @author Bruce Parrello
 *
 */
public class FeatureDiscriminatingKmerDb extends DnaDiscriminatingKmerDb {

    public FeatureDiscriminatingKmerDb(int kmerSize) {
        super(kmerSize);
    }

    @Override
    public void addGenome(Genome genome, String groupId) {
        for (Feature feat : genome.getFeatures()) {
            switch (feat.getType()) {
            case "rna" :
            case "CDS" :
                String dna = feat.getDna().toLowerCase();
                this.addSequence(dna, groupId);
            }
        }
    }

}
