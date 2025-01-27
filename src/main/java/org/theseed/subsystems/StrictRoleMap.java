/**
 *
 */
package org.theseed.subsystems;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.io.TabbedLineReader;
import org.theseed.magic.MagicMap;

/**
 * This is a special role map for subsystem projection. When doing subsystem projection, there is
 * no role normalization, since the subsystems should only instantiate over authorized roles.
 *
 * A utility method is provided to parse functions into useful roles, as well as methods to save
 * and load the role map.
 *
 * @author Bruce Parrello
 *
 */
public class StrictRoleMap extends MagicMap<StrictRole> {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(StrictRoleMap.class);


    /**
     * Construct a blank, empty subsystem role map.
     *
     * @param searchObject	prototype strict-role object
     */
    public StrictRoleMap() {
        super(new StrictRole());
    }

    /**
     * Load a subsystem role map from a flat file.
     *
     * @param roleFile	flat file containing the role definitions
     *
     * @return the loaded role map
     *
     * @throws IOException
     */
    public static StrictRoleMap load(File roleFile) throws IOException {
        // Create an empty role map.
        StrictRoleMap retVal = new StrictRoleMap();
        // Open the load file. Note that it is headerless.
        try (TabbedLineReader roleStream = new TabbedLineReader(roleFile, 3)) {
            // The role ID is in the first column and the role name in the third. (This is a legacy
            // situation.)
            for (var line : roleStream) {
                String roleId = line.get(0);
                String name = line.get(2);
                StrictRole role = new StrictRole(roleId, name);
                retVal.put(role);
            }
        }
        return retVal;
    }

    /**
     * Save a subsystem role map to a flat file.
     *
     * @param roleFile	flat file to contain this role map's definitions
     *
     * @throws IOException
     */
    public void save(File roleFile) throws IOException {
        // Open the output file.
        try (PrintWriter writer = new PrintWriter(roleFile)) {
            // Write out all the roles.
            for (StrictRole role : this)
                writer.println(role.getId() + "\tx\t" + role.getName());
        }
    }

    /**
     * Read this subsystem role map from an object stream.
     *
     * @param in	input object stream
     */
    public void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        // Get the number of roles in this map.
        final int size = in.readInt();
        // Read through the roles in sequence and add them to the map.
        for (int i = 0; i < size; i++) {
            String roleId = in.readUTF();
            String roleDesc = in.readUTF();
            StrictRole role = new StrictRole(roleId, roleDesc);
            this.put(role);
        }
    }

    /**
     * Write this subsystem role map to an object stream.
     *
     * @param out	output object stream
     */
    public void writeObject(ObjectOutputStream out) throws IOException {
        // Write the number of roles in this map.
        out.writeInt(this.size());
        // Run through the roles in sequence and write them.
        for (StrictRole role : this) {
            out.writeUTF(role.getId());
            out.writeUTF(role.getName());
        }
    }

    /**
     * @return a list of the mapped roles in a functional assignment
     *
     * @param function	array of role descriptions to scan
     */
    public List<StrictRole> usefulRoles(String function) {
        String[] roleNames = Feature.rolesOfFunction(function);
        List<StrictRole> retVal = new ArrayList<StrictRole>(roleNames.length);
        for (String roleName : roleNames) {
            StrictRole role = this.getByName(roleName);
            if (role != null) {
                retVal.add(role);
            }
        }
        return retVal;
    }

    /**
     * Find or insert a new role into the map.
     *
     * @param roleName	role string to find
     *
     * @return the ID for the role
     */
    public String findOrInsert(String roleName) {
        StrictRole role = this.getByName(roleName);
        if (role == null) {
            // Here we must add the role. Doing a put when the ID is null
            // generates a new ID.
            role = new StrictRole(null, roleName);
            this.put(role);
        }
        return role.getId();
    }

    /**
     * Get a role presence map for a genome. The role presence map contains each role ID and maps it
     * to the set of IDs for the features containing the role. This can be used to project subdsystems
     * onto the genome via the subsystem rules.
     *
     * @param genome	genome to process
     *
     * @return a map from role IDs to feature ID sets for all the roles in the genome
     */
    public Map<String, Set<String>> getRolePresenceMap(Genome genome) {
        Map<String, Set<String>> retVal = new HashMap<String, Set<String>>(genome.getFeatureCount());
        for (Feature feat : genome.getFeatures()) {
            String function = feat.getFunction();
            // Get all the roles in the function found in our role map.
            var roles = this.usefulRoles(function);
            for (StrictRole role : roles) {
                // Extract the role ID and get its entry in the output map.
                String roleId = role.getId();
                Set<String> fidSet = retVal.computeIfAbsent(roleId, x -> new TreeSet<String>());
                // Add this feature to the role's feature set.
                fidSet.add(feat.getId());
            }
        }
        return retVal;
    }

}
