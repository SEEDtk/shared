/**
 *
 */
package org.theseed.counters;

import java.util.Collection;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * This is a simple map from a random object to a number. It is used for simple counters.
 *
 * This class will fail if the string representations of two unequal objects are the same.
 *
 * @author Bruce Parrello
 *
 */
public class CountMap<K> {

    /** underlying hash map */
    HashMap<K, Count> map;

    /**
     * Utility class to encapsulate the count.
     */
    public class Count implements Comparable<Count> {
        private K key;
        private int num;

        /**
         * Create a new count.
         *
         * @param key	key being counted
         */
        private Count(K key) {
            this.key = key;
            this.num = 0;
        }

        /**
         * @return the key counted by this counter.
         */
        public K getKey() {
            return this.key;
        }

        /**
         * @return the value counted for this key
         */
        public int getCount() {
            return this.num;
        }

        /**
         * The sort order is highest count to lowest count.  If counts are equal but the keys
         * are not, we need to compare different.  In that case, we sort by the string
         * representation.
         */
        @Override
        public int compareTo(CountMap<K>.Count o) {
            // Note we compare the counts in reverse.
            int retVal = o.num - this.num;
            if (retVal == 0) {
                retVal = this.key.toString().compareTo(o.key.toString());
            }
            return retVal;
        }

        /**
         * The string representation includes the key and the count.
         */
        @Override
        public String toString() {
            String retVal;
            switch (this.num) {
            case 0:
                retVal = this.key.toString() + " (not found)";
                break;
            case 1:
                retVal = this.key.toString() + " (1 occurrence)";
                break;
            default:
                retVal = String.format("%s (%d occurrences,)", this.key.toString(),
                        this.num);
            }
            return retVal;
        }

    }


    /**
     * Create a blank counting map.
     */
    public CountMap() {
        this.map = new HashMap<K, Count>();
    }

    /**
     * @return the count for a given key
     *
     * @param key	key for the counter of interest
     */
    public int getCount(K key) {
        int retVal = 0;
        Count found = this.map.get(key);
        if (found != null) {
            retVal = found.num;
        }
        return retVal;
    }

    /**
     * @return the counter object for a specified key.  If none exists, one will be created.
     *
     * @param key	key for the counter of interest
     */
    private Count getCounter(K key) {
        Count retVal;
        retVal = this.map.get(key);
        if (retVal == null) {
            retVal = new Count(key);
            this.map.put(key, retVal);
        }
        return retVal;
    }

    /**
     * @return the counter object for the specified key, or NULL if the key has not been counted
     *
     * @param key	key of interest
     */
    public Count findCounter(K key) {
        return this.map.get(key);
    }

    /** Increment the count for a key and return the new result. */
    public int count(K key) {
        Count myCount = this.getCounter(key);
        int retVal = ++myCount.num;
        return retVal;
    }
    /**
     * @return	a sorted collection of all the keys in this object
     */
    public SortedSet<K> keys() {
        TreeSet<K> retVal = new TreeSet<K>(this.map.keySet());
        return retVal;
    }

    /**
     * @return the number of keys in this map
     */
    public int size() {
        return this.map.size();
    }

    /**
     * @return a collection of all the counts in this object, sorted from highest to lowest
     */
    public Collection<CountMap<K>.Count> sortedCounts() {
        TreeSet<CountMap<K>.Count> retVal = new TreeSet<CountMap<K>.Count>(this.map.values());
        return retVal;
    }

}
