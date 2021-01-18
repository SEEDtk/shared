/**
 *
 */
package org.theseed.locations;

import org.theseed.genome.Contig;
import org.theseed.genome.Genome;
import org.theseed.proteins.CodonSet;

/** This represents a location on the minus strand.
 *
 * @author Bruce Parrello
 *
 */
public class BLocation extends Location {

    /** array of start codon sets by genetic code */
    private static final CodonSet[] STARTS = new CodonSet[] { null,
            /*  1 */ new CodonSet("caa", "cag", "cat"),
            /*  2 */ new CodonSet("aat", "gat", "tat", "cat", "cac"),
            /*  3 */ new CodonSet("tat", "cat", "cac"),
            /*  4 */ new CodonSet("caa", "cag", "cat"),
            null, null, null, null, null, null,
            /* 11 */ new CodonSet("caa", "cac", "cat")
            };

    /** array of stop codon sets by genetic code */
    private static final CodonSet[] STOPS = new CodonSet[] { null,
            /*  1 */ new CodonSet("tta", "cta", "tca"),
            /*  2 */ new CodonSet("tta", "cta", "tct", "cct"),
            /*  3 */ new CodonSet("tta", "cta"),
            /*  4 */ new CodonSet("tta", "cta"),
            null, null, null, null, null, null,
            /* 11 */ new CodonSet("tta", "cta", "tca")
            };

    /** Create a new backward location.
     *
     * @param contigId	ID of the contig containing this location on the minus strand.
     */
    public BLocation(String contigId) {
        super(contigId);
    }

    @Override
    public int getBegin() {
        return this.getRight();
    }

    @Override
    public int getEnd() {
        return this.getLeft();
    }

    @Override
    public char getDir() {
        return '-';
    }

    @Override
    public void addRegion(int begin, int length) {
        // For a backward location, the begin is on the right.
        int left = begin - length + 1;
        int right = begin;
        this.putRegion(left, right);
    }

    @Override
    protected Frame calcFrame(int pos, int end, Region region) {
        return Frame.minusFrames[(region.getRight() - end) % 3];
    }

    @Override
    public Frame getFrame() {
        return Frame.minusFrames[2 - this.getRight() % 3];
    }


    @Override
    protected Location createEmpty() {
        BLocation retVal = new BLocation(this.contigId);
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
        // First we find a stop, moving backward from the left edge.  If there already is one, we are good.
        CodonSet stops = STOPS[gc];
        int newLeft = this.getLeft();
        while (newLeft > 0 && ! stops.contains(sequence, newLeft)) newLeft -= 3;
        // Only proceed if we found a stop.
        if (newLeft > 0) {
            // Search for a start, moving forward from the right edge.  Again, if there already is one, we
            // are good.
            CodonSet starts = STARTS[gc];
            int newRight = this.getRight();
            while (newRight <= contigEnd && ! starts.contains(sequence, newRight - 2)) newRight += 3;
            // Only proceed if we found a start. Also check for internal stops.
            if (newRight <= contigEnd && ! this.internalStops(sequence, gc, newLeft + 3, newRight)) {
                // We found what we want! Return the new location.
                retVal = Location.create(this.contigId, "-", newLeft, newRight);
            }
        }
        return retVal;
    }

    @Override
    public Location extendToOrf(Genome genome) {
        // Get the contig sequence for this location.
        Contig contig = genome.getContig(this.contigId);
        String sequence = contig.getSequence();
        int contigEnd = contig.length();
        int gc = genome.getGeneticCode();
        // Get the stops for this genetic code.
        CodonSet stops = STOPS[gc];
        // Build the ORF.
        Location retVal = this.findOrf(sequence, stops, contigEnd);
        return retVal;
    }

    @Override
    protected boolean internalStops(String sequence, int gc, int left, int right) {
        return Location.containsCodon(STOPS[gc], sequence, left, right);
    }

    @Override
    public void setBegin(int begin) {
        this.setRight(begin);
    }

    @Override
    public Location converse(int seqLen) {
        Location retVal = new FLocation(this.contigId);
        for (Region region : this.regions)
            retVal.addRegion(seqLen + 1 - region.getRight(), region.getLength());
        return retVal;
    }

    @Override
    public String getDna(String dna) {
        StringBuilder retVal = new StringBuilder(this.getLength());
        for (int i = this.getRegions().size() - 1; i >= 0; i--) {
            Region region = this.getRegions().get(i);
            retVal.append(dna.subSequence(region.getLeft() - 1, region.getRight()));
        }
        return Contig.reverse(retVal.toString());
    }

    @Override
    public SequenceLocation createSequenceLocation(Genome genome) {
        return BSequenceLocation.create(this, genome);
    }

    @Override
    protected CodonSet getStops(int gc) {
        return STOPS[gc];
    }

    @Override
    protected int calcUpstreamDistance(Location other) {
        return other.getLeft() - this.getRight() - 1;
    }

    @Override
    public Location upstream(int gap) {
        BLocation retVal = new BLocation(this.contigId);
        retVal.addRegion(this.getRight() + gap, gap);
        return retVal;
    }

    @Override
    protected boolean isUpstreamCheck(Location other) {
        return (this.getLeft() > other.getRight());
    }

    @Override
    public Location subLocation(int offset, int len) {
        // Create a new location.
        Location retVal = new BLocation(this.contigId);
        // Add regions until will fill the space.
        int used = 0;
        int curr = 0;
        for (int i = this.regions.size() - 1; used < len && i >= 0; i--) {
            Region region = this.regions.get(i);
            // Only use this region if it contains data to the left of the new right.
            int next = curr + region.getLength();
            if (next > offset) {
                // Constrain the right edge.
                int right = region.getRight();
                if (curr < offset) right = region.getRight() - (offset - curr);
                // Constrain the left edge.
                int left = region.getLeft();
                int next2 = used + right + 1 - left;
                if (next2 > len) left = left + (next2 - len);
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
        retVal.expand(0, upstream, contigLen);
        return retVal;
    }

    @Override
    public int offsetPoint(int offset) {
        return (this.getBegin() - offset);
    }

    @Override
    public Location downstream(int gap) {
        BLocation retVal = new BLocation(this.contigId);
        retVal.addRegion(this.getLeft() - 1, gap);
        return retVal;
    }


}
