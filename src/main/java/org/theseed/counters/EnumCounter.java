/**
 *
 */
package org.theseed.counters;

/**
 * This is a simple counter class where the key is an enum.
 *
 * @author Bruce Parrello
 *
 */
public class EnumCounter<K extends Enum<K>> {

    // FIELDS
    /** number of values in the set */
    private int size;
    /** array of counts */
    private int[] counts;

    /**
     * Construct an enum-keyed counter set.
     *
     * @param keyType	type of enum used to index the counters
     */
    public EnumCounter(Class<K> keyType) {
        this.size = keyType.getEnumConstants().length;
        this.counts = new int[this.size];
        this.clear();
    }

    /**
     * Clear all the counter values.
     */
    public void clear() {
        for (int i = 0; i < this.size; i++)
            this.counts[i] = 0;
    }

    /**
     * Increment a count.
     *
     * @param key	enum value to count
     */
    public int count(K key) {
        return ++this.counts[key.ordinal()];
    }

    /**
     * Return a count.
     *
     * @param key	enum value whose count is desired
     */
    public int getCount(K key) {
        return this.counts[key.ordinal()];
    }

    /**
     * Accumulate a second set of counters in these counters.
     *
     * @param counters	second set of counters to add to these
     */
    public void sum(EnumCounter<K> counters) {
        for (int i = 0; i < this.counts.length; i++)
            this.counts[i] += counters.counts[i];
    }


}
