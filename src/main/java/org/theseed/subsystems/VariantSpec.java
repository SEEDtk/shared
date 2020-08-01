/**
 *
 */
package org.theseed.subsystems;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.theseed.genome.Genome;
import org.theseed.genome.SubsystemRow;
import org.theseed.proteins.RoleSet;

/**
 * A variant specification is an ordered list of protein families and/or role IDs that form a possible subsystem implementation.
 * Variant specifications are ordered by the number of role slots filled in (descending), followed by the total number of
 * role slots (ascending), followed by an item-by-item comparison of the role slots.  This allows us to use a sorted set
 * for the subsystem projector's variant lists.
 *
 * @author Bruce Parrello
 *
 */
public class VariantSpec implements Comparable<VariantSpec> {

    // FIELDS
    /** subsystem represented by this configuration */
    private SubsystemSpec subsystem;
    /** variant code of this configuration */
    private String variantCode;
    /** array of role IDs */
    private RoleSet[] cells;
    /** number of filled slots in the array */
    private int cellCount;

    /**'
     * Create a new, blank variant specification.
     *
     * @param subsystem		specification of the subsystem
     * @param code			variant code
     */
    public VariantSpec(SubsystemSpec subsystem, String code) {
        this.subsystem = subsystem;
        this.variantCode = code;
        this.cells = new RoleSet[subsystem.getRoleCount()];
        for (int i = 0; i < this.cells.length; i++)
            this.cells[i] = RoleSet.NO_ROLES;
        this.cellCount = 0;
    }

    @Override
    public int compareTo(VariantSpec o) {
        int retVal = o.cellCount - this.cellCount;
        if (retVal == 0) {
            retVal = this.cells.length - o.cells.length;
            if (retVal == 0) {
                for (int i = 0; retVal == 0 && i < this.cells.length; i++)
                    retVal = this.cells[i].compareTo(o.cells[i]);
            }
        }
        return retVal;
    }

    /**
     * Specify the role ID for a particular role position (cell).  The role ID is computed
     * by pulling the appropriate role ID from the subsystem projector.  The role description
     * is known from the parent subsystem specification.
     *
     * @param i				index of the cell
     * @param projector		target subsystem projector
     */
    public void setCell(int i, SubsystemProjector projector) {
        boolean oldEmpty = this.cells[i].isEmpty();
        String roleDesc = this.subsystem.getRole(i);
        this.cells[i] = projector.getRoleIds(roleDesc);
        if (oldEmpty) this.cellCount++;
    }

    /**
     * @return the name of the relevant subsystem
     */
    public String getName() {
        return this.subsystem.getName();
    }

    /**
     * @return the variant code
     */
    public String getCode() {
        return this.variantCode;
    }

    /**
     * @return the occupied cell count
     */
    public int getCellCount() {
        return this.cellCount;
    }

    /**
     * @return TRUE if all of the roles in this specification are keys in the hash map
     *
     * @param roleMap		hash of role IDs to feature sets
     */
    public boolean matches(Map<String, Set<String>> roleMap) {
        boolean retVal = true;
        for (int i = 0; retVal && i < this.cells.length; i++) {
            // We fail (return FALSE) iff the cell has roles, and there is no feature
            // containing all the roles
            retVal = (this.cells[i].isEmpty() || this.cells[i].featureSet(roleMap) != null);
        }
        return retVal;
    }

    /**
     * Install a subsystem row for this variant in the specified genome.
     *
     * @param genome		genome to contain the subsystem row
     * @param roleMap		hash of role IDs to feature sets
     *
     * @return the subsystem row
     */
    public SubsystemRow instantiate(Genome genome, Map<String, Set<String>> roleMap) {
        SubsystemRow retVal = new SubsystemRow(genome, this.getName());
        retVal.setVariantCode(this.variantCode);
        retVal.setClassifications(this.subsystem.getClassifications());
        // For each cell in the subsystem, we add the role.
        for (int i = 0; i < this.cells.length; i++) {
            String role = this.subsystem.getRole(i);
            retVal.addRole(role);
            // Now add any features in this cell.  They will be the ones with the specified role.
            if (! this.cells[i].isEmpty()) {
                Set<String> fids = this.cells[i].featureSet(roleMap);
                if (fids != null) {
                    for (String fid : fids)
                        retVal.addFeature(role, fid);
                }
            }
        }
        return retVal;
    }

    /**
     * @return the lexically lowest role ID for this variant specification, or NULL if the variant specification
     * 		   is empty
     */
    public String getKeyRole() {
        return RoleSet.min(this.cells);
    }

    /**
     * @return the role ID array for this specification
     */
    public RoleSet[] getCells() {
        return this.cells;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(cells);
        result = prime * result + ((subsystem == null) ? 0 : subsystem.hashCode());
        result = prime * result + ((variantCode == null) ? 0 : variantCode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VariantSpec)) {
            return false;
        }
        VariantSpec other = (VariantSpec) obj;
        if (!Arrays.equals(cells, other.cells)) {
            return false;
        }
        if (subsystem == null) {
            if (other.subsystem != null) {
                return false;
            }
        } else if (!subsystem.equals(other.subsystem)) {
            return false;
        }
        if (variantCode == null) {
            if (other.variantCode != null) {
                return false;
            }
        } else if (!variantCode.equals(other.variantCode)) {
            return false;
        }
        return true;
    }

    /**
     * @return TRUE if the other variant specification is for the same subsystem
     */
    public boolean isRedundant(VariantSpec other) {
        return other.getName().contentEquals(this.getName());
    }

    @Override
    public String toString() {
        return "VariantSpec[" + (subsystem != null ? subsystem : "(null)")
                + ":" + (variantCode != null ? variantCode : "(null)") + ", " + cellCount + " cells]";
    }

}
