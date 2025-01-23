/**
 *
 */
package org.theseed.subsystems.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.theseed.genome.Genome;
import org.theseed.genome.SubsystemRow;
import org.theseed.subsystems.StrictRole;
import org.theseed.subsystems.StrictRoleMap;
import org.theseed.subsystems.VariantId;

/**
 * This is an abbreviated descriptor for a core subsystem. It contains the roles, the classifications,
 * the name, and the rules. This is precisely the information that a subsystem projector needs.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemDescriptor implements Serializable {

    // FIELDS
    /** subsystem name */
    private String name;
    /** classifications */
    private String[] classes;
    /** array of role names */
    private List<String> roleNames;
    /** variant rules */
    private LinkedHashMap<String, SubsystemRule> variantRules;
    /** serialization object ID */
    private static final long serialVersionUID = 8444755932664902942L;

    /**
     * Create a blank subsystem descriptor.
     */
    public SubsystemDescriptor() {
        this.name = "";
        this.classes = new String[] { "", "", "" };
        this.roleNames = new ArrayList<String>();
        this.variantRules = new LinkedHashMap<String, SubsystemRule>();
    }

    /**
     * Create a subsystem descriptor from a core subsystem object.
     *
     * @param sub	source core subsystem
     */
    public SubsystemDescriptor(CoreSubsystem sub) {
        // Get the name and the classification array.
        this.name = sub.getName();
        this.classes = new String[] { sub.getSuperClass(), sub.getMiddleClass(), sub.getSubClass() };
        // Get the role names (in order).
        final int roleCount = sub.getRoleCount();
        this.roleNames = new ArrayList<String>(roleCount);
        for (int i = 0; i < roleCount; i++)
            this.roleNames.add(sub.getRole(i));
        // Store the variant rules.
        this.variantRules = sub.getVariantRuleMap();
    }

    /**
     * Attempt to project this subsystem into a genome using the genome's role presence map.
     * This method is thread-safe. You can have multiple projections into the same genome
     * going on in parallel so long as the genome isn't having features added and removed.
     *
     * @param genome		target genome for projection
     * @param roleSet		role presence map
     * @param roleMap		role ID definition map
     * @param activeFlag	if TRUE, only active variants will be projected
     *
     * @return the variant code of the projected subsystem row, or NULL if the subsystem is not present
     */
    public String project(Genome genome, Map<String, Set<String>> roleSet, StrictRoleMap roleMap, boolean activeFlag) {
        // Loop through the variant rules.
        var iter = this.variantRules.entrySet().iterator();
        String retVal = null;
        while (iter.hasNext() && retVal == null) {
            var ruleEntry = iter.next();
            String vCode = ruleEntry.getKey();
            // Check the rule to see if the variant is present.
            if (ruleEntry.getValue().check(roleSet.keySet())) {
                // Yes it is. Denote we found a variant.
                retVal = vCode;
            }
        }
        // Make sure this is a worthwhile variant.
        if (retVal != null && activeFlag && ! VariantId.isActive(retVal))
            retVal = null;
        else if (retVal != null) {
            // We want to project this subsystem onto the genome. We build a subsystem row using the name,
            // classes, and variant code and connect it to the genome.
            SubsystemRow row;
            synchronized (genome) {
                row = new SubsystemRow(genome, this.name);
            }
            row.setClassifications(this.classes[0], this.classes[1], this.classes[2]);
            row.setVariantCode(retVal);
            // Next we fill in each role. Loop through the roles in this subsystem.
            for (String roleName : this.roleNames) {
                // Check for features performing this role.
                StrictRole role = roleMap.getByName(roleName);
                if (role != null) {
                    String roleId = role.getId();
                    Set<String> fidSet = roleSet.get(roleId);
                    if (fidSet != null) {
                        for (String fid : fidSet)
                            row.addFeature(roleName, fid);
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * Attempt to project this subsystem into a genome using the genome's role presence map. This
     * version projects both active and inactive variants.
     *
     * @param genome		target genome for projection
     * @param roleSet		role presence map
     * @param roleMap		role ID definition map
     *
     * @return the variant code of the projected subsystem row, or NULL if the subsystem is not present
     */
    public String project(Genome genome, Map<String, Set<String>> roleSet, StrictRoleMap roleMap) {
        return this.project(genome, roleSet, roleMap, false);
    }

    /**
     * Attempt to project this subsystem into a genome using the genome's role presence map. This
     * version projects only active variants.
     *
     * @param genome		target genome for projection
     * @param roleSet		role presence map
     * @param roleMap		role ID definition map
     *
     * @return the variant code of the projected subsystem row, or NULL if the subsystem is not present
     */
    public String projectActive(Genome genome, Map<String, Set<String>> roleSet, StrictRoleMap roleMap) {
        return this.project(genome, roleSet, roleMap, true);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.classes);
        result = prime * result + Objects.hash(name, roleNames, variantRules);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SubsystemDescriptor other = (SubsystemDescriptor) obj;
        return Arrays.equals(this.classes, other.classes) && Objects.equals(this.name, other.name)
                && Objects.equals(this.roleNames, other.roleNames)
                && Objects.equals(this.variantRules, other.variantRules);
    }

    /**
     * @return the name of this subsystem
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the classifications of this subsystem
     */
    public String[] getClasses() {
        return this.classes;
    }

    /**
     * @return the role names of this subsystem, in order
     */
    public List<String> getRoleNames() {
        return this.roleNames;
    }

    /**
     * @return TRUE if the specified role is in this subsystem
     *
     * @param roleName	role name to check
     */
    public boolean contains(String roleName) {
        return this.roleNames.contains(roleName);
    }

    /**
     * @return the number of roles in this subsystem
     */
    public int getRoleCount() {
        return this.roleNames.size();
    }

    /**
     * @return the role name at the specified position
     *
     * @param idx	index of the desired role
     */
    public String getRole(int idx) {
        return this.roleNames.get(idx);
    }

}
