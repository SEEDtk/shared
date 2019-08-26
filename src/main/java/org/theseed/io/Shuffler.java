/**
 *
 */
package org.theseed.io;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * This is a minor extension to ArrayList that provides a method for shuffling the bulk of the array into the
 * first N positions.  It is used to make random selections from the array, then iterate them off.
 *
 * @author Bruce Parrello
 *
 */
public class Shuffler<T> extends ArrayList<T> {

    /**
     * serialization ID
     */
    private static final long serialVersionUID = 2842409846771927109L;

    /**
     * Construct a shuffling-arraylist with the specified capacity.
     *
     * @param cap	initial array list capacity
     */
    public Shuffler(int cap) {
        super(cap);
    }

    /**
     * Shuffle the array to randomize the content of the first N positions.
     *
     * @param size	number of positions to shuffle into
     */
    public void shuffle(int size) {
        Random randStream = new Random();
        // This will be the position we are currently filling.
        int i = 0;
        // Handle the edge case of a small array.
        if (this.size() <= size) size = this.size() - 1;
        // Loop until there is no more shuffling to do.
        while (i < size) {
            // Determine how much space is available to pick from.
            int remaining = this.size() - i;
            // Compute the place to pick from.
            int j = randStream.nextInt(remaining) + i;
            if (j != i) {
                T buffer = this.get(j);
                this.set(j, this.get(i));
                this.set(i, buffer);
            }
            i++;
        }
    }

    /**
     * Return an iterator for the first N elements of the array.
     *
     * @param limit		maximum number of elements to return
     */
    public Iterator<T> limitedIter(int limit) {
        return new Limited(limit);
    }

    /**
     * This is a simple utility method that adds an Iterable to the array list.
     *
     * @param iterable	set of things to add
     */
    public void addSequence(Iterable<T> iter) {
        for (T entry : iter) this.add(entry);
    }

    /**
     * This class iterates through the array but stops after a
     * certain number of elements.
     */
    public class Limited implements Iterator<T> {

        private int remaining;
        private Iterator<T> iter;

        protected Limited(int limit) {
            remaining = limit;
            iter = Shuffler.this.iterator();
        }

        @Override
        public boolean hasNext() {
            return (remaining > 0 && iter.hasNext());
        }

        @Override
        public T next() {
            remaining--;
            return (remaining >= 0 ? iter.next() : null);
        }

    }

}
