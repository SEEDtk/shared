/**
 *
 */
package org.theseed.proteins;

import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.theseed.magic.MagicObject;

/**
 * This class represents a full functional assignment, and is used when we want to classify proteins by function instead
 * of role.
 *
 * @author Bruce Parrello
 *
 */
public class Function extends MagicObject {

    // FIELDS
    /** serialization ID */
    private static final long serialVersionUID = -454453869754978161L;
    /** parsing pattern for removing function comments */
    private static final Pattern COMMENT_PATTERN = Pattern.compile("\\s*[#!].+");
    /** parsing pattern for removing EC numbers */
    private static final Pattern EC_PATTERN = Pattern.compile(Role.EC_REGEX);
    /** parsing pattern for removing TC numbers */
    private static final Pattern TC_PATTERN = Pattern.compile(Role.TC_REGEX);


    /**
     * Create a blank function object.
     */
    public Function() {
        super();
    }

    /**
     * Create a function object with a blank ID for a specific description.
     *
     * @param funDesc	function description
     */
    public Function(String funDesc) {
        super(null, funDesc);
    }

    @Override
    protected String normalize() {
        String retVal = commentFree(this.getName());
        // Remove all the EC and TC numbers.
        retVal = RegExUtils.replaceAll(retVal, EC_PATTERN, " ");
        retVal = RegExUtils.replaceAll(retVal, TC_PATTERN, " ");
        // Fix common spelling and punctuation errors.
        retVal = Role.fixSpelling(retVal);
        // Remove any leftover spaces or extra punctuation.
        retVal = RegExUtils.replaceAll(retVal, Role.EXTRA_SPACES, " ");
        // Return the normalized result.
        return retVal;
    }

    /**
     * @return a function with the comment removed
     *
     * @param function	function to check for comments
     */
    public static String commentFree(String function) {
        return RegExUtils.removeFirst(function, COMMENT_PATTERN);
    }


}
