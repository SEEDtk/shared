/**
 *
 */
package org.theseed.locations;

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

}
