/**
 *
 */
package org.theseed.counters;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * This is a simple map from a random object to a pair of numbers. It is used for simple counters.
 * There are two counts computed-- good and bad.
 *
 * @author Bruce Parrello
 *
 */
public class QualityCountMap<K> {

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
    public QualityCountMap() {
        this.map = new HashMap<K, Counts>();
    }

    /**
     * @return the good count for a given key
     *
     * @param key	key for the counter of interest
     */
    public int good(K key) {
        int retVal = 0;
        Counts found = this.map.get(key);
        if (found != null) {
            retVal = found.good;
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
        Counts found = this.map.get(key);
        if (found != null) {
            retVal = found.bad;
        }
        return retVal;
    }

    /**
     * @return the fraction of good instances for a given key
     *
     * @param key	key for the counter of interest
     */
    public double fractionGood(K key) {
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
        retVal = this.map.get(key);
        if (retVal == null) {
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

    /**
     * @return the number of keys in this map
     */
    public int size() {
        return this.map.size();
    }




}
