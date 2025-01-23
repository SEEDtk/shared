/**
 *
 */
package org.theseed.subsystems.core;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This object contains the bitmap for a particular variant specification. It provides methods for translating
 * the bits to conjunctions, finding common subsets, and other operations.
 *
 * @author Bruce Parrello
 *
 */
public class RuleBits implements Comparable<RuleBits> {

    // FIELDS
    /** basic bit set indicating the roles in use */
    private BitSet roleBits;
    /** number of bits set */
    private int size;
    /** relevant subsystem */
    private CoreSubsystem parent;

    /**
     * Construct the rule bits for a subsystem row.
     *
     * @param row	subsystem row to parse
     */
    public RuleBits(CoreSubsystem.Row row) {
        this.parent = row.getParent();
        List<Set<String>> cols = row.getColumns();
        // We'll count the number of active roles in here.
        this.size = 0;
        // We need to create the bit map.  Note we will skip the auxiliary roles.
        final int width = cols.size();
        this.roleBits = new BitSet(width);
        BitSet auxMap = this.parent.getAuxMap();
        // Loop through the role positions checking for presence.
        for (int i = 0; i < width; i++) {
            if (! auxMap.get(i) && ! cols.get(i).isEmpty()) {
                this.size++;
                this.roleBits.set(i);
            }
        }
    }

    /**
     * Construct an empty rulebit set.
     *
     * @param sub	parent subsystem
     */
    protected RuleBits(CoreSubsystem sub) {
        this.parent = sub;
        this.roleBits = new BitSet(sub.getRoleCount());
        this.size = 0;
    }

    @Override
    public int compareTo(RuleBits o) {
        // Compare the cardinality. Bigger sets compare first.
        int retVal = o.size - this.size;
        if (retVal == 0) {
            // Here we need to insure only equal bit sets result in 0. Other than that, the order doesn't matter, so long
            // as it is invariant. We start by comparing parent subsystems.
            retVal = this.parent.getName().compareTo(o.parent.getName());
            if (retVal == 0) {
                // Now we compare hash codes. This is for performance.
                retVal = this.hashCode() - o.hashCode();
                if (retVal == 0) {
                    // Finally, we have a bit-for-bit match.
                    int pos = 0;
                    final int width = this.parent.getRoleCount();
                    while (pos < width && retVal == 0) {
                        int pos1 = this.roleBits.nextSetBit(pos+1);
                        if (pos1 < 0) pos1 = width;
                        pos = o.roleBits.nextSetBit(pos+1);
                        if (pos < 0) pos = width;
                        retVal = pos1 - pos;
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * Check if this rulebit set subsumes or is subsumed by another set. A rule bit set subsumes another iff
     * every TRUE bit in this set is also TRUE in the other set.
     *
     * @param other		other set to compare to this one
     *
     * @returns -1 if we subsume (or are equal to) the other set, 1 if we are subsumed by it, else 0
     */
    public int subsumeCompare(RuleBits other) {
        int retVal;
        if (this.size <= other.size) {
            retVal = -1;
            int pos = this.roleBits.nextSetBit(0);
            while (pos >= 0 && retVal == -1) {
                if (! other.roleBits.get(pos))
                    retVal = 0;
                else
                    pos = this.roleBits.nextSetBit(pos + 1);
            }
        } else {
            retVal = -other.subsumeCompare(this);
        }
        return retVal;
    }

    /**
     * @return the number of roles in this set
     */
    public int size() {
        return this.size;
    }

    /**
     * Merge this rulebit set into a list of rulebits. If this set equals or is subsumed by a set already in the list,
     * the list is unchanged. If this set subsumes any sets already in the list, they are removed before it is added.
     *
     * NOTE that the type of collection must support the "remove" method in its iterator.
     *
     * @param ruleList	a collection of rules to check
     */
    public void mergeInto(Collection<RuleBits> ruleList) {
        Iterator<RuleBits> iter = ruleList.iterator();
        boolean discard = false;
        while (iter.hasNext() && ! discard) {
            RuleBits other = iter.next();
            int cmp = this.subsumeCompare(other);
            if (cmp < 0) {
                // Here we subsume the other rule set, so the other rule set can be removed.
                iter.remove();
            } else if (cmp > 0) {
                // Here we are subsumed, so we abort the merge.
                discard = true;
            }
        }
        if (! discard)
            ruleList.add(this);
    }

    /**
     * Compute the intersection of a collection of rulebit sets. This essentially gives us the roles in common
     * for all the rulebit sets.
     *
     * @param sub		parent subsystem
     * @param ruleList	collection of rulebit sets
     */
    public static RuleBits intersection(CoreSubsystem sub, Collection<RuleBits> ruleList) {
        RuleBits retVal = new RuleBits(sub);
        // Note we only proceed if we have at least one rulebit set in our list.
        if (! ruleList.isEmpty()) {
            Iterator<RuleBits> iter = ruleList.iterator();
            retVal.roleBits = (BitSet) iter.next().roleBits.clone();
            while (iter.hasNext())
                retVal.roleBits.and(iter.next().roleBits);
            // We must fix the size.
            retVal.size = retVal.roleBits.cardinality();
        }
        return retVal;
    }

    /**
     * Determine if this rulebit set matches one of the sets in a list.
     *
     * @param ruleList	collection of rulebit sets
     *
     * @return TRUE if this set matches at least one item in the list, else FALSE
     */
    public boolean matches(Collection<RuleBits> ruleList) {
        boolean retVal = ruleList.stream().anyMatch(x -> x.subsumeCompare(this) < 0);
        return retVal;
    }


    /**
     * Convert this role set into a string, omitting roles in a specified common set.
     *
     * @param common	rulebit set for the common roles to omit
     */
    public String ruleString(RuleBits common) {
        final int width = this.parent.getRoleCount();
        String retVal = IntStream.range(0, width)
                .filter(i -> ! common.roleBits.get(i) && this.roleBits.get(i))
                .mapToObj(i -> this.parent.getRoleAbbr(i)).collect(Collectors.joining(" and "));
        return retVal;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.parent == null) ? 0 : this.parent.hashCode());
        result = prime * result + ((this.roleBits == null) ? 0 : this.roleBits.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RuleBits)) {
            return false;
        }
        RuleBits other = (RuleBits) obj;
        if (this.parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (!this.parent.equals(other.parent)) {
            return false;
        }
        if (this.roleBits == null) {
            if (other.roleBits != null) {
                return false;
            }
        } else if (!this.roleBits.equals(other.roleBits)) {
            return false;
        }
        return true;
    }

    /**
     * @return TRUE if there are no bits set in this rule map
     */
    public boolean isEmpty() {
        return this.size == 0;
    }

}
