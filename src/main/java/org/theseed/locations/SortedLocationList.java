/**
 *
 */
package org.theseed.locations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * This is a sorted list of locations based on an array list.
 *
 * @author Bruce Parrello
 *
 */
public class SortedLocationList implements Iterable<Location> {

    // FIELDS

    /** underlying array list */
    private ArrayList<Location> list;

    /**
     * Create an empty location list.
     */
    public SortedLocationList() {
        this.list = new ArrayList<Location>();
    }

    /**
     * Create a location list with a specified capacity.
     */
    public SortedLocationList(int capacity) {
        this.list = new ArrayList<Location>(capacity);
    }

    /**
     * Create a location list from a collection.
     */
    public SortedLocationList(Location... locs) {
        this.list = new ArrayList<Location>(locs.length);
        // We use our add to insure the sort is maintained.
        this.addAll(locs);
    }

    /**
     * Add a location to the list.
     */
    public void add(Location loc) {
        int i = Collections.binarySearch(this.list, loc);
        if (i < 0) {
            // The location was not found.  Convert the return to the insertion point value.
            i = -i - 1;
        }
        // Add the new location at the insertion point.  If a matching location exists, the new one goes first.
        this.list.add(i, loc);
    }

    /**
     * Add a whole bunch of locations. Return TRUE if successful.
     */
    public void addAll(Location... c) {
        for (Location loc : c) this.add(loc);
    }

    /**
     * Empty the list.
     */
    public void clear() {
        this.list.clear();
    }

    /**
     * Check for an item in the list.  We can use binary search here.
     */
    public boolean contains(Location loc) {
        int i = Collections.binarySearch(this.list, loc);
        return (i >= 0);
    }

    /**
     * @return the location at the specified position, or NULL if the position is invalid.
     */
    public Location get(int idx) {
        Location retVal = null;
        if (idx >= 0 && idx < this.list.size())
            retVal = this.list.get(idx);
        return retVal;
    }

    /**
     * @return an iterator through this list.
     */
    @Override
    public Iterator<Location> iterator() {
        return this.list.iterator();
    }

    /**
     * @return the number of locations in the list
     */
    public int size() {
        return this.list.size();
    }

    /**
     * @return the list as an array
     */
    public Location[] toArray() {
        Location[] retVal = new Location[this.list.size()];
        return this.list.toArray(retVal);
    }

    /**
     * @return 	a collection of the locations in the list at index i or greater than have
     * 			the same contig ID
     *
     * @param i	start point for the collection returned
     */
    public List<Location> contigRange(int i) {
        // Find the end of the contig range.
        String contig = this.get(i).getContigId();
        int j = i + 1;
        while (j < this.size() && contig.contentEquals(this.get(j).getContigId())) j++;
        // Return everything from i through j exclusive.
        return this.list.subList(i, j);
    }

    /** Delete a location from the list.
     *
     * @param loc	location to delete
     */
    public void remove(Location loc) {
        int i = Collections.binarySearch(this.list, loc);
        if (i >= 0)
            this.list.remove(i);
    }

    /**
     * @return this location list as a stream of locations
     */
    public Stream<Location> stream() {
        return this.list.stream();
    }

}
