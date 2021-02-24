/**
 *
 */
package org.theseed.counters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This manages a list of the highest-rated object in a set.  The list is sorted from highest rating to lowest.
 * An attempt is made to keep the list at or below a specified length.  However, if the entries at the end all
 * have the same rating, the list can grow a little larger so that all the ties are captured.
 *
 * @author Bruce Parrello
 *
 */
public class RatingList<E extends Comparable<E>> implements Iterable<Rating<E>> {

    // FIELDS
    /** sorted list of ratings */
    private List<Rating<E>> list;
    /** desired maximum rating count */
    private int maxRatings;

    /**
     * Construct an empty ratings list.
     *
     * @param size		desired maximum list size
     */
    public RatingList(int size) {
        this.maxRatings = size;
        // We leave a little extra room for objects with identical ratings.
        this.list = new ArrayList<Rating<E>>(size + 10);
    }

    @Override
    public Iterator<Rating<E>> iterator() {
        return this.list.iterator();
    }

    /**
     * @return an ordered stream of the highest-rated objects
     */
    public Stream<Rating<E>> stream() {
        return this.list.stream();
    }

    /**
     * Add a new object to the list.
     *
     * @param key		object being rated
     * @param rating	rating of object
     */
    public RatingList<E> add(E key, double rating) {
        Rating<E> entry = new Rating<E>(key, rating);
        if (this.list.size() < this.maxRatings) {
            // Here the list is below the maximum size, so we add the new rating regardless.
            this.insert(entry);
        } else {
            // Get the index of the last list entry.
            int lastN = this.list.size() - 1;
            // Get the rating of the last entry.
            double lastRating = this.list.get(lastN).getRating();
            if (rating == lastRating) {
                // The new entry has the same rating as the last entry, so add it at the end.
                this.list.add(entry);
            } else if (rating > lastRating) {
                // Here the list is full, and we need to insert in the middle.
                this.insert(entry);
                lastN++;
                // Find the last entry with the same rating as the last item.
                while (lastN >= 0 && this.list.get(lastN).getRating() == lastRating) lastN--;
                lastN++;
                // If it's past the end, delete it and everything following.
                if (lastN >= this.maxRatings)
                    this.list.subList(lastN, this.list.size()).clear();
            }
        }
        return this;
    }

    /**
     * Insert the specified entry in the proper position of the output list.
     *
     * @param entry		rating entry to insert.
     */
    private void insert(Rating<E> entry) {
        // Search for the insertion point.
        int iPoint = Collections.binarySearch(this.list, entry);
        // Convert a not-found result to the insertion point.
        if (iPoint < 0) iPoint = -1 - iPoint;
        // Insert the entry.
        this.list.add(iPoint, entry);
    }

    /**
     * @return a sorted list of the highest-rated items
     */
    public List<E> getBest() {
        return this.list.stream().map(x -> x.getKey()).collect(Collectors.toList());
    }

    /**
     * @return the size of this list
     */
    public int size() {
        return this.list.size();
    }

}
