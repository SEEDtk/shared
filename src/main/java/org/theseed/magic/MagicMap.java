/**
 *
 */
package org.theseed.magic;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import murmur3.MurmurHash3.LongPair;

/**
 * This object creates magic IDs, a readable short form of a long string and
 * uses them to store named objects.
 *
 * @author Bruce Parrello
 *
 */
public class MagicMap<T extends MagicObject> {

    // TODO:  add list of aliases, put aliases in checksum hash


    // FIELDS
    /** map from prefixes to the next usable suffix number */
    HashMap<String, Integer> suffixMapper;
    /** map from ids to objects */
    HashMap<String, T> idMapper;
    /** map from checksums to objects */
    HashMap<LongPair, T> checkMapper;
    /** dummy object for lookups */
    T searchObj;

    /** set of little words */
    private static final HashSet<String> LITTLE_WORDS =
            Stream.of("and", "or", "the", "a", "of", "in", "an", "to", "on", "").collect(Collectors.toCollection(HashSet::new));
    /** parentheticals */
    private static final Pattern PARENTHETICAL = Pattern.compile("\\(.*?\\)");
    /** things that are not digits and letters */
    private static final String PUNCTUATION = "\\W+";
    /** separate prefix from suffix */
    private static final Pattern ID_PARSER = Pattern.compile("(\\S+?)(\\d+)");

    /**
     * Create a new, blank magic ID table.
     *
     * @param searchObject	dummy object that can be used for searching
     */
    public MagicMap(T searchObject) {
        this.suffixMapper = new HashMap<String, Integer>();
        this.idMapper = new HashMap<String, T>();
        this.checkMapper = new HashMap<LongPair, T>();
        this.searchObj = searchObject;
    }

    /**
     * Condense a string into a magic ID.  Note there will be no
     * suffix.
     *
     * @param full	full string to be condensed
     *
     * @return a shorter representation of the string
     */
    public static String condense(String full) {
        // Remove all parentheticals.
        String noParens = RegExUtils.replaceAll(full.toLowerCase(),
                PARENTHETICAL, " ");
        // Separate into words.
        String[] words = noParens.split(PUNCTUATION);
        // Loop through the words, putting them into the output.
        StringBuilder retVal = new StringBuilder(16);
        for (int wordIdx = 0; retVal.length() < 16 && wordIdx < words.length; wordIdx++) {
            String thisWord = words[wordIdx];
            // Note we skip little words.
            if (! LITTLE_WORDS.contains(thisWord)) {
                // Capitalize the first letter and shrink to four characters.
                retVal.append(Character.toUpperCase(thisWord.charAt(0)));
                for (int i = 1; i < 4 && i < thisWord.length(); i++) {
                    retVal.append(Character.toLowerCase(thisWord.charAt(i)));
                }
            }
        }
       return retVal.toString();
    }

    /**
     * Associate a pre-generated ID with an object.
     *
     * @param obj	object to be mapped to the ID
     */
    public void register(T obj) {
        // Get the ID and name.
        String id = obj.getId();
        // Parse out the prefix and suffix.
        Matcher m = ID_PARSER.matcher(id);
        String prefix, suffixString;
        if (m.matches()) {
            prefix = m.group(1);
            suffixString = m.group(2);
        } else {
            prefix = id;
            suffixString = "";
        }
        int suffix = (suffixString.isEmpty() ? 0 : Integer.valueOf(suffixString));
        // Insure this suffix is not reused.
        if (! this.suffixMapper.containsKey(prefix) ||
                this.suffixMapper.get(prefix) < suffix) {
            suffixMapper.put(prefix, suffix + 1);
        }
        // Associate the value with the ID.
        this.idMapper.put(id, obj);
        // Update the checksum map.
        this.checkMapper.put(obj.getChecksum(), obj);
    }

    /**
     * @return the number of objects in this map
     */
    public int size() {
        return this.idMapper.size();
    }

    /**
     * @return TRUE if this map has nothing in it
     */
    public boolean isEmpty() {
        return this.idMapper.isEmpty();
    }

    /**
     * @return TRUE if this map has an object with the given ID
     *
     * @param key	ID to check
     */
    public boolean containsKey(String key) {
        return this.idMapper.containsKey(key);
    }

    /**
     * @return TRUE if this map has the given object in it
     *
     * @param obj	object for which to search
     */
    public boolean containsValue(T value) {
        return this.idMapper.containsKey(value.getId());
    }

    /**
     * @return TRUE if this map has an object with the given name in it.
     *
     * @param name	name of the object for which to search
     */
    public boolean containsName(String name) {
        return (this.getByName(name) != null);
    }

    /**
     * @return the object with the given ID, or NULL if it does not exist
     *
     * @param key	ID of interest
     */
    public T get(String key) {
        return this.idMapper.get(key);
    }

    /**
     * Store the specified object in this map.  If the object has no ID,
     * one will be created.
     *
     * @param 	value	object to store
     * @return	the original object
     */
    public T put(T value) {
        if (value.getId() == null) {
            this.storeNew(value);
        }
        this.register(value);
        return value;
    }

    /**
     * Generate an ID for the specified object and store it in the object,
     * then add the object to the map.
     *
     * @param value	object for which a magic ID is desired
     */
    private void storeNew(T value) {
        String prefix = condense(value.getName());
        int minSuffix = 0;
        // Does the prefix end with a digit?
        String end = StringUtils.right(prefix, 1);
        if (StringUtils.isNumeric(end)) {
            // Insure it doesn't.
            prefix += "n";
            // The suffixes start with 1 in this case.
            minSuffix = 1;
        }
        // Get the new suffix and append it.
        int suffix = this.suffixMapper.getOrDefault(prefix, minSuffix);
        String id = (suffix > 0 ? prefix + suffix : prefix);
        // Update the suffix map. Note that we don't use 1 as a suffix except for
        // the "n"-prefix case.
        int newSuffix = (suffix < 2 ? 2 : suffix + 1);
        this.suffixMapper.put(prefix, newSuffix);
        // Update the target object.
        value.setId(id);
        // Update the master map.
        this.idMapper.put(id, value);
        // Update the checksum map.
        this.checkMapper.put(value.getChecksum(), value);
    }

    /**
     * Remove the object with the specified key.
     *
     * @param key	key of object to remove
     *
     * @return the removed object
     */
    public T remove(String key) {
        return idMapper.remove(key);
    }

    /**
     * Copy the objects into this magic ID mapping.
     *
     * @param m
     */
    public void putAll(Collection<T> m) {
        for (T value : m) {
            this.put(value);
        }

    }

    /**
     * Erase everything in this mapping.  This will cause IDs to be reused.
     */
    public void clear() {
        this.suffixMapper.clear();
        this.idMapper.clear();
    }

    /**
     * @return a list of role IDs
     */
    public Set<String> keySet() {
        return this.idMapper.keySet();
    }

    /**
     * @return all the objects in this map
     */
    public Collection<T> values() {
        return this.idMapper.values();
    }

    /**
     * @return the name of the object with the specified key, or NULL if it does
     * 		   not exist
     *
     * @param key	key to check
     */
    public String getName(String key) {
        T obj = this.get(key);
        return (obj == null ? null : obj.getName());
    }

    /**
     * @return the object with the specified name, or NULL if there is none
     *
     * @param name	name of the desired object
     */
    public T getByName(String name) {
        // Store the name in our handy search object.
        searchObj.setName(name);
        // Get the checksum.
        LongPair checksum = searchObj.getChecksum();
        // Try to find it in the checksum map.
        T retVal = this.checkMapper.get(checksum);
        return retVal;
    }

}
