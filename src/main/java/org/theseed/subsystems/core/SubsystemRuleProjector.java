/**
 *
 */
package org.theseed.subsystems.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.proteins.RoleSet;
import org.theseed.subsystems.StrictRole;
import org.theseed.subsystems.StrictRoleMap;

/**
 * This object will project subsystems onto a genome using rules. It is serializable to an from an object stream.
 * Note there is no provision for subsystems without rules. If a subsystem has no rules, the rules need to be
 * generated using the RuleGenerator class.
 *
 * The projector consists of a strict role map and a descriptor for each subsystem.
 *
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemRuleProjector implements Serializable {


    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SubsystemRuleProjector.class);
    /** map of subsystem roles */
    private StrictRoleMap roleMap;
    /** list of subsystem descriptors */
    private Map<String, SubsystemDescriptor> subsystems;
    /** serialization object ID */
    private static final long serialVersionUID = 3404995748940446240L;

    /**
     * Create a blank, empty subsystem rule projector.
     */
    public SubsystemRuleProjector() {
        this.roleMap = new StrictRoleMap();
        this.subsystems = new HashMap<String, SubsystemDescriptor>();
    }

    /**
     * Create a subsystem rule projector with pre-loaded role definitions.
     *
     * @param roleFile	file of role definitions, or NULL if there is none
     *
     * @throws IOException
     */
    public SubsystemRuleProjector(File roleFile) throws IOException {
        if (roleFile != null) {
            this.roleMap = StrictRoleMap.load(roleFile);
            log.info("{} roles loaded from {}.", this.roleMap.size(), roleFile);
        } else
            this.roleMap = new StrictRoleMap();
        this.subsystems = new HashMap<String, SubsystemDescriptor>(1000);
    }

    /**
     * Load a subsystem projector from a binary save file.
     *
     * @param projectorFile		name of the save file containing the projector
     *
     * @return the projector loaded from the file
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static SubsystemRuleProjector load(File projectorFile) throws IOException {
        SubsystemRuleProjector retVal = null;
        try (FileInputStream inStream = new FileInputStream(projectorFile)) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            try {
                retVal = (SubsystemRuleProjector) in.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e.toString());
            }
            in.close();
        }
        return retVal;
    }

    /**
     * Compute the role IDs for a function.
     *
     * @param function	functional assignment
     *
     * @return the set of role IDs for the function
     */
    public RoleSet getRoleIds(String function) {
        return RoleSet.create(function, this.roleMap);
    }

    /**
     * @return the subsystem role map
     */
    public StrictRoleMap usefulRoles() {
        return this.roleMap;
    }

    /**
     * Project active subsystems or all subsystems onto the specified genome.
     *
     * @param genome		target genome for the projection
     * @param activeFlag	TRUE to project only active subsystems, else FALSE
     */
    public void project(Genome genome, boolean activeFlag) {
        // Get the genome's role presence map.
        Map<String, Set<String>> rolePresenceMap = this.roleMap.getRolePresenceMap(genome);
        // Do the projection.
        this.project(genome, rolePresenceMap, activeFlag);
    }

    /**
     * Project active subsystems or all subsystems onto the specified genome. This is an alternate
     * method where the role presence map has already been created.
     *
     * @param genome			target genome for the projection
     * @param rolePresenceMap	role presence map for the genome
     * @param activeFlag		TRUE to project only active subsystems, else FALSE
     */
    public void project(Genome genome, Map<String, Set<String>> rolePresenceMap, boolean activeFlag) {
        // Erase the existing subsystems.
        genome.clearSubsystems();
        // Project from the subsystem descriptors.
        this.subsystems.values().parallelStream()
                .forEach(x -> x.project(genome, rolePresenceMap, this.roleMap, activeFlag));
    }

    /**
     * Save this projector to a file.
     *
     * @param projectorFile		output file for the save
     *
     * @throws IOException
     */
    public void save(File projectorFile) throws IOException {
        try (FileOutputStream fileStream = new FileOutputStream(projectorFile)) {
            ObjectOutputStream out = new ObjectOutputStream(fileStream);
            out.writeObject(this);
            out.close();
        }
        log.info("Subsystem projector saved to {}.", projectorFile);
    }

    /**
     * Get a subsystem's descriptor.
     *
     * @param subName	name of the subsystem whose descriptor is desired
     *
     * @return the subsystem's descriptor, or NULL if it does not exist
     */
    public SubsystemDescriptor getSubsystem(String subName) {
        return this.subsystems.get(subName);
    }

    /**
     * Get the list of roles in a function assignment.
     *
     * @param function	functional assignment to parse
     *
     * @return a list of subsystem roles found in the function
     */
    public List<StrictRole> getUsefulRoles(String function) {
        return this.roleMap.usefulRoles(function);
    }

    /**
     * Add a new subsystem definition to the projector.
     *
     * @param sub	subsystem object loaded from the directory
     */
    public void addSubsystem(CoreSubsystem sub) {
        SubsystemDescriptor subDesc = new SubsystemDescriptor(sub);
        this.subsystems.put(sub.getName(), subDesc);
    }

}