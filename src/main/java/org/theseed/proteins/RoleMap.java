/**
 *
 */
package org.theseed.proteins;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.theseed.utils.MagicMap;

/**
 * @author parrello
 *
 */
public class RoleMap extends MagicMap<Role> {

    /**
     * Create a new, empty role map.
     */
    public RoleMap() {
        super(new Role());
    }

    /**
     * Find the named role.  If it does not exist, a new role will be created.
     *
     * @param roleDesc	the role name
     *
     * @return	a Role object for the role
     */
    public Role findOrInsert(String roleDesc) {
        Role retVal = this.getByName(roleDesc);
        if (retVal == null) {
            // Create a role without an ID.
            retVal = new Role(null, roleDesc);
            // Store it in the map to create the ID.
            this.put(retVal);
        }
        return retVal;
    }

    public void save(File saveFile) {
        try {
            PrintWriter printer = new PrintWriter(saveFile);
            for (Role role : this.values()) {
                printer.format("%s\t%s%n", role.getId(), role.getName());
            }
            printer.close();
        } catch (IOException e) {
            throw new RuntimeException("Error saving role map.", e);
        }
    }

    public static RoleMap load(File loadFile) {
        RoleMap retVal = new RoleMap();
        try {
            Scanner reader = new Scanner(loadFile);
            while (reader.hasNext()) {
                String myLine = reader.nextLine();
                String[] fields = StringUtils.splitByWholeSeparator(myLine, "\t", 2);
                Role newRole = new Role(fields[0], fields[1]);
                retVal.put(newRole);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Error loading role map.", e);
        }
        return retVal;
    }

}
