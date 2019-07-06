/**
 *
 */
package org.theseed.utils;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * This is a simple map from a random object to a number. It is used for simple counters.
 * Currently, the implementation is fairly inefficient.  When I have time, I will do something
 * smarter.  There are two counts computed-- good and bad.  The user can also ask for the mean
 * good count and the mean bad count.  Both values can be retrieved, but only one can be incremented.
 *
 * @author Bruce Parrello
 *
 */
public class CountMap<K> {

    /** underlying hash map */
    HashMap<K, Counts> map;

    private class Counts {
        public int good;
        public int bad;

        public Counts() {
            this.good = 0;
            this.bad = 0;
        }
    }

    /**
     * Create a blank counting map.
     */
    public CountMap() {
        this.map = new HashMap<K, Counts>();
    }

    /**
     * @return the good count for a given key
     *
     * @param key	key for the counter of interest
     */
    public int good(K key) {
        int retVal = 0;
        if (this.map.containsKey(key)) {
            retVal = this.map.get(key).good;
        }
        return retVal;
    }

    /**
     * @return the bad count for a given key
     *
     * @param key	key for the counter of interest
     */
    public int bad(K key) {
        int retVal = 0;
        if (this.map.containsKey(key)) {
            retVal = this.map.get(key).bad;
        }
        return retVal;
    }

    /**
     * @return the mean good count for a given key
     *
     * @param key	key for the counter of interest
     */
    public double meanGood(K key) {
        double retVal = 0;
        int good = this.good(key);
        if (good > 0) {
            retVal = good / (double) (good + this.bad(key));
        }
        return retVal;
    }

    /**
     * @return the counter object for a specified key.  If none exists, one will be created.
     *
     * @param key	key for the counter of interest
     */
    private Counts getCounts(K key) {
        Counts retVal;
        if (this.map.containsKey(key)) {
            retVal = this.map.get(key);
        } else {
            retVal = new Counts();
            this.map.put(key, retVal);
        }
        return retVal;
    }

    /** Increment the good count for a key. */
    public void setGood(K key) {
        Counts myCount = this.getCounts(key);
        myCount.good++;
    }

    /** Increment the bad count for a key. */
    public void setBad(K key) {
        Counts myCount = this.getCounts(key);
        myCount.bad++;
    }

    /**
     * @return	a sorted collection of all the keys in this object
     */
    public SortedSet<K> keys() {
        TreeSet<K> retVal = new TreeSet<K>(this.map.keySet());
        return retVal;
    }


}
