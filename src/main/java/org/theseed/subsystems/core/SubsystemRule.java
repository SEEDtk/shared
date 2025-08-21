/**
 *
 */
package org.theseed.subsystems.core;

import java.io.Serializable;
import java.util.Set;

import org.theseed.basic.ParseFailureException;

/**
 * This object represents a rule to be applied to a set of roles.  Essentially, a rule can be (1) a role ID, (2) an inverted
 * rule, or (3) a list of rules and a number.  A role rule is called a primitive rule and is satisfied if the role exists in
 * the set.  An inverted rule is satisfied if the specified rule is NOT satisfied.  A list rule is satisfied if at least
 * the indicated number of rules in the list is satisfied.
 *
 * For each subsystem, we read in a list of rule definitions.  The primitive rules are defined in the spreadsheet in the form
 * of rule abbreviations.  Then the checkvariant_definitions file contains a bunch of unordered rule definitions.  Finally,
 * the checkvariant_rules file contains a rule definition for each variant, in order.
 *
 *
 *
 * @author Bruce Parrello
 *
 */
public abstract class SubsystemRule implements Serializable {

    // FIELDS
    /** serialization object ID */
    private static final long serialVersionUID = -1773990479324882478L;

    /**
     * During compilation, this method adds a sub-rule to this rule.
     *
     * @param subRule	sub-rule to add
     * @param compiler	controlling rule compiler
     */
    protected abstract void addParm(SubsystemRule subRule, RuleCompiler compiler) throws ParseFailureException;

    /**
     * Verify that this rule matches the specified role set.
     *
     * @param roleSet	role set to check
     *
     * @return TRUE if it matches, else FALSE
     */
    public abstract boolean check(Set<String> roleSet);

    /**
     * @return the hash code for this rule
     */
    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    /**
     * @return TRUE if this rule is equal to the specified other rule
     *
     * @param other		other object to compare
     */
    @Override
    public abstract boolean equals(Object other);

    /**
     * @return TRUE if this is a compound rule, else FALSE
     */
    protected abstract boolean isCompound();

    /**
     * This is used to normalize the second operand of an equality operator.  If the
     * operand is not a subsystem rule, we return NULL.  If it is a basic rule, we
     * unspool it.
     *
     * @param operand	operand to normalize
     *
     * @return a non-basic subsystem rule, or NULL if the operand is invalid
     */
    public SubsystemRule normalize(Object operand) {
        SubsystemRule retVal;
        switch (operand) {
            case SubsystemBasicRule subsystemBasicRule -> retVal = subsystemBasicRule.unspool();
            case SubsystemRule subsystemRule -> retVal = subsystemRule;
            default -> retVal = null;
        }
        return retVal;
    }

}
