/**
 *
 */
package org.theseed.locations;

/** This represents a location on the minus strand.
 *
 * @author Bruce Parrello
 *
 */
public class BLocation extends Location {

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

}
