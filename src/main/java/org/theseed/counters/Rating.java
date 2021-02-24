/**
 *
 */
package org.theseed.counters;

/**
 * This class represents rated objects.  It is sorted by highest rating followed by the objects themselves.
 * A null-valued object sorts before everything with the same rating.
 *
 * @param <E>
 */
public class Rating<E extends Comparable<E>> implements Comparable<Rating<E>> {

    /** object being rated */
    private E key;
    /** rating of the object */
    private double value;

    @Override
    public int compareTo(Rating<E> o) {
        int retVal = Double.compare(o.value, this.value);
        if (retVal == 0 && this.key != o.key) {
            // Here we need to compare the rated objects.
            if (this.key == null)
                retVal = -1;
            else if (o.key == null)
                retVal = 1;
            else
                retVal = this.key.compareTo(o.key);
        }
        return retVal;
    }

    /**
     * Construct a rating.
     *
     * @param key		rated object
     * @param value		rating of object
     */
    public Rating(E key, double value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @return the key
     */
    public E getKey() {
        return this.key;
    }

    /**
     * @return the rating
     */
    public double getRating() {
        return this.value;
    }

}
