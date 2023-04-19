/**
 *
 */
package org.theseed.subsystems;

import org.theseed.magic.MagicObject;

/**
 * This object represents a subsystem name.  Its primary utility is in generating subsystem IDs and maps for them.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemName extends MagicObject {

    // FIELDS
    /** type ID number for serialiation */
    private static final long serialVersionUID = -8239693749019702662L;

    /**
     * Construct a subsystem name object from an ID and name.
     *
     * @param id		subsystem ID
     * @param name		subsystem name
     */
    public SubsystemName(String id, String name) {
        super(id, name);
    }

    /**
     * Construct a blank, empty subsystem name object.
     */
    public SubsystemName() {
    }

    /**
     * This method normalizes a subsystem name.  Since even a tiny punctuation or capitalization change can denote
     * a different subsystem, no normalization is performed.
     */
    @Override
    protected String normalize(String name) {
        return name;
    }

}
