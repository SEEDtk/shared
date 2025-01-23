/**
 *
 */
package org.theseed.subsystems.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.theseed.basic.ParseFailureException;

/**
 * A subsystem list rule contains a number and a list of sub-rules.  The rule is satisfied if the specified number
 * of sub-rules is satisfied.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemListRule extends SubsystemRule {

    // FIELDS
    /** mode of this subsystem list rule during compilation */
    private Mode mode;
    /** number of rules that must be satisfied for this rule to be satisfied */
    private int num;
    /** list of rules to satisfy */
    private List<SubsystemRule> rules;
    /** serialization object ID */
    private static final long serialVersionUID = 2239241481602944956L;

    protected static enum Mode {
        /** this list rule is simulating an AND */
        AND {
            @Override
            protected void updateCount(SubsystemListRule rule) {
                rule.num = rule.rules.size();
            }

            @Override
            public String print(SubsystemListRule rule) {
                return "(" + rule.printBoolean(" and ") + ")";
            }
        },
        /** this list rule is simulating an OR */
        OR {
            @Override
            protected void updateCount(SubsystemListRule rule) {
            }

            @Override
            public String print(SubsystemListRule rule) {
                return "(" + rule.printBoolean(" or ") + ")";
            }
        },
        /** this is a standard list rule */
        NUM {
            @Override
            protected void updateCount(SubsystemListRule rule) {
            }

            @Override
            public String print(SubsystemListRule rule) {
                return Integer.toString(rule.num) + " of {" + rule.printBoolean(", ") + "}";
            }
        };

        /**
         * Update the count for the specified rule, if necessary.
         *
         * @param rule	rule possessing this mode
         */
        protected abstract void updateCount(SubsystemListRule rule);

        /**
         * Convert the specified rule to a string.
         */
        public abstract String print(SubsystemListRule rule);

    }

    /**
     * Construct a subsystem list rule for the AND or OR mode.
     *
     * @param operator	operator mode
     * @param top		subsystem rule to use as first operand
     */
    public SubsystemListRule(Mode operator, SubsystemRule top) {
        this.mode = operator;
        this.num = 1;
        this.setup();
        this.rules.add(top);
    }

    /**
     * @return a printed version of this rule using the specified delimiter between sub-rules
     *
     * @param delim		delimiter (e.g. " and ", " or ", or ", ")
     */
    protected String printBoolean(String delim) {
        return this.rules.stream().map(x -> x.toString()).collect(Collectors.joining(delim));
    }

    /**
     * Initialize a subsystem list rule for a specified number.
     *
     * @param num	number of sub-rules that must be satisfied
     */
    public SubsystemListRule(int num) {
        this.mode = Mode.NUM;
        this.num = num;
        this.setup();
    }

    /**
     * Initialize the rule list.
     */
    private void setup() {
        this.rules = new ArrayList<SubsystemRule>();
    }

    @Override
    protected void addParm(SubsystemRule subRule, RuleCompiler compiler) throws ParseFailureException {
        // Add the new sub-rule.
        this.rules.add(subRule);
        // Update the count.
        this.mode.updateCount(this);
    }

    @Override
    public boolean check(Set<String> roleSet) {
        // Loop through the list of sub-rules, setting the return to TRUE if the desired number are true.
        int found = 0;
        final int n = this.rules.size();
        for (int i = 0; i < n && found < this.num; i++) {
            if (this.rules.get(i).check(roleSet))
                found++;
        }
        return (found >= this.num);
    }

    /**
     * @return the mode of this rule
     */
    public Mode type() {
        return this.mode;
    }

    @Override
    public int hashCode() {
        int retVal = this.num;
        for (SubsystemRule subRule : this.rules)
            retVal = 19 * retVal + subRule.hashCode();
        return retVal;

    }

    @Override
    public boolean equals(Object other) {
        boolean retVal = false;
        SubsystemRule right = this.normalize(other);
        if (right != null && right instanceof SubsystemListRule) {
            SubsystemListRule o = (SubsystemListRule) right;
            final int n = this.rules.size();
            retVal = (this.num == o.num && n == o.rules.size());
            for (int i = 0; i < n && retVal; i++)
                retVal = this.rules.get(i).equals(o.rules.get(i));
        }
        return retVal;
    }

    /**
     * Remove and return the last parameter for this rule.
     *
     * @return the last parameter in this rule's sub-list
     *
     * @throws ParseFailureException
     */
    public SubsystemRule popLast() throws ParseFailureException {
        final int n = this.rules.size() - 1;
        if (n < 0)
            throw new ParseFailureException("Unexpected operator in list context.");
        SubsystemRule retVal = this.rules.remove(n);
        return retVal;
    }

    @Override
    public String toString() {
        // The way we print this depends on the mode.
        String retVal;
        if (this.rules.isEmpty())
            retVal = "FAIL";
        else
            retVal = this.mode.print(this);
        return retVal;
    }

    @Override
    protected boolean isCompound() {
        return true;
    }

}
