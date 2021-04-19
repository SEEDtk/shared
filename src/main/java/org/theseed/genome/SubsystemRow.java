/**
 *
 */
package org.theseed.genome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.theseed.subsystems.VariantId;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This object represents a subsystem as it is implemented in a genome.  It contains various bits of
 * information about the subsystem plus a list of role bindings.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemRow implements Comparable<SubsystemRow> {

    public static enum SubsystemKeys implements JsonKey {
        VARIANT_CODE("active"),
        ROLE_BINDINGS(noEntries),
        NAME(""),
        CLASSIFICATION(noEntries),
        ROLE_ID(""),
        FEATURES(noEntries)
        ;

        private final Object m_value;

        SubsystemKeys(Object value) {
            this.m_value = value;
        }

        /** This is the string used as a key in the incoming JsonObject map.
         */
        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        /** This is the default value used when the key is not found.
         */
        @Override
        public Object getValue() {
            return this.m_value;
        }

    }

    /**
     * This nested class represents a role in a subsystem.
     */
    public class Role {

        /** name of this role */
        private String name;

        /** set of features bound to this role */
        private Set<String> fids;

        /**
         * Construct a new role.
         *
         * @param name	name of the role
         */
        public Role(String name) {
            this.name = name;
            this.fids = new HashSet<String>(3);
        }

        /**
         * @return the name of the enclosing subsystem
         */
        public String getSubName() {
            return SubsystemRow.this.variant.getName();
        }

        /**
         * @return the name of this role
         */
        public String getName() {
            return name;
        }

        /**
         * @return the set of features attached to this role
         */
        public Set<Feature> getFeatures() {
            Set<Feature> retVal = new HashSet<Feature>(this.fids.size());
            for (String fid : this.fids) {
                Feature feat = SubsystemRow.this.parent.getFeature(fid);
                if (feat != null)
                    retVal.add(feat);
            }
            return retVal;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getRow().hashCode();
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Role)) {
                return false;
            }
            Role other = (Role) obj;
            if (!getRow().equals(other.getRow())) {
                return false;
            }
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
         * @return the parent subsystem row
         */
        public SubsystemRow getRow() {
            return SubsystemRow.this;
        }

    }

    // FIELDS
    /** empty list  */
    private static final JsonArray noEntries = new JsonArray();
    /** name and variant code for this subsystem */
    private VariantId variant;
    /** list of roles */
    private List<Role> roles;
    /** parent genome */
    private Genome parent;
    /** classifications */
    private List<String> classifications;

    /**
     * Create a subsystem row descriptor from an incoming JSON object.
     *
     * @param genome		parent genome for the subsystem
     * @param subsystemObj	JSON object describing the subsystem
     */
    public SubsystemRow(Genome genome, JsonObject subsystemObj) {
        // Get the subsystem metadata.
        this.variant = new VariantId(subsystemObj.getStringOrDefault(SubsystemKeys.NAME),
                subsystemObj.getStringOrDefault(SubsystemKeys.VARIANT_CODE));
        this.classifications = new ArrayList<String>(3);
        JsonArray classList = subsystemObj.getCollectionOrDefault(SubsystemKeys.CLASSIFICATION);
        for (int i = 0; i < classList.size(); i++)
            this.classifications.add(classList.getString(i));
        // Connect to the parent genome.
        this.parent = genome;
        genome.connectSubsystem(this);
        // Now we run through the role bindings.  For each one, we insert the role name in
        // the role list, the feature IDs in the role map, and the role descriptors themselves
        // into the subsystem pointers of the features.
        this.roles = new ArrayList<Role>(20);
        JsonArray bindings = subsystemObj.getCollectionOrDefault(SubsystemKeys.ROLE_BINDINGS);
        for (Object binding : bindings) {
            JsonObject bindingObj = (JsonObject) binding;
            String roleName = bindingObj.getStringOrDefault(SubsystemKeys.ROLE_ID);
            this.addRole(roleName);
            JsonArray fids = bindingObj.getCollectionOrDefault(SubsystemKeys.FEATURES);
            for (int i = 0; i < fids.size(); i++)
                this.addFeature(roleName, fids.getString(i));
        }
    }

    /**
     * Add a new role to this subsystem.
     *
     * @param roleName	name of the new role
     *
     * @return the role created
     */
    public Role addRole(String roleName) {
        Role retVal = new Role(roleName);
        this.roles.add(retVal);
        return retVal;
    }

    /**
     * Create a brand-new subsystem implementation.
     *
     * @param genome	genome implementing the subsystem
     * @param name		name of the new subsystem
     */
    public SubsystemRow(Genome genome, String name) {
        this.variant = new VariantId(name, "active");
        this.parent = genome;
        this.classifications = new ArrayList<String>(3);
        this.roles = new ArrayList<Role>();
        genome.connectSubsystem(this);
    }

    /**
     * Connect a feature to one of our roles.
     *
     * @param role		name of role bound to the feature
     * @param fid		ID of the bound feature
     */
    public void addFeature(String role, String fid) {
        Feature feat = this.parent.getFeature(fid);
        Optional<Role> thisRole = this.roles.stream().filter(k -> k.name.contentEquals(role)).findFirst();
        if (thisRole.isPresent() && feat != null) {
            thisRole.get().fids.add(fid);
            feat.connectSubsystem(thisRole.get());
        }
    }

    /**
     * @return this object as a JSON string
     */
    public JsonObject toJson() {
        JsonObject retVal = new JsonObject();
        retVal.put(SubsystemKeys.VARIANT_CODE.getKey(), this.variant.getCode());
        retVal.put(SubsystemKeys.NAME.getKey(), this.variant.getName());
        JsonArray classList = new JsonArray().addAllChain(this.classifications);
        retVal.put(SubsystemKeys.CLASSIFICATION.getKey(), classList);
        JsonArray bindings = new JsonArray();
        for (Role role : this.roles) {
            JsonObject roleObject = new JsonObject();
            roleObject.put(SubsystemKeys.ROLE_ID.getKey(), role.getName());
            JsonArray features = new JsonArray().addAllChain(role.fids);
            roleObject.put(SubsystemKeys.FEATURES.getKey(), features);
            bindings.add(roleObject);
        }
        retVal.put(SubsystemKeys.ROLE_BINDINGS.getKey(), bindings);
        return retVal;
    }

    /**
     * @return the variant code for this subsystem implementation
     */
    public String getVariantCode() {
        return this.variant.getCode();
    }

    /**
     * @return TRUE if this is an active variant, else FALSE
     */
    public boolean isActive() {
        return this.variant.isActive();
    }

    /**
     * @return the list of roles in this subsystem
     */
    public List<Role> getRoles() {
        return roles;
    }

    /**
     * @return the name of this subsystem
     */
    public String getName() {
        return this.variant.getName();
    }

    /**
     * @return the classifications for this subsystem
     */
    public List<String> getClassifications() {
        return classifications;
    }

    /**
     * Set the classifications for this subsystem.
     *
     * @param superClass	highest-level classification
     * @param midClass		mid-level classification
     * @param subClass		lowest-level classification
     */
    public void setClassifications(String superClass, String midClass, String subClass) {
        this.classifications.clear();
        this.classifications.add(superClass);
        this.classifications.add(midClass);
        this.classifications.add(subClass);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        String name = this.getName();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.getId().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SubsystemRow)) {
            return false;
        }
        SubsystemRow other = (SubsystemRow) obj;
        String name = this.getName();
        String oName = other.getName();
        if (name == null) {
            if (oName != null) {
                return false;
            }
        } else if (!name.equals(oName)) {
            return false;
        }
        if (parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (!parent.getId().equals(other.parent.getId())) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(SubsystemRow o) {
        int retVal = this.getName().compareTo(o.getName());
        if (retVal == 0)
            retVal = this.parent.getId().compareTo(o.parent.getId());
        return retVal;
    }

    /**
     * @return the genome implementing this subsystem
     */
    public Object getGenome() {
        return this.parent;
    }

    /**
     * Specify a new variant code for this subsystem.
     *
     * @param code		new variant code
     */
    public void setVariantCode(String code) {
        this.variant.setCode(code);

    }

    /**
     * Set the classifications for this subsystem.
     *
     * @param classList		list of classifications, in order
     */
    public void setClassifications(List<String> classList) {
        this.classifications.clear();
        this.classifications.addAll(classList);
    }

}
