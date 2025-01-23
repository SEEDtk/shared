/**
 *
 */
package org.theseed.subsystems.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.subsystems.VariantId;

/**
 * This object is used to build rules from a CoreSubsystem. We do this when no existing rules are found.
 *
 * The basic procedure is to create a bitmap for each instance of a variant, indicating the roles present.
 * Unique bitmaps that are not a superset of others are kept. The intersection of all the bitmaps for a
 * variant is defined as a group, and then this group is ANDed with the conjunction of the remaining bits
 * in each map. The rules are ordered from the most bits to the fewest. A default rule is added for variant
 * -1 that matches if any role is present. To facilitate this rule, a definition is added for "any" that
 * matches if any one role is present. Auxiliary roles are not included. Bits for those will never be
 * set.
 *
 * @author Bruce Parrello
 *
 */
public class RuleGenerator {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(RuleGenerator.class);
    /** map of variant codes to rulebit lists */
    private Map<String, Set<RuleBits>> variantMap;
    /** index for the next group definition */
    private int nextNum;
    /** map of group definitions */
    private Map<String, RuleBits> groupMap;
    /** original subsystem */
    private CoreSubsystem parent;
    /** empty role set for this subsystem */
    private RuleBits emptySet;

    /**
     * Construct a rule generator from an existing core subsystem. We build a map of variant codes
     * to collections of rulebits. Each rulebit represents a set of roles that indicates the variant.
     *
     * @param sub	core subsystem to process
     */
    public RuleGenerator(CoreSubsystem sub) {
        this.parent = sub;
        this.emptySet = new RuleBits(sub);
        // Initialize the group definitions.
        this.groupMap = new TreeMap<String, RuleBits>();
        // Create the variant code map. We use a tree map because we expect it to be small.
        this.variantMap = new TreeMap<String, Set<RuleBits>>();
        // Loop through the subsystem rows.
        Iterator<CoreSubsystem.Row> iter = sub.rowIterator();
        while (iter.hasNext()) {
            CoreSubsystem.Row row = iter.next();
            // Skip over inactive rows.
            if (! row.isInactive()) {
                // Get the rulebit set.
                RuleBits ruleSet = new RuleBits(row);
                // We skip empty sets. In general, empty sets should be inactive anyway.
                if (! ruleSet.isEmpty()) {
                    // Find the rulebit list for this variant and merge us in.
                    String vCode = row.getVariantCode();
                    Collection<RuleBits> ruleBitList = this.variantMap.computeIfAbsent(vCode, x -> new TreeSet<RuleBits>());
                    ruleSet.mergeInto(ruleBitList);
                }
            }
        }
    }

    /**
     * @return the set of variant codes for this rule generator
     */
    public Set<String> getVariantCodes() {
        return this.variantMap.keySet();
    }

    /**
     * @return the rulebit collection for a variant (or NULL if the variant does not exist)
     *
     * @param vCode		relevant variant code
     */
    public Collection<RuleBits> getRuleBits(String vCode) {
        return this.variantMap.get(vCode);
    }

    /**
     * Add a new group definition and return its name.
     *
     * @param groupRoles	RuleBits map for the roles in the group
     *
     * @return the name given to the group
     */
    public String addGroup(RuleBits groupRoles) {
        // Check for a matching group already named.
        Optional<String> found = this.groupMap.keySet()
                .stream().filter(x -> this.groupMap.get(x).equals(groupRoles)).findAny();
        String retVal;
        if (found.isPresent())
            retVal = found.get();
        else {
            // No group already exists, so we add a new one.
            retVal = "group" + this.nextNum;
            this.nextNum++;
            this.groupMap.put(retVal, groupRoles);
        }
        return retVal;
    }

    /**
     * Convert the group definitions to a list of strings suitable to output as the definitions file.
     * This must be called AFTER getRuleDefinitions, since the latter creates the group definitions
     * themselves.
     */
    public List<String> getGroupDefinitions() {
        // This will contain our definition strings.
        List<String> retVal = new ArrayList<String>(this.groupMap.size() + 1);
        // First we do the "any" definition.
        final int width = this.parent.getRoleCount();
        String anyDef = "any means 1 of {" + IntStream.range(0, width).mapToObj(i -> this.parent.getRoleAbbr(i))
                .collect(Collectors.joining(", ")) + "}";
        // Emit the group definitions. We'll add ANY at the end.
        for (var groupEntry : this.groupMap.entrySet()) {
            String name = groupEntry.getKey();
            RuleBits roleSet = groupEntry.getValue();
            // We use the empty set so that the full set of roles is emitted.
            String roleString = roleSet.ruleString(this.emptySet);
            retVal.add(name + " means " + roleString);
        }
        // Finally, add the ANY definition.
        retVal.add(anyDef);
        return retVal;
    }

    /**
     * Convert the variant rules to a list of strings suitable to output as the definitions file.
     */
    public List<String> getVariantRules() {
        // We do active variants first, then the others.
        List<String> retVal = new ArrayList<String>(this.variantMap.size() + 1);
        // The boolean here determines which value of "isActive" should be on the variants output.
        this.processVariantRules(retVal, true);
        this.processVariantRules(retVal, false);
        // Append the invalid-variant rule.
        retVal.add("-1 means any");
        // Return the result.
        return retVal;
    }

    /**
     * This method runs through the variants and emits rules for the variants with the isActive
     * value indicated. Group definitions will be added as needed.
     *
     * @param output		string list for output
     * @param activeFlag	TRUE for only active variants, FALSE for non-active variants
     */
    private void processVariantRules(List<String> output, boolean activeFlag) {
        for (var variantEntry : variantMap.entrySet()) {
            String vCode = variantEntry.getKey();
            if (VariantId.isActive(vCode) == activeFlag) {
                // Here we have a variant we want to output in this pass.
                Collection<RuleBits> ruleList = variantEntry.getValue();
                String ruleString;
                // We have three cases: (1) only one rule, (2) multiple rules with no common
                // subset, and (3) multiple rules with a common subset.
                if (ruleList.size() == 1) {
                    // The simplest case is only one rule. We output just the rule, and use an
                    // empty set for the common group.
                    Iterator<RuleBits> iter = ruleList.iterator();
                    RuleBits rule = iter.next();
                    ruleString = vCode + " means " + rule.ruleString(this.emptySet);
                } else {
                    // Look for a common subset.
                    RuleBits common = RuleBits.intersection(this.parent, ruleList);
                    if (common.isEmpty()) {
                        // Here there is no common subset. We simply OR the rules together.
                        ruleString = vCode + " means " +
                                ruleList.stream().map(x -> this.parenthesize(x, common))
                                .collect(Collectors.joining(" or "));
                    } else {
                        // With a common subset, we must OR the rules (with the common removed)
                        // and AND them to the common group.
                        String groupName = this.addGroup(common);
                        ruleString = vCode + " means " + groupName + " and (" +
                                ruleList.stream().map(x -> this.parenthesize(x, common))
                                .collect(Collectors.joining(" or ")) + ")";
                    }
                    // Add this rule to the output.
                    output.add(ruleString);
                }
            }
        }
    }

    /**
     * This returns a rule string unmodified if it is primitive, and with parentheses if it is a conjunction.
     *
     * @param rule		role set for the rule
     * @param common	common set to omit
     */
    private String parenthesize(RuleBits rule, RuleBits common) {
        // Compute how many roles will be output in the rule. We are guaranteed that "common" is a proper
        // subset of "rule".
        int size = rule.size() - common.size();
        String retVal = rule.ruleString(common);
        if (size > 1)
            retVal = "(" + retVal + ")";
        return retVal;
    }
}
