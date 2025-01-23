/**
 *
 */
package org.theseed.subsystems.core;

import java.util.Set;

import org.theseed.basic.ParseFailureException;

/**
 * This is a primitive rule.  It is satisfied if the identified role is in the role set.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemPrimitiveRule extends SubsystemRule {

    // FIELDS
    /** role of interest */
    private String roleId;
    /** parent core subsystem, if tracking is desired */
    private transient CoreSubsystem parent;
    /** role abbreviation, if tracking is desired */
    private transient String abbr;
    /** serialization object ID */
    private static final long serialVersionUID = -5528547368975299906L;

    /**
     * Construct a primitive rule for the specified role.
     *
     * @param role	ID of the role of interest
     */
    public SubsystemPrimitiveRule(String role) {
        this.roleId = role;
        this.parent = null;
        this.abbr = "";
    }

    /**
     * Denote we want to track this rule during evaluation.
     *
     * @param subsystem		parent subsystem
     * @param name			abbreviation
     */
    protected void setTracking(CoreSubsystem subsystem, String name) {
        this.parent = subsystem;
        this.abbr = name;
    }

    @Override
    protected void addParm(SubsystemRule subRule, RuleCompiler compiler) throws ParseFailureException {
        throw new ParseFailureException("Cannot add parameters to a primitive rule.");
    }

    @Override
    public boolean check(Set<String> roleSet) {
        boolean retVal = roleSet.contains(this.roleId);
        if (this.parent != null)
            this.parent.record(this.abbr, retVal);
        return retVal;
    }

    @Override
    public int hashCode() {
        return 17 * this.roleId.hashCode() + 1;
    }

    @Override
    public boolean equals(Object other) {
        boolean retVal;
        SubsystemRule operand = this.normalize(other);
        if (operand != null && operand instanceof SubsystemPrimitiveRule)
            retVal = this.roleId.equals(((SubsystemPrimitiveRule) operand).roleId);
        else
            retVal = false;
        return retVal;
    }

    @Override
    public String toString() {
        // If we don't have an abbreviation, we use the role ID.  In that latter case, the rule string will not
        // compile.
        String retVal = this.abbr;
        if (retVal == null)
            retVal = this.roleId;
        return retVal;
    }

    @Override
    protected boolean isCompound() {
        return false;
    }

}
