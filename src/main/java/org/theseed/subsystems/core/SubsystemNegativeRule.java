/**
 *
 */
package org.theseed.subsystems.core;

import java.util.Set;

import org.theseed.basic.ParseFailureException;

/**
 * This rule is satisfied only if the single parameter is unsatisfied.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemNegativeRule extends SubsystemRule {

    // FIELDS
    /** parameter rule */
    private SubsystemRule parm;
    /** serialization object ID */
    private static final long serialVersionUID = -1638369691178018009L;

    /**
     * Construct an unary rule.
     */
    public SubsystemNegativeRule() {
        this.parm = null;
    }

    @Override
    protected void addParm(SubsystemRule subRule, RuleCompiler compiler) throws ParseFailureException {
        this.parm = subRule;
        // This is an unary operator, so it is now complete and can be added to its parent.
        compiler.unroll();
    }


     @Override
    public boolean check(Set<String> roleSet) {
        return ! this.parm.check(roleSet);
    }

    @Override
    public int hashCode() {
        return 59 * this.parm.hashCode() + 1;
    }

    @Override
    public boolean equals(Object other) {
        boolean retVal;
        SubsystemRule operand = this.normalize(other);
        if (operand != null && operand instanceof SubsystemNegativeRule)
            retVal = this.parm.equals(((SubsystemNegativeRule) operand).parm);
        else
            retVal = false;
        return retVal;
    }

    @Override
    public String toString() {
        return "not (" + this.parm.toString() + ")";
    }

    @Override
    protected boolean isCompound() {
        return false;
    }

}
