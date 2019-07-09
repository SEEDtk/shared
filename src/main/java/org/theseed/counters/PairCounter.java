/**
 *
 */
package org.theseed.counters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class counts pairs and occurrences of objects.  The main method records the existence of an
 * object and lists the others found with it.  At the end, it is possible to extract how often two
 * objects occur together regardless of ordering.
 *
 * @author Bruce Parrello
 *
 */
public class PairCounter<K> {

    // FIELDS
    /** counts of the pairs */
    CountMap<KeyPair<K>> pairCountMap;
    /** counts of the individual keys */
    CountMap<K> keyCountMap;
    /** storage space for lookups */
    KeyPair<K> testKey;

    /**
     * Utility class for returning counts.
     */
    public class Count implements Comparable<Count> {
        private K key1;
        private K key2;
        private int count;

        /**
         * We construct this object from a counter stored in the pair map. We lexically sort the
         * two key fields based on their string representation to simplify the compareTo method.
         *
         * @param counter	keypair-counter object to convert to an instance of this object
         */
        private Count(CountMap<KeyPair<K>>.Count counter) {
            K left = counter.getKey().getLeft();
            K right = counter.getKey().getRight();
            if (left.toString().compareTo(right.toString()) < 0) {
                key1 = left; key2 = right;
            } else {
                key1 = right; key2 = left;
            }
            this.count = counter.getCount();
        }

        /**
         * We sort by count descending, then key1, then key2.
         */
        @Override
        public int compareTo(PairCounter<K>.Count o) {
            int retVal = o.count - this.count;
            if (retVal == 0) {
                retVal = this.key1.toString().compareTo(o.key1.toString());
                if (retVal == 0) {
                    retVal = this.key2.toString().compareTo(o.key2.toString());
                }
            }
            return retVal;
        }

        /**
         * @return the tendency of these two items to occur together
         *
         * This value is computed as the number of mutual occurrences over the number
         * of total occurrences.  So, if k1 occurs 26 times, k2 occurs 24 times, and
         * they occur together 20 times, the togetherness is 20 / (24 + 26 - 20) = 0.666+.
         */
        public double togetherness() {
            int count1 = keyCountMap.getCount(key1);
            int count2 = keyCountMap.getCount(key2);
            int denom = (count1 + count2 - this.count);
            double retVal;
            if (denom > 0) {
                retVal = (double) this.count / (count1 + count2 - this.count);
            } else if (denom < count) {
                retVal = 1.0;
            } else {
                retVal = 0;
            }
            return retVal;
        }

        /**
         * @return the first key
         */
        public K getKey1() {
            return this.key1;
        }

        /**
         * @return the second key
         */
        public K getKey2() {
            return this.key2;
        }

        /**
         * @return the count of occurrences together
         */
        public int getCount() {
            return this.count;
        }

    }

    /**
     * Construct a new, blank pair counter.
     */
    public PairCounter() {
        this.pairCountMap = new CountMap<KeyPair<K>>();
        this.keyCountMap = new CountMap<K>();
        this.testKey = new KeyPair<K>();
    }

    /**
     * Return the number of pairs mapped.
     */
    public int size() {
        return pairCountMap.size();
    }

    /**
     * Return the number of items mapped.
     */
    public int itemSize() {
        return this.keyCountMap.size();
    }

    /**
     * Record that each other item occurs once with the first item.  Only the first item is
     * recorded as an individual item appearance.
     *
     * @param item			item that occurs
     * @param otherItems	collection of other items that occur with it
     */
    public void recordOccurrence(K item, Collection<K> otherItems) {
        // First, count the item.
        this.recordOccurrence(item);
        // Now count the pairings.
        for (K other : otherItems) {
            this.recordPairing(item, other);
        }
    }

    /**
     * Record that an item has occurred with no neighbors.
     *
     * @param item	item that has occurred
     */
    public void recordOccurrence(K item) {
        this.recordOccurrences(item, 1);
    }

    /**
     * Record that an item has occurred a specified number of times.
     *
     * @param item	item that has occurred
     * @param num	number of times it occurred
     */
    public void recordOccurrences(K item, int num) {
        this.keyCountMap.count(item, num);
    }

    /**
     * Record that two items have occurred together.  This does not record an individual appearance
     * of either item.
     *
     * @param key1 	first item
     * @param key2	second item
     */
    public void recordPairing(K key1, K key2) {
        this.recordPairings(key1, key2, 1);
    }

    /**
     * Record that two items have occurred together a specified number of times.  This does
     * not record an individual appearance of either item.
     *
     * @param key1 	first item
     * @param key2	second item
     * @param num	number of occurrences
     */
    public void recordPairings(K key1, K key2, int num) {
        this.testKey.set(key1,  key2);
        int count = this.pairCountMap.count(this.testKey, num);
        if (count == num) {
            // Here the key was inserted, so we can't re-use testKey any more.  Allocate a new
            // copy.
            this.testKey = new KeyPair<K>();
        }
    }

    /**
     * @return the number of times the two items have occurred together
     *
     * @param key1	first item
     * @param key2	second item
     */
    public int getCount(K key1, K key2) {
        this.testKey.set(key1, key2);
        return this.pairCountMap.getCount(this.testKey);
    }


    /**
     * @return the number of times an item has occurred
     */
    public int getCount(K item) {
        return this.keyCountMap.getCount(item);
    }

    /**
     * @return a list of pairings, sorted from most frequent to least frequent.
     */
    public List<Count> sortedCounts() {
        // Create space to store our results.
        ArrayList<Count> retVal = new ArrayList<Count>(this.size());
        // Run through a sorted list of pair counts.  Add them to the output array in order.
        for (CountMap<KeyPair<K>>.Count counter : this.pairCountMap.sortedCounts()) {
            retVal.add(new Count(counter));
        }
        // Return the array.
        return retVal;
    }

    /**
     * @return a list of the items that participated in counts
     */
    public List<CountMap<K>.Count> sortedItemCounts() {
        return this.keyCountMap.sortedCounts();
    }

    /**
     * @return the counter object for the specified pair of keys, or NULL if the keys do not
     * 		   occur together
     *
     * @param key1	first key of interest
     * @param key2	second key of interest
     */
    public Count getPairCount(K key1, K key2) {
        Count retVal = null;
        this.testKey.left = key1;
        this.testKey.right = key2;
        CountMap<KeyPair<K>>.Count buffer = this.pairCountMap.findCounter(this.testKey);
        if (buffer != null) {
            retVal = new Count(buffer);
        }
        return retVal;
    }

}
