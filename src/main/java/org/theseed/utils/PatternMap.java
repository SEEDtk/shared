/**
 *
 */
package org.theseed.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This object contains an ordered list of search patterns associated with objects.  When a string
 * is applied, the object associated with the first matching pattern will be returned.
 *
 * @author Bruce Parrello
 *
 */
public class PatternMap<T>  {

    /**
     * This class represents a single entry, matching a pattern and an object.
     *
     * @param <T>
     */
    public static class Entry<T> {

        /** pattern to match */
        private Pattern key;
        /** object to return */
        private T value;

        /**
         * Store a pattern/value pair.
         *
         * @param pattern	string containing the regex pattern
         * @param value		value to associate with the pattern
         */
        public Entry(String pattern, T value) {
            this.key = Pattern.compile(pattern);
            this.value = value;
        }

        /**
         * This will test a string against the pattern and return NULL if there is no match,
         * and the value if there is.
         *
         * @param string		string to test
         *
         * @return the value of this object, or NULL if the string did not match
         */
        public T test(String string) {
            T retVal = (this.key.matcher(string).matches() ? this.value : null);
            return retVal;
        }
    }

    // FIELDS
    /** list of pattern/value pairs, in presentation order */
    private List<Entry<T>> entries;

    /**
     * Construct an empty pattern map.
     *
     * @param capacity		initial capacity
     */
    public PatternMap(int capacity) {
        this.entries = new ArrayList<Entry<T>>(capacity);
    }

    /**
     * Construct an empty pattern map with the default capacity.
     */
    public PatternMap() {
        this.entries = new ArrayList<Entry<T>>();
    }

    /**
     * @return the number of map entries
     */
    public int size() {
        return this.entries.size();
    }

    /**
     * @return TRUE if the map is empty
     */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /**
     * @return the object appropriate to the specified key string, or NULL if there is none
     *
     * @param key	key string to check
     */
    public T get(String key) {
        Iterator<Entry<T>> iter = this.entries.iterator();
        T retVal = null;
        while (retVal == null && iter.hasNext())
            retVal = iter.next().test(key);
        return retVal;
    }

    /**
     * Store a new pattern/value pair in the map.
     *
     * @param key		regex string for the pattern
     * @param value		value to associate with it
     */
    public void add(String key, T value) {
        Entry<T> entry = new Entry<T>(key, value);
        this.entries.add(entry);
    }

}
