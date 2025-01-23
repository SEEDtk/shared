/**
 *
 */
package org.theseed.subsystems;

import org.theseed.magic.MagicObject;

/**
 * A strict role is a role-like magic object where there is no normalization. This is used for
 * subsystem projection, where only the authorized version of a role indicates the possibility of
 * a subsystem presence.
 *
 * @author Bruce Parrello
 *
 */
public class StrictRole extends MagicObject {

    /** serialization ID */
    private static final long serialVersionUID = 318453324521699303L;

    public StrictRole(String id, String name) {
        super(id, name);
    }

    public StrictRole() { }

    @Override
    protected String normalize(String name) {
        return name;
    }

}
