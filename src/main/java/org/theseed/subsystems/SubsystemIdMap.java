/**
 *
 */
package org.theseed.subsystems;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.TabbedLineReader;
import org.theseed.magic.MagicMap;

/**
 * This object represents a map from subsystem IDs to names (and vice versa).  It can also be used
 * to generate IDs for new subsystems.  It is designed to be easily saved to a tab-delimited file and
 * reloaded.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemIdMap extends MagicMap<SubsystemName> {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SubsystemIdMap.class);

    /**
     * Construct a new, blank subsystem ID map.
     */
    public SubsystemIdMap() {
        super(new SubsystemName());
    }

    /**
     * Load a subsystem ID map from a file.
     *
     * @param loadFile	name of file to which the map had been saved
     *
     * @throws IOException
     */
    public static SubsystemIdMap load(File loadFile) throws IOException {
        // Create the blank map.
        SubsystemIdMap retVal = new SubsystemIdMap();
        // Connect to the load file.
        try (TabbedLineReader inStream = new TabbedLineReader(loadFile)) {
            // Locating the columns by name helps to insure the input file is valid.
            int idCol = inStream.findField("sub_id");
            int nameCol = inStream.findField("sub_name");
            // Loop through the input file, storing ID/name pairs.
            for (var line : inStream) {
                SubsystemName nameDesc = new SubsystemName(line.get(idCol), line.get(nameCol));
                retVal.put(nameDesc);
            }
        }
        log.info("{} subsystems loaded from {}.", retVal.size(), loadFile);
        return retVal;
    }

    /**
     * Find or insert a subsystem by name.  If the subsystem is not in the map, it will be created and an ID generated
     * for it; otherwise, its current ID will be returned.
     *
     * @param name	name of the subsystem of interest
     *
     * @return the ID of the subsystem
     */
    public String findOrInsert(String name) {
        SubsystemName nameDesc = this.getByName(name);
        if (nameDesc == null) {
            // Here we have to create an ID for the subsystem. We do this by creating a name object without an ID
            // and inserting it.
            nameDesc = new SubsystemName(null, name);
            this.put(nameDesc);
        }
        // Return the ID found.
        return nameDesc.getId();
    }

    /**
     * Save this map to a file.
     *
     * @param saveFile	name of the output file to contain the map
     *
     * @throws IOException
     */
    public void save(File saveFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(saveFile)) {
            writer.println("sub_id\tsub_name");
            for (var descriptor : this.objectValues())
                writer.println(descriptor.getId() + "\t" + descriptor.getName());
        }
        log.info("{} subsystems saved to {}.", this.size(), saveFile);
    }

}
