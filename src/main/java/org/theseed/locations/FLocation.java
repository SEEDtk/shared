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

    /**
     * Create a new forward strand location at a specific place.
     *
     * @param contigId	ID of the contig containing location
     * @param begin		start position (1-based)
     * @param end		end position (1-based)
     */
    public FLocation(String contigId, int begin, int end) {
        super(contigId);
        this.putRegion(begin, end);
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
        int gc = genome.getGeneticCode();
        return extend(sequence, gc);
    }

    /**
     * Extend this location to a start and a stop.
     *
     * @param sequence		DNA sequence containing the location
     * @param gc			genetic code of the sequence
     *
     * @return a new location that includes a start and a stop, or NULL if that is not possible
     */
    protected FLocation extend(String sequence, int gc) {
        // Compute the length of the sequence.
        int contigEnd = sequence.length();
        // We will set this to the new location if we succeed.
        FLocation retVal = null;
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
                retVal = (FLocation) Location.create(this.contigId, "+", newLeft, newRight);
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

    @Override
    public SequenceLocation createSequenceLocation(Genome genome) {
        return FSequenceLocation.create(this, genome);
    }

    @Override
    protected CodonSet getStops(int gc) {
        return DnaTranslator.STOPS[gc];
    }

    @Override
    protected int calcUpstreamDistance(Location other) {
        return this.getLeft() - other.getRight() - 1;
    }

    @Override
    public Location upstream(int gap) {
        FLocation retVal = new FLocation(this.contigId);
        retVal.addRegion(this.getBegin() - gap, gap);
        return retVal;
    }

    @Override
    protected boolean isUpstreamCheck(Location other) {
        return (this.getRight() < other.getLeft());
    }

    @Override
    public Location subLocation(int offset, int len) {
        // Create a new location.
        Location retVal = new FLocation(this.contigId);
        // Add regions until will fill the space.
        int used = 0;
        int curr = 0;
        for (int i = 0; used < len && i < this.regions.size(); i++) {
            Region region = this.regions.get(i);
            // Only use this region if it contains data to the right of the new left.
            int next = curr + region.getLength();
            if (next > offset) {
                // Constrain the left edge.
                int left = region.getLeft();
                if (curr < offset) left = region.getLeft() + offset - curr;
                // Constrain the right edge.
                int right = region.getRight();
                int next2 = used + right + 1 - left;
                if (next2 > len) right = right - (next2 - len);
                // Store the new region.
                retVal.putRegion(left, right);
                // Update the counters.
                int newLen = right + 1 - left;
                used += newLen;
            }
            curr= next;
        }
        return retVal;
    }

    @Override
    public Location expandUpstream(int upstream, int contigLen) {
        Location retVal = (Location) this.clone();
        retVal.expand(upstream, 0, contigLen);
        return retVal;
    }


}
