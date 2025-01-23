/**
 *
 */
package org.theseed.subsystems.core;

import java.util.Set;

import org.theseed.basic.ParseFailureException;

/**
 * A basic rule contains a single sub-rule, and is used as a placeholder during compilation.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemBasicRule extends SubsystemRule {

    // FIELDS
    /** parameter sub-rule */
    private SubsystemRule parm;
    /** serialization object ID */
    private static final long serialVersionUID = -833023044697700080L;

    /**
     * Construct an empty basic rule.
     */
    public SubsystemBasicRule() {
        this.parm = null;
    }

    @Override
    protected void addParm(SubsystemRule subRule, RuleCompiler compiler) throws ParseFailureException {
        // Insure we are not trying to put too many parameters in this rule.
        if (this.parm != null)
            throw new ParseFailureException("Operands found without an operator.");
        this.parm = subRule;
    }

    @Override
    public boolean check(Set<String> roleSet) {
        return this.parm.check(roleSet);
    }

    @Override
    public int hashCode() {
        return 31 * this.parm.hashCode() + 1;
    }

    /**
     * Unspool this basic rule to find the first real rule underneath it.
     *
     * @return the first non-basic rule that is a descendant of this one
     */
    protected SubsystemRule unspool() {
        SubsystemRule retVal = this.parm;
        while (retVal instanceof SubsystemBasicRule)
            retVal = ((SubsystemBasicRule) retVal).parm;
        return retVal;
    }

    @Override
    public boolean equals(Object other) {
        // Equality is tricky, since the basic rule has no effect and just passes the check through
        // to the parameter.  We unspool both operands until we get something other that a basic rule.
        boolean retVal = false;
        if (other instanceof SubsystemRule) {
            SubsystemRule left = this.unspool();
            SubsystemRule right = (SubsystemRule) other;
            if (right instanceof SubsystemBasicRule)
                right = ((SubsystemBasicRule) right).unspool();
            retVal = left.equals(right);
        }
        return retVal;
    }

    @Override
    public String toString() {
        String retVal = this.parm.toString();
        if (this.parm.isCompound())
            retVal = "(" + retVal + ")";
        return retVal;
    }

    @Override
    protected boolean isCompound() {
        return false;
    }

}
