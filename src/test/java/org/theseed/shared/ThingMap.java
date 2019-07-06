/**
 *
 */
package org.theseed.shared;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.theseed.utils.MagicMap;

/**
 * Test class for magic maps.
 *
 * @author Bruce Parrello
 *
 */
public class ThingMap extends MagicMap<Thing> {

    public ThingMap() {
        super(new Thing());
    }

    /**
     * Find the named thing.  If it does not exist, a new thing will be created.
     *
     * @param thingDesc	the thing name
     *
     * @return	a Thing object for the thing
     */
    public Thing findOrInsert(String thingDesc) {
        Thing retVal = this.getByName(thingDesc);
        if (retVal == null) {
            // Create a thing without an ID.
            retVal = new Thing(null, thingDesc);
            // Store it in the map to create the ID.
            this.put(retVal);
        }
        return retVal;
    }

    public void save(File saveFile) {
        try {
            PrintWriter printer = new PrintWriter(saveFile);
            for (Thing thing : this.values()) {
                printer.format("%s\t%s%n", thing.getId(), thing.getName());
            }
            printer.close();
        } catch (IOException e) {
            throw new RuntimeException("Error saving thing map.", e);
        }
    }

    public static ThingMap load(File loadFile) {
        ThingMap retVal = new ThingMap();
        try {
            Scanner reader = new Scanner(loadFile);
            while (reader.hasNext()) {
                String myLine = reader.nextLine();
                String[] fields = StringUtils.splitByWholeSeparator(myLine, "\t", 2);
                Thing newThing = new Thing(fields[0], fields[1]);
                retVal.put(newThing);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Error loading thing map.", e);
        }
        return retVal;
    }


}
