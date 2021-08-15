/**
 *
 */
package org.theseed.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * This is a static class containing some primitive set utilities.
 *
 * @author Bruce Parrello
 *
 */
public class SetUtils {

    /**
     * This method creates a typed set from an array of elements.  The set
     * will be a tree if it is very small and a hash set otherwise.
     *
     * @param <T>		type of elements in the set
     * @param elements	array of elements to put in the set
     *
     * @return a set containing the specified elements
     */
    public static <T> Set<T> newFromArray(T[] elements) {
        Set<T> retVal = (elements.length < 10 ? new TreeSet<T>() : new HashSet<T>(elements.length));
        for (T item : elements)
            retVal.add(item);
        return retVal;
    }

    /**
     * This method will return TRUE if the set contains the element or if the set was passed as NULL,
     * and FALSE otherwise.  The effect is to use the null pointer to represent everything.
     *
     * @param <T>		type of elements in the set
     * @param set		set to check
     * @param element	element to check for containment
     *
     * @return TRUE if the element is in the set or if the set does not exist
     */
    public static <T> boolean isMember(Set<T> set, T element) {
        return (set == null || set.contains(element));
    }

}
