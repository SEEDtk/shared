/**
 *
 */
package org.theseed.counters;

import java.util.Collection;
import java.util.Iterator;

/**
 * This method creates a more-or-less equally-spaced iterator through a collection.  The constructor takes as input the
 * collection and the number of desired output objects.  It will attempt to choose objects from the list that are
 * to the extent possible equally far apart.  The collection should not be changed while iterating.
 *
 * @author Bruce Parrello
 *
 */
public class Spacer<T> implements Iterator<T> {

    // FIELDS
    /** iterator over the collection through which we are iterating */
    private Iterator<T> iter;
    /** number of objects already found for output */
    private int foundCount;
    /** number of objects already scanned */
    private int scanCount;
    /** optimal space between objects */
    private double space;
    /** next object to return */
    private T nextObject;

    /**
     * Construct a spaced iterator.
     *
     * @param source		source collection over which to iterate
     * @param count			number of objects to return
     */
    public Spacer(Collection<T> source, int count) {
        this.space = source.size() / (double) count;
        // Here the user wants more than the collection holds, so we just give him the whole list.
        if (this.space < 1.0)
            this.space = 1.0;
        // Denote we haven't returned anything yet.
        this.iter = source.iterator();
        this.foundCount = 0;
        this.scanCount = 0;
        // We need to get the first object, since the first object has index 0, and that's the scan count.
        this.nextObject = this.safeIter();
        // Find the first object to return.
        this.lookAhead();
    }


    /**
     * Scan ahead until we find the next object to return.
     */
    private void lookAhead() {
        // Compute the index of the next object to return.  Since the space value is >= 1.0, this will always
        // be strictly greater than the last index returned.
        int nextIndex = (int) ((this.foundCount + 0.5) * this.space);
        // Scan until we find the desired object.  We pretend that there is nothing but nulls past the end of the
        // list.
        while (this.scanCount < nextIndex) {
            this.nextObject = this.safeIter();
            this.scanCount++;
        }
        // Denote we've found a new one.
        this.foundCount++;
    }


    /**
     * @return the next object in the list, or NULL if we are at the end
     */
    public T safeIter() {
        return this.iter.hasNext() ? iter.next() : null;
    }

    @Override
    public boolean hasNext() {
        return (this.nextObject != null);
    }

    @Override
    public T next() {
        T retVal = this.nextObject;
        this.lookAhead();
        return retVal;
    }

}
