/**
 *
 */
package org.theseed.locations;

import java.util.Collection;
import java.util.Iterator;

/** This class implements a single region on a contig.  Only the left and right positions (1-based) are given.
 *
 * @author Bruce Parrello
 *
 */
public class Region implements Comparable<Region> {

    // FIELDS
    private int left;
    private int right;

    /**
     * Construct a region from the two positions.
     */
    public Region(int left, int right) {
        this.left = left;
        this.right = right;
    }

    /**
     * @return the left position
     */
    public int getLeft() {
        return left;
    }

    /**
     * @return the right position
     */
    public int getRight() {
        return right;
    }

    /**
     * @param left	the left position to set
     */
    public void setLeft(int left) {
        this.left = left;
    }

    /**
     * @param right the right position to set
     */
    public void setRight(int right) {
        this.right = right;
    }

    /**
     * Shift this region one position to the right.
     */
    public void ShiftRight() {
        this.left++;
        this.right++;
    }

    /**
     * @return a string representation of this region
     */
    @Override
    public String toString() {
        String retVal = "[" + this.left + ", " + this.right + "]";
        return retVal;
    }

    /**
     * @return TRUE if a region with the same extent as this one exists in the collection
     */
    public boolean containedIn(Collection<Region> regions) {
        boolean retVal = false;
        Iterator<Region> iter = regions.iterator();
        while (! retVal && iter.hasNext()) {
            Region r = iter.next();
            if (r.getLeft() == this.left && r.getRight() == this.right) {
                retVal = true;
            }
        }
        return retVal;
    }
    /**
     * Compare two regions.  The left positions are compared first.  If they are equal, the longer
     * region compares first.
     *
     * @param other	the other region to compare
     */
    @Override
    public int compareTo(Region other) {
        int retVal = this.left - other.left;
        if (retVal == 0) {
            retVal = other.right - this.right;
        }
        return retVal;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.left;
        result = prime * result + this.right;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (! (obj instanceof Region))
            return false;
        Region other = (Region) obj;
        if (this.left != other.left)
            return false;
        if (this.right != other.right)
            return false;
        return true;
    }

    /**
     * @return the begin point for this region with respect to the given strand
     *
     * @param dir	strand (+ or -)
     */
    public int getBegin(String dir) {
        return (dir.contentEquals("+") ? this.left : this.right);
    }

    /**
     * @return the length of this region
     */
    public int getLength() {
        return (this.right - this.left + 1);
    }

}
