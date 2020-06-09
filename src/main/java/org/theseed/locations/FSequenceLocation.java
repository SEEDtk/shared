/**
 *
 */
package org.theseed.locations;

import org.theseed.genome.Genome;

/**
 * This is a sequence-location created on the plus strand.  It is generally constructed from a location object.
 *
 * @author Bruce Parrello
 *
 */
public class FSequenceLocation extends SequenceLocation {

    /**
     * Construct a sequence-location on the plus strand.
     *
     * @param sequence	DNA of the contig containing the location
     * @param loc		location of interest
     * @param gc		genetic code of the containing genome
     */
    private FSequenceLocation(String sequence, FLocation loc, int gc) {
        super(sequence, loc, gc);
    }

    /**
     * @return a new sequence-location from a plus-strand location in a genome.
     *
     * @param loc		location of interest
     * @param genome	genome containing the location
     */
    public static FSequenceLocation create(FLocation loc, Genome genome) {
        String sequence = genome.getContig(loc.getContigId()).getSequence();
        return new FSequenceLocation(sequence, loc, genome.getGeneticCode());
    }

    @Override
    public Location realLocation(int left, int right) {
        return Location.create(this.getContigId(), left, right);
    }

    @Override
    public String positionString() {
        return this.getContigId() + "+" + Integer.toString(this.getPosition());
    }

    @Override
    public int getRealPosition() {
        return this.getPosition();
    }

    @Override
    public String endString() {
        return this.getContigId() + "+" + Integer.toString(this.getEndPosition());
    }

    @Override
    public int getBegin(Location loc) {
        return loc.getBegin();
    }

}
