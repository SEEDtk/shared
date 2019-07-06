/**
 *
 */
package org.theseed.shared;

import org.theseed.utils.MagicObject;

/**
 * Test class for magic objects.
 *
 * @author Bruce Parrello
 *
 */
public class Thing extends MagicObject {

    /**
     *
     */
    private static final long serialVersionUID = -5855290474686618053L;

    /** Create a blank thing. */
    public Thing() { }

    /** Create a new thing with a given ID and description. */
    public Thing(String thingId, String thingDesc) {
        super(thingId, thingDesc);
    }

    @Override
    protected String normalize() {
        // Convert all sequences of non-word characters to a single space and lower-case it.
        String retVal = this.getName().replaceAll("\\W+", " ").toLowerCase();
        return retVal;
    }

}
