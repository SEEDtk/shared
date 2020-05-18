/**
 *
 */
package org.theseed.locations;

import org.theseed.genome.Contig;
import org.theseed.genome.Genome;
import org.theseed.proteins.CodonSet;
import org.theseed.proteins.DnaTranslator;

/**
 * This represents a location on the plus strand.
 *
 * @author Bruce Parrello
 *
 */
public class FLocation extends Location {

    /**
     * Create a new forward strand location.
     *
     * @param contigId
     */
    public FLocation(String contigId) {
        super(contigId);
    }

    @Override
    public int getBegin() {
        return this.getLeft();
    }

    @Override
    public int getEnd() {
        return this.getRight();
    }

    @Override
    public char getDir() {
        return '+';
    }

    @Override
    public void addRegion(int begin, int length) {
        // For a forward location, the begin is on the left.
        int left = begin;
        int right = begin + length - 1;
        this.putRegion(left, right);
    }

    @Override
    protected Frame calcFrame(int pos, int end, Region region) {
        return Frame.plusFrames[(pos - region.getLeft()) % 3];
    }

    @Override
    public Frame getFrame() {
        return Frame.plusFrames[this.getLeft() % 3];
    }

    @Override
    protected Location createEmpty() {
        FLocation retVal = new FLocation(this.contigId);
        return retVal;
    }

    @Override
    public Location extend(Genome genome) {
        // Get the contig sequence for this location.
        Contig contig = genome.getContig(this.contigId);
        String sequence = contig.getSequence();
        int contigEnd = contig.length();
        int gc = genome.getGeneticCode();
        // We will set this to the new location if we succeed.
        Location retVal = null;
        // First, we find a stop, moving forward from the right edge.  If we already have one, it's ok.
        CodonSet stops = DnaTranslator.STOPS[gc];
        int newRight = this.getRight();
        while (newRight <= contigEnd && ! stops.contains(sequence, newRight - 2)) newRight += 3;
        // Only proceed if we found a stop.
        if (newRight <= contigEnd) {
            // Find a start, moving backward from the left edge.  If we already have one, it's ok., but a stop
            // in the way will fail the operation.
            CodonSet starts = DnaTranslator.STARTS[gc];
            int newLeft = this.getLeft();
            while (newLeft > 0 && ! starts.contains(sequence, newLeft)) newLeft -= 3;
            // Insure we found a start and there are no internal stops.
            if (newLeft > 0 && ! this.internalStops(sequence, gc, newLeft, newRight - 3)) {
                // Here it worked.  We can return the location.
                retVal = Location.create(this.contigId, "+", newLeft, newRight);
            }
        }
        return retVal;
    }

    @Override
    protected boolean internalStops(String sequence, int gc, int left, int right) {
        return Location.containsCodon(DnaTranslator.STOPS[gc], sequence, left, right);
    }

    @Override
    public void setBegin(int begin) {
        this.setLeft(begin);
    }

    @Override
    public Location converse(int seqLen) {
        Location retVal = new BLocation(this.contigId);
        for (Region region : this.regions)
            retVal.addRegion(seqLen + 1 - region.getLeft(), region.getLength());
        return retVal;
    }

    @Override
    public String getDna(String dna) {
        StringBuilder retVal = new StringBuilder(this.getLength());
        for (Region region : this.regions)
            retVal.append(dna.subSequence(region.getLeft() - 1, region.getRight()));
        return retVal.toString();
    }



}
