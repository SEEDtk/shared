/**
 *
 */
package org.theseed.counters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
public class QualityCountMap<K extends Comparable<K>> {

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
        this.setGood(key, 1);
    }

    /** Increment the bad count for a key. */
    public void setBad(K key) {
        this.setBad(key, 1);
    }

    /**
     * Increment the good count for a key.
     *
     * @param num	amount to increment the count
     */
    public void setGood(K key, int num) {
        Counts myCount = this.getCounts(key);
        myCount.good += num;
    }

    /**
     * Increment the bad count for a key.
     *
     * @param num	amount to increment the count
     */
    public void setBad(K key, int num) {
        Counts myCount = this.getCounts(key);
        myCount.bad += num;
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
     * This nested class is a comparator for sorting by best good count
     */
    private class Sorter implements Comparator<K> {

        @Override
        public int compare(K o1, K o2) {
            Counts count1 = map.get(o1);
            Counts count2 = map.get(o2);
            int g1 = (count1 == null ? 0 : count1.good);
            int g2 = (count2 == null ? 0 : count2.good);
            int retVal = g2 - g1;
            if (retVal == 0) {
                // Good counts same, prefer lower bad count.
                int b1 = (count1 == null ? 0 : count1.bad);
                int b2 = (count2 == null ? 0 : count2.bad);
                retVal = b1 - b2;
                if (retVal == 0) {
                    // All counts same, sort by key.
                    retVal = o1.compareTo(o2);
                }
            }
            return retVal;
        }

    }

    /**
     * @return the keys in this map, sorted by highest good count
     */
    public List<K> bestKeys() {
        ArrayList<K> retVal = new ArrayList<K>(this.map.keySet());
        retVal.sort(this.new Sorter());
        return retVal;
    }

    /**
     * @return all the keys in this map, unordered
     */
    public Collection<K> allKeys() {
        return this.map.keySet();
    }

    /**
     * Erase all the counts.
     */
    public void clear() {
        this.map.clear();
    }


}
