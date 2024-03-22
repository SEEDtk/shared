/**
 *
 */
package org.theseed.stats;

/**
 * This class represents an unordered pair of elements of the same type. The equals and hashcode
 * methods return the same result regardless of the internal ordering of the paired objects.
 *
 * The default string conversion separates the key values with a slash.  This may need to be
 * overridden if the keys are at all complicated.
 *
 * Neither key can be NULL.  Null values will cause NullPointerExceptions at runtime.
 *
 * @author Bruce Parrello
 *
 */
public class KeyPair<K> {

    // FIELDS
    K left;
    K right;

    /**
     * Create a blank key pair object from specified pair elements.
     */
    public KeyPair(K l, K r) {
        this.left = l;
        this.right = r;
    }

    /**
     * Create an empty key pair for utility purposes.  This is restricted to internal use.
     */
    /* package */ KeyPair() {
        this.left = null;
        this.right = null;
    }

    /**
     * Store new values for the key pointers.
     */
    /* package */ void set(K l, K r) {
        this.left = l;
        this.right = r;
    }

    /**
     * @return the hash code for this object
     *
     * Two objects with the same pair of keys will have the same hash code regardless of
     * the ordering of the keys.  [1,2] is the same as [2,1].
     */
    @Override
    public int hashCode() {
        return this.left.hashCode() + this.right.hashCode();
    }

    /**
     * @return TRUE if the other object has the same elements as this one, else FALSE
     *
     * NOTE that the order of the two elements cannot matter when checking for equality.
     * [1,2] is the same as [2,1].
     *
     * @param object to compare to this one
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof KeyPair))
            return false;
        // Because of the parameterized type, the casting has to happen at runtime.
        @SuppressWarnings("unchecked")
        KeyPair<K> other = (KeyPair<K>) obj;
        // Note here that we assume neither key is null.
        if (this.left.equals(other.left))
            return this.right.equals(other.right);
        else if (this.left.equals(other.right))
            return this.right.equals(other.left);
        else
            return false;
    }

    /**
     * @return the string representation of this pair
     *
     * Note that we sort the strings so that identical pairs have identical strings.
     */
    @Override
    public String toString() {
        String leftString = this.left.toString();
        String rightString = this.right.toString();
        String retVal;
        if (leftString.compareTo(rightString) < 0) {
            retVal = leftString + "/" + rightString;
        } else {
            retVal = rightString + "/" + leftString;
        }
        return retVal;
    }

    /**
     * @return the left key
     */
    public K getLeft() {
        return this.left;
    }

    /**
     * @return the right key
     */
    public K getRight() {
        return this.right;
    }

}
