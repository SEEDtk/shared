/**
 *
 */
package org.theseed.locations;

import org.theseed.genome.Genome;

/**
 * This is a sequence-location object on the minus strand.  Everything is reverse-complemented.
 *
 * @author Bruce Parrello
 *
 */
public class BSequenceLocation extends SequenceLocation {

    /**
     * Construct a new minus-strand sequence-location object
     *
     * @param sequence	reverse-complemented contig sequence
     * @param loc		converse of original location
     * @param gc		genetic code of genome
     */
    private BSequenceLocation(String sequence, FLocation loc, int gc) {
        super(sequence, loc, gc);
    }

    /**
     * @return a new sequence-location object for a location on the minus strand
     *
     * @param loc		location of interest
     * @param genome	genome containing the location
     */
    public static BSequenceLocation create(Location loc, Genome genome) {
        String sequence = genome.getContig(loc.getContigId()).getRSequence();
        FLocation floc = (FLocation) loc.converse(sequence.length());
        return new BSequenceLocation(sequence, floc, genome.getGeneticCode());
    }

    @Override
    public Location realLocation(int left, int right) {
        int offset = this.getSeqLen() + 1;
        return Location.create(this.getContigId(), offset - left, offset - right);
    }

    @Override
    public String positionString() {
        int realPosition = this.getSeqLen() + 1 - this.getPosition();
        return this.getContigId() + "-" + Integer.toString(realPosition);
    }

    @Override
    public int getRealPosition() {
        return this.getSeqLen() + 1 - this.getPosition();
    }

    @Override
    public String endString() {
        int endPosition = this.getSeqLen() + 1 - this.getEndPosition();
        return this.getContigId() + "-" + Integer.toString(endPosition);
    }

    @Override
    public int getBegin(Location loc) {
        return loc.converse(this.getSeqLen()).getBegin();
    }

}
