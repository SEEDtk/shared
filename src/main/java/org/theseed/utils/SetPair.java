/**
 *
 */
package org.theseed.utils;

import java.util.Set;

/**
 * This is a simple utility class for an immutable pair of sets.  It is not comparable or hashable; rather, it is
 * a simple way to package two sets for returning from a method.
 *
 * @author Bruce Parrello
 *
 */
public class SetPair<V> {

    // FIELDS
    /** first set */
    private Set<V> set1;
    /** second set */
    private Set<V> set2;

    /**
     * Construct a set pair from two sets.
     */
    public SetPair(Set<V> first, Set<V> second) {
        this.set1 = first;
        this.set2 = second;
    }

    /**
     * @return the first set
     */
    public Set<V> getSet1() {
        return this.set1;
    }

    /**
     * @return the second set
     */
    public Set<V> getSet2() {
        return this.set2;
    }

}
