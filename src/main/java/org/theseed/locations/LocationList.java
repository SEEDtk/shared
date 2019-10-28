/**
 *
 */
package org.theseed.locations;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.theseed.genome.Contig;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;

/**
 *
 * This object maintains a sorted list of non-overlapping locations on a contig.  The
 * locations are inserted by adding features from a genome.  If the feature is on a
 * different contig, it will be rejected.  Features with multiple regions will be
 * marked as invalid.  Overlapping locations will be separated out and marked as invalid.
 *
 * The resulting list can be used to compute coding frames for any kmer location on the
 * contig.
 *
 * Several things are always true about the locations in this list.  They are all on the same
 * contig.  They never overlap.  They are all single-segment.  These assumptions simplify the
 * operations on this object.
 *
 * @author Bruce Parrello
 */
public class LocationList implements Iterable<Location> {


    // FIELDS
    /** ID of the contig of interest */
    private String contigId;
    /** sorted list of locations */
    private TreeSet<Location> locations;
    /** utility location for point searches */
    private Location searchLoc;
    /** starts and stops on plus strand */
    private HashMap<Integer, Edge> plusEdges;
    /** starts and stops on minus strand */
    private HashMap<Integer, Edge> minusEdges;

    /** types of edges */
    public enum Edge { OTHER("other"), START("start"), STOP("stop");

        private String name;

        private Edge(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }

    };

    /**
     * Construct a location list for a specified contig.
     *
     * @param contigId	ID of the contig to which this location list is specific
     */
    public LocationList(String contigId) {
        this.contigId = contigId;
        this.locations = new TreeSet<Location>(new Location.Sorter());
        this.searchLoc = Location.create(contigId, "+", 1, 1);
        this.plusEdges = new HashMap<Integer, Edge>(200);
        this.minusEdges = new HashMap<Integer, Edge>(200);
    }

    /**
     * Add a new location to this location list.
     *
     * @param loc	location to add
     *
     * @return TRUE if the location was added, FALSE if it belongs to a different contig
     */
    public boolean addLocation(Location loc) {
        boolean retVal = false;
        if (contigId.equals(loc.getContigId())) {
            // Here we are on the same contig, so we can add the location.  Create a single-region
            // copy.
            Location regionLoc = loc.regionOf();
            // Invalidate it if it is segmented.
            if (loc.isSegmented()) {
                regionLoc.invalidate();
            }
            // Process the edges.
            if (loc.getDir() == '+') {
                this.plusEdges.put(regionLoc.getLeft(), Edge.START);
                this.plusEdges.put(regionLoc.getRight() - 2, Edge.STOP);
            } else {
                this.minusEdges.put(regionLoc.getRight(), Edge.START);
                this.minusEdges.put(regionLoc.getLeft() + 2, Edge.STOP);
            }
            // Now we need to merge it in.  The only tricky part to this is if there is an overlap,
            // we have to create invalid locations for the overlap area. Since there is no overlap
            // inside the list, for another location to overlap with this one, it must be either the
            // floor or ceiling. We remove the adjacent locations, resolve the overlaps, and then
            // add them back.
            Location before = this.locations.floor(regionLoc);
            if (before != null && before.getRight() >= regionLoc.getLeft()) {
                locations.remove(before);
                regionLoc = this.ResolveOverlap(before, regionLoc);
            }
            // There can only be one floor, but there may be many ceilings.  This is because
            // the floor's left is definitely to the left of our left, but the ceiling's right
            // could be inside our right, leaving room for more ceilings.  We have to do these
            // in a loop.
            Location after = this.locations.ceiling(regionLoc);
            while (after != null && after.getLeft() <= regionLoc.getRight()) {
                locations.remove(after);
                regionLoc = this.ResolveOverlap(regionLoc, after);
                after = this.locations.ceiling(regionLoc);
            }
            // Add what's left of the new location.
            locations.add(regionLoc);
            // Denote we've incorporated this location.
            retVal = true;
        }
        return retVal;
    }

    /**
     * Resolve overlaps for a pair of overlapping locations, and add all but the last to the
     * location list.
     *
     * @param loc1	leftmost overlapping location
     * @param loc2	rightmost overlapping location
     *
     * @return a location at the end that has not been incorporated into the list
     */
    private Location ResolveOverlap(Location loc1, Location loc2) {
        Location retVal;
        if (loc1.getRight() >= loc2.getRight()) {
            // Here the second location is wholly inside the first.  Mark it as invalid.
            loc2.invalidate();
            // Separate the part of loc1 that precedes loc2.
            if (loc1.getLeft() < loc2.getLeft()) {
                Location prefix = loc1.createEmpty();
                prefix.putRegion(loc1.getLeft(), loc2.getLeft() - 1);
                this.locations.add(prefix);
            }
            // Now, the big question here is, does loc2 extend past loc1.
            if (loc1.getRight() == loc2.getRight()) {
                // It does not.  We return loc2 and discard loc1.
                retVal = loc2;
            } else {
                // It does.  Add the new location.
                this.locations.add(loc2);
                // Shorten loc1 and save it as the residual.
                loc1.setLeft(loc2.getRight() + 1);
                retVal = loc1;
            }
        } else if (loc1.getLeft() == loc2.getLeft()) {
            // Here the two locations start at the same place, but the second location
            // extends past the first.  Invalidate the first and shorten the second
            // as the residual.
            loc1.invalidate();
            this.locations.add(loc1);
            loc2.setLeft(loc1.getRight() + 1);
            retVal = loc2;
        } else {
            // Here we have a partial overlap.  We shrink both locations, and create a new
            // one at the end to encompass the suffix.
            Location suffix = loc2.createEmpty();
            suffix.putRegion(loc1.getRight() + 1, loc2.getRight());
            retVal = suffix;
            loc1.setRight(loc2.getLeft() - 1);
            this.locations.add(loc1);
            loc2.setRight(suffix.getLeft() - 1);
            loc2.invalidate();
            this.locations.add(loc2);
        }
        return retVal;
    }

    /**
     * Compute the strand for a position. The strand is '+' or '-' for an occupied point,
     * and '0' for an unoccupied point.
     *
     * @param pos	a position on the contig
     * @return '+' 	if the position is in a forward strand location, '-' if it is in a
     * 				backward strand location, and '0' if it is between locations
     */
    public char computeStrand(int pos) {
        char retVal = '0';
        // Create a location for the target position.
        searchLoc.setRegion(pos, pos);
        // Find the floor-- that is, the last location before or on the position.
        // Because none of the locations overlap, it will be the only one that could
        // contain the position.
        Location floor = this.locations.floor(searchLoc);
        // If it covers our position, it is the strand we want.
        if (floor != null && floor.getRight() >= pos) {
            retVal = floor.getDir();
        }
        return retVal;
    }

    /**
     * @return an iterator for the locations in this list
     */
    @Override
    public Iterator<Location> iterator() {
        return this.locations.iterator();
    }

    /**
     * @return the contig ID for this location list
     */
    public String getContigId() {
        return this.contigId;
    }

    /**
     * @return the type of edge at the specified location
     *
     * @param pos		the location of interest
     * @param negative	TRUE if a negative strand edge should be considered
     */
    public Edge isEdge(int pos, boolean negative) {
        Edge retVal = Edge.OTHER;
        if (this.plusEdges.containsKey(pos)) {
            retVal = this.plusEdges.get(pos);
        } else if (negative && this.minusEdges.containsKey(pos)) {
            retVal = this.minusEdges.get(pos);
        }
        return retVal;
    }

    /**
     * Create a hashmap of LocationLists for all the contigs in a genome.
     *
     * @param genome	a genome object whose protein coding regions are to be encoded in location lists
     *
     * @return a collection of location lists, one per contig, with the coding regions defined
     */
    public static Map<String, LocationList> createGenomeCodingMap(Genome genome) {
        Collection<Contig> contigs = genome.getContigs();
        Map<String, LocationList> retVal = new HashMap<String, LocationList>();
        // Initialize the contig lists.
        for (Contig contig : contigs) {
            LocationList newList = new LocationList(contig.getId());
            retVal.put(contig.getId(), newList);
        }
        // Now run through the CDS features, adding them to the location lists.
        Collection<Feature> pegs = genome.getPegs();
        for (Feature feat : pegs) {
            Location loc = feat.getLocation();
            if (feat != null) {
                String contigId = loc.getContigId();
                LocationList contigList = retVal.get(contigId);
                if (contigList != null) {
                    contigList.addLocation(loc);
                }
            }
        }
        return retVal;
    }

    /**
     * @return the frame for a specified kmer location
     *
     * This method must find the location containing the start position of the kmer and then
     * compute the frame relative to that location.
     *
     * @param pos	start position of the kmer
     * @param end	end position of the kmer
     */
    public Frame computeRegionFrame(int pos, int end) {
        Frame retVal = Frame.XX;
        // Set the search location to the kmer limits.
        this.searchLoc.setRegion(pos, end);
        // Find the rightmost location that can include the search location.
        Location loc = this.locations.floor(this.searchLoc);
        if (loc == null) {
            // Here we are in front of the first coding region.
            retVal = Frame.F0;
        } else {
            // We found one. Use it to compute the frame.
            retVal = loc.regionFrame(pos, end);
        }
        return retVal;
    }
}
