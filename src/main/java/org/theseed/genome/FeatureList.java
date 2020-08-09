/**
 *
 */
package org.theseed.genome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.theseed.locations.Location;

/**
 * This represents a list of features sorted by location.  Methods are provided to return the
 * features in specified regions.  The list is immutable once created.
 *
 * @author Bruce Parrello
 *
 */
public class FeatureList implements Iterable<Feature> {

    // FIELDS
    /** length of the longest feature added */
    int longest;
    /** underlying set of features, ordered by location left point */
    ArrayList<Feature> features;
    /** relevant contig ID */
    String contigId;

    /**
     * Construct a list of all the features in a genome on a particular contig,
     * sorted by location
     *
     * @param genome	the genome containing the features
     * @param contigId	the ID of the relevant contig
     */
    public FeatureList(Genome genome, String contigId) {
        // Load the features from the contig.
        init(genome, contigId);
    }

    /**
     * Initialize this list with all the features in the specified contig.
     *
     * @param genome	the genome containing the features
     * @param contigId	the ID of the relevant contig
     */
    private void init(Genome genome, String contigId) {
        // Save the contig ID.
        this.contigId = contigId;
        // Put all the features from the contig into the feature list.
        this.features = new ArrayList<Feature>();
        this.longest = 0;
        for (Feature feat : genome.getFeatures()) {
            Location floc = feat.getLocation();
            if (floc.isContig(contigId)) {
                this.features.add(feat);
                if (floc.getLength() > longest) {
                    longest = floc.getLength();
                }
            }
        }
        // Sort all the features by leftmost location point.
        this.features.sort(new Feature.LocationComparator());
    }


    /**
     * Construct a list of all the features in a genome on a particular contig,
     * sorted by location.
     *
     * @param genome	the genome containing the features
     * @param contigId	the contig of interest
     */
    public FeatureList(Genome genome, Contig contig) {
        // Load the features from the contig.
        init(genome, contig.getId());
    }


    @Override
    public Iterator<Feature> iterator() {
        return this.features.iterator();
    }

    /**
     * @return the number of features in this list
     */
    public int size() {
        return this.features.size();
    }

    /**
     * @return the left edge of the specified feature
     *
     * @param idx	index of the feature whose left edge is desired
     */
    private int lEdge(int idx) {
        return this.features.get(idx).getLocation().getLeft();
    }

    /**
     * @return 	the index of the leftmost feature that starts at or to the right of the specified point,
     * 			or -1 if there is no such feature
     *
     * This uses a binary search.  Note that we cannot call it if the array is empty, because then the
     * lidx parameter cannot exist.
     *
     * @param pos	position in the contig of interest
     * @param lidx	index of a feature known to be to the left of the position
     */
    private int firstInside(int pos, int lidx) {
        int retVal = -1;
        int l = lidx;
        int r = this.features.size() - 1;
        int rPos = this.lEdge(r);
        if (rPos >= pos) {
            // Here there is a feature in the target range.  We are guaranteed that rPos >= pos at all times.
            while (l < r) {
                // Here there is space between both features.  We need to move them closer.
                int m = (r + l) >> 1;
                int mPos = this.lEdge(m);
                if (pos < mPos) {
                    r = m;
                    rPos = mPos;
                } else {
                    l = m + 1;
                    int lPos = this.lEdge(l);
                    if (lPos >= pos) {
                        r = l;
                        rPos = lPos;
                    }
                }
            }
            // We may need to roll back to find the first feature with the same left edge.
            // If r is greater than lidx, then we can back up one feature one position too far to find the
            // limit point.
           while (this.lEdge(r) >= pos) r--;
           retVal = r + 1;
        }
        return retVal;
    }


    /**
     * @return a collection of all features that overlap the given region
     *
     * @param left	left position of region
     * @param right	right position of region
     */
    public Collection<Feature> inRegion(int left, int right) {
        ArrayList<Feature> retVal = new ArrayList<Feature>();
        // Only search if the list is nonempty.
        if (this.size() > 0) {
            // Compute the earliest point at which a feature can start to overlap the region.
            int farLeft = left - this.longest;
            int lIdx = 0;
            if (this.lEdge(0) < farLeft) {
                lIdx = this.firstInside(farLeft, 0);
            }
            if (lIdx >= 0) {
                // Now lIdx is the index of the first feature that can possibly overlap the region.
                // Insure that it is not too far to the right.
                int rIdx = lIdx;
                if (this.lEdge(lIdx) < right) {
                    // We need to search for the last feature that can overlap.
                    rIdx = this.firstInside(right, lIdx);
                    // No feature is out of range, so check to the end of the contig.
                    if (rIdx < 0) rIdx = this.size() - 1;
                }
                // Now we check the features from lIdx to rIdx, keeping the features that overlap.
                Location testLoc = Location.create(this.contigId, "+", left, right);
                for (int i = lIdx; i <= rIdx; i++) {
                    Feature feat = this.features.get(i);
                    if (testLoc.distance(feat.getLocation()) < 0) {
                        retVal.add(feat);
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * @return TRUE if there is a feature overlapping the specified location's region on the same strand
     *
     * @param loc	location to check
     */
    public boolean isOccupied(Location loc) {
        Collection<Feature> overlaps = this.inRegion(loc.getLeft(), loc.getRight());
        Iterator<Feature> iter = overlaps.iterator();
        boolean retVal = false;
        while (! retVal && iter.hasNext())
            retVal = (iter.next().getLocation().getDir() == loc.getDir());
        return retVal;
    }

    /**
     * This nested class represents a current position in the feature list.  The client can ask
     * for the feature at this position, can move the position forward, and can iterate through
     * all the features within a certain distance to the right.
     *
     * @author Bruce Parrello
     *
     */
    public class Position {

        // FIELDS
        /** position of next feature to return */
        private int position;
        /** right edge of current feature */
        private int rEdge;

        /**
         * Initialize at the beginning of this feature list.
         *
         * @param distance	distance limit for iteration at this position
         */
        public Position() {
            this.position = 0;
            this.rEdge = 0;
        }

        /**
         * @return TRUE if this is not the last position in the feature list.
         */
        public boolean hasNext() {
            return this.position < size();
        }

        /**
         * @return the next feature in the feature list
         */
        public Feature next() {
            Feature retVal = features.get(position);
            this.rEdge = retVal.getLocation().getRight();
            position++;
            return retVal;
        }

        /**
         * @return a collection of the features within a certain distance after this one
         */
        public Collection<Feature> within(int distance) {
            ArrayList<Feature> retVal = new ArrayList<Feature>();
            if (position <= size() && position > 0) {
                // Compute the distance from our right edge.
                int limit = this.rEdge + distance + 1;
                for (int i = this.position; i < size() && lEdge(i) <= limit; i++) {
                    retVal.add(features.get(i));
                }
            }
            return retVal;
        }

    }

}
