/**
 *
 */
package org.theseed.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

/**
 * This is a minor extension to ArrayList that provides a method for shuffling the bulk of the array into the
 * first N positions.  It is used to make random selections from the array, then iterate them off.  In
 * addition, it allows the array to be initialized in random order.  If the array is too small to fit
 * an object, it will be expanded to the proper size.
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

    @Override
    public T get(int idx) {
        T retVal = null;
        if (this.size() > idx)
            retVal = super.get(idx);
        return retVal;
    }

    @Override
    public T set(int idx, T obj) {
        if (this.size() > idx)
            super.set(idx, obj);
        else {
            while (this.size() < idx)
                this.add(null);
            this.add(obj);
        }
        return obj;
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

    /**
     * This is a static utility method for randomly selecting a small portion of a big
     * collection.
     *
     * @param collection	collection from which to select the results
     * @param limit			maximum number of results to select
     *
     * @return a random subset of the proper size taken from the original collection
     */
    public static <X> Collection<X> selectPart(Collection<X> collection, int limit) {
        Collection<X> retVal = collection;
        if (retVal.size() > limit) {
            Random randStream = new Random();
            // Here we have to select a random subset of the instances to return. We do not have
            // efficient random access to the instance list, so we run through them in order,
            // using a probability function to select based on the number of instances we still
            // need over the number of instances left. If this becomes greater than 1, we pick
            // everything remaining.
            double remaining = retVal.size();
            Iterator<X> iter = retVal.iterator();
            // We'll put the instances we keep in here. Note that we don't need the old value of
            // "retVal" since we only access it via the iterator.
            retVal = new ArrayList<X>(limit);
            while (retVal.size() < limit && iter.hasNext()) {
                // Get the current instance.
                X curr = iter.next();
                // Determine how much we want to keep it.
                double desire = (limit - retVal.size()) / remaining;
                remaining -= 1.0;
                // Roll a die to see if we keep it.
                if (randStream.nextDouble() < desire)
                    retVal.add(curr);
            }
        }
        return retVal;
    }

    /**
     * Extract the entry at the desired position of a random-access collection.
     *
     * @param collection	collection
     * @param idx			position (0-based) of the desired entry
     *
     * @return the desired entry in the specified collection, or NULL if there is none
     */
    public static <X> X selectItem(Collection<X> collection, int desired) {
        X retVal;
        if (desired < 0 && desired >= collection.size())
            retVal = null;
        else {
            Iterator<X> iter = collection.iterator();
            retVal = iter.next();
            for (int i = 0; i < desired; i++)
                retVal = iter.next();
        }
        return retVal;
    }

}
