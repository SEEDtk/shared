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

    /** Construct a shuffling-arraylist from an iterator.
     *
     * @param source	an iterable object for initializing the list
     */
    public Shuffler(Iterable<T> source) {
        super();
        for (T item : source)
            this.add(item);
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
     * Rotate the array to the left by the specified number of positions starting with the given index.
     *
     * @param start		index of first position to move
     * @param distance	number of positions to rotate
     */
    public void rotate(int start, int distance) {
        // Compute the size of the array being rotated.
        int size = this.size() - start;
        // Convert the distance to a positive number.
        if (distance < 0)
            distance = size + distance;
        // Each (i) will move to (i + distance).  If this goes past the end, we subtract (size) to fix it.
        // We pull the target location, move the source to it, then find where the old target goes and repeat
        // until we have performed the correct number of moves.  A glitch occurs if we end up back at the
        // first position before finishing the array.  To detect this, we use a variable "orig".
        int rem = size;
        int orig = start;
        while (rem > 0) {
            int i = orig;
            T buffer = this.get(i);
            boolean loop = false;
            while (! loop) {
                int j = i + distance;
                if (j >= this.size()) j -= size;
                T newBuffer = this.get(j);
                this.set(j, buffer);
                i = j;
                buffer = newBuffer;
                rem--;
                loop = (i == orig);
            }
            orig++;
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
    public Shuffler<T> addSequence(Iterable<T> iter) {
        for (T entry : iter) this.add(entry);
        return this;
    }

    /**
     * This is a simple utility that adds a single member to the array list in a
     * fluent manner.
     *
     * @param member	new member to add
     */
    public Shuffler<T> add1(T member) {
        this.add(member);
        return this;
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
