/**
 *
 */
package org.theseed.genome;

import com.github.cliftonlabs.json_simple.JsonArray;

/**
 * A coupling represents an association between two features or families.  The coupling is associated with two
 * score values, an integer size and a floating-point strength.  The coupling is anchored in a source object
 * and contains the ID of the target object to which it is coupled.
 *
 * @author Bruce Parrello
 *
 */
public class Coupling implements Comparable<Coupling> {

    // FIELDS
    /** ID of the target feature or family */
    private String target;
    /** coupling size */
    private int size;
    /** coupling strength */
    private double strength;

    /**
     * Construct a new coupling descriptor.
     *
     * @param target	ID of coupled object
     * @param size		size of the coupling (number found during search)
     * @param strength	strength of he coupling (computed score based on distance)
     */
    public Coupling(String target, int size, double strength) {
        this.target = target;
        this.size = size;
        this.strength = strength;
    }

    /**
     * Construct a coupling descriptor from a JSON array.
     *
     * @param source	JSON array containing the coupling description
     */
    public Coupling(JsonArray source) {
        this.target = source.getString(0);
        this.size = source.getInteger(1);
        this.strength = source.getDouble(2);
    }

    /**
     * @return the target object ID
     */
    public String getTarget() {
        return target;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the strength
     */
    public double getStrength() {
        return strength;
    }

    @Override
    /**
     * Sort by highest coupling strength and then target ID.  Note, however, two identical target IDs must still compare as 0.
     */
    public int compareTo(Coupling o) {
        int retVal = 0;
        int tDiff = this.target.compareTo(o.target);
        if (tDiff != 0) {
            retVal = Double.compare(o.strength, this.strength);
            if (retVal == 0) retVal = tDiff;
        }
        return retVal;
    }

    @Override
    public int hashCode() {
        return this.target.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Coupling)) {
            return false;
        }
        Coupling other = (Coupling) obj;
        if (target == null) {
            if (other.target != null) {
                return false;
            }
        } else if (!target.equals(other.target)) {
            return false;
        }
        return true;
    }

}
