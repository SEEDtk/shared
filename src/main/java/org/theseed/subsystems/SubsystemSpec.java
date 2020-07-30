/**
 *
 */
package org.theseed.subsystems;

import java.util.ArrayList;
import java.util.List;

import org.theseed.proteins.Role;

/**
 * This object specifies the characteristics of a subsystem.  It contains the subsystem name
 * and an ordered list of the subsystem roles.
 *
 * Subsystem specifications are sorted by subsystem name.  There can be only one specification per subsystem.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemSpec implements Comparable<SubsystemSpec> {

    // FIELDS
    /** subsystem name */
    private String name;
    /** roles in this subsystem */
    private List<String> roles;
    /** classifications of this subsystem */
    private List<String> classes;

    /**
     * Create a blank subsystem specification.
     *
     * @param name		name of the subsystem
     */
    public SubsystemSpec(String name) {
        this.name = name;
        this.roles = new ArrayList<String>(20);
        this.classes = new ArrayList<String>(3);
    }

    /**
     * Add a role to this subsystem.
     *
     * @param role		name of role being added
     */
    public void addRole(String role) {
        this.roles.add(role);
    }

    /**
     * @return the number of roles in this subsystem
     */
    public int getRoleCount() {
        return this.roles.size();
    }

    /**
     * @return the name of the role at the specified position
     *
     * @param i		index of desired role
     */
    public String getRole(int i) {
        String retVal = null;
        if (i >= 0 && i < this.roles.size())
            retVal = this.roles.get(i);
        return retVal;
    }

    /**
     * Specify the classifications.
     *
     * @param classes	array of classifications
     */
    public void setClassifications(String... classes) {
        this.classes.clear();
        for (String class0 : classes)
            this.classes.add(class0);
    }

    /**
     * @return the classifications
     */
    public List<String> getClassifications() {
        return this.classes;
    }

    /**
     * @return the name of this subsystem
     */
    public String getName() {
        return this.name;
    }

    @Override
    public int compareTo(SubsystemSpec o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SubsystemSpec)) {
            return false;
        }
        SubsystemSpec other = (SubsystemSpec) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * @return the list of roles in this subsystem
     */
    public List<String> getRoles() {
        return this.roles;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * @return TRUE if this subsystem contains the specified role
     *
     * @param role	role of interest
     */
    public boolean contains(Role role) {
        return this.roles.stream().anyMatch(r -> role.matches(r));
    }
}
