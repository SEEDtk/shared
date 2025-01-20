/**
 *
 */
package org.theseed.proteins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.theseed.genome.Feature;

/**
 * This is a simple object that represents a short array of role IDs, generally computed from a
 * functional assignment.  The base class is abstract, and all empty role sets point to the
 * same object.
 *
 * @author Bruce Parrello
 *
 */
public class RoleSet implements Iterable<String>, Comparable<RoleSet> {

    /**
     * static role set for the common case of no known roles
     */
    public static final RoleSet NO_ROLES = new Empty();

    /**
     * delimiter for string conversions
     */
    private static final String DELIM = ",";

    // FIELDS
    /** array of roles in this set */
    protected String[] roles;

    /**
     * Construct a role set from a role ID collection.
     *
     * @param roles		collection of role IDs
     */
    private RoleSet(Collection<String> roles) {
        this.roles = new String[roles.size()];
        this.roles = roles.toArray(this.roles);
    }

    /**
     * Construct a role set from a role ID array.
     *
     * @param roles		array of role IDs
     */
    private RoleSet(String[] roles) {
        this.roles = roles;
    }

    /**
     * Construct an empty role set.
     */
    protected RoleSet() {
        this.roles = new String[0];
    }

    /**
     * Construct a role set from its string representation.
     *
     * @param string	string representation of the role set
     */
    public static RoleSet fromString(String string) {
        RoleSet retVal;
        if (string == null || string.isEmpty())
            retVal = NO_ROLES;
        else
            retVal = new RoleSet(StringUtils.split(string, DELIM));
        return retVal;
    }

    /**
      * Construct a role set from a functional assignment.
     *
     * @param function	functional assignment to parse
     * @param roleMap	role map for conversion from descriptions to IDs
     *
     * @return the role set representing this function
     */
    public static RoleSet create(String function, RoleMap roleMap) {
        String[] roleDescs = Feature.rolesOfFunction(function);
        List<String> buffer = new ArrayList<String>(roleDescs.length);
        for (String roleDesc : roleDescs) {
            Role found = roleMap.getByName(roleDesc);
            if (found != null)
                buffer.add(found.getId());
        }
        // As a minor memory optimization, we return a constant if there are no known roles.
        RoleSet retVal = (buffer.isEmpty() ? NO_ROLES : new RoleSet(buffer));
        return retVal;
    }

    /**
     * @return TRUE if this role set is empty, else FALSE
     *
     * NOTE that this method is overridden for the empty role set
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * This class is the iterator for the role set.
     */
    public class Iter implements Iterator<String> {

        // FIELDS
        private int nextIdx;

        public Iter() {
            nextIdx = 0;
        }

        @Override
        public boolean hasNext() {
            if (nextIdx >= RoleSet.this.size())
                return false;
            else
                return true;
        }

        @Override
        public String next() {
            return RoleSet.this.roles[nextIdx++];
        }

    }
    @Override
    public Iterator<String> iterator() {
        return this.new Iter();
    }

    /**
     * @return the number of roles in the set
     *
     * NOTE that this method is overridden for the empty role set
     */
    public int size() {
        return this.roles.length;
    }


    @Override
    public int compareTo(RoleSet o) {
        // Bigger role sets compare lower
        int retVal = o.size() - this.size();
        if (retVal == 0) {
            // Both sets are the same size.  Compare pairwise.
            for (int i = 0; i < this.size() && retVal == 0; i++)
                retVal = this.roles[i].compareTo(o.roles[i]);
        }
        return retVal;
    }

    /**
     * @return the lexically lowest role ID in an array of role sets, or NULL if there are no roles
     *
     * @param cells		array of role sets to scan
     */
    public static String min(RoleSet[] cells) {
        String retVal = null;
        for (RoleSet roles : cells) {
            for (String role : roles) {
                if (retVal == null || role.compareTo(retVal) < 0)
                    retVal = role;
            }
        }
        return retVal;
    }

    /**
     * @return a stream of the roles in this set
     */
    public Stream<String> stream() {
        return Arrays.stream(this.roles);
    }

    /**
     * @return the set of all feature IDs in the specified role map that contain all of this set's roles,
     * 		   or NULL if there are no such features or this set is empty
     *
     * @param roleMap	map of roles to feature ID sets
     *
     * NOTE that this method is overridden for the empty role set.  Also, the use of NULL is a bit clumsy.
     * We are looking to optimize the single-role case, which is the vast majority of what we see.
     */
    public Set<String> featureSet(Map<String, Set<String>> roleMap) {
        Set<String> retVal = roleMap.get(this.roles[0]);
        for (int i = 1; i < this.size() && retVal != null; i++) {
            Set<String> other = roleMap.get(this.roles[i]);
            if (other == null)
                retVal = null;
            else {
                // We need the intersection of the two sets.  We start by
                // duplicating the current set and taking the intersection.
                retVal = new HashSet<String>(retVal);
                retVal.retainAll(other);
                if (retVal.size() == 0)
                    retVal = null;
            }
        }
        return retVal;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(roles);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RoleSet)) {
            return false;
        }
        RoleSet other = (RoleSet) obj;
        if (!Arrays.equals(roles, other.roles)) {
            return false;
        }
        return true;
    }

    /**
     * NOTE that this method is overridden for the empty role set
     */
    @Override
    public String toString() {
        return StringUtils.join(this.roles, DELIM);
    }

    /**
     * @return TRUE if all the roles in this set are contained in the specified other set
     *
     * @param other		other set to check
     */
    public boolean contains(RoleSet other) {
        boolean retVal = true;
        for (int i = 0; i < other.size() && retVal; i++) {
            retVal = false;
            for (int j = 0; ! retVal && j < this.size(); j++)
                retVal = this.roles[j].contentEquals(other.roles[i]);
        }
        return retVal;
    }

    /**
     * This class is used for empty role sets.  It has special overrides for optimization.
     * The base class assumes the set is nonempty, and the public constructors assure that
     * ALL empty role sets are instances of this class.
     */
    public static class Empty extends RoleSet {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public Set<String> featureSet(Map<String, Set<String>> roleMap) {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(RoleSet other) {
            return false;
        }

    }

}
