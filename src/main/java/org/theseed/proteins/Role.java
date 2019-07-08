package org.theseed.proteins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.theseed.utils.MagicObject;

public class Role extends MagicObject {

    // ROLE-PARSING PATTERNS
    static private final Pattern EC_PATTERN = Pattern.compile("(.+?)\\s*\\(\\s*E\\.?C\\.?(?:\\s+|:)(\\d\\.(?:\\d+|-)\\.(?:\\d+|-)\\.(?:n?\\d+|-))\\s*\\)\\s*(.*)");
    static private final Pattern TC_PATTERN = Pattern.compile("(.+?)\\s*\\(\\s*T\\.?C\\.?(?:\\s+|:)(\\d\\.[A-Z]\\.(?:\\d+|-)\\.(?:\\d+|-)\\.(?:\\d+|-)\\s*)\\)\\s*(.*)");
    static private final Pattern HYPO_WORD_PATTERN = Pattern.compile("^\\d{7}[a-z]\\d{2}rik\\b|\\b(?:hyphothetical|hyothetical)\\b");
    static private final Pattern CR_PATTERN = Pattern.compile("\\r");
    static private final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
    static private final Pattern EXTRA_SPACES = Pattern.compile("[\\s,.:]{2,}");


    /**
     * serialization object type ID
     */
    private static final long serialVersionUID = 3656080126488315968L;

    /**
     * Create a role with a known ID and name.
     *
     * @param id	ID of this role
     * @param name	name of the role
     */
    public Role(String id, String name) {
        super(id, name);
    }

    /**
     * Create a blank, empty role object.
     */
    public Role() { }

    @Override
    /** Compute the normalized version of the role description. */
    protected String normalize() {
        String retVal = this.getName();
        // Extract the EC and TC numbers.
        String ecNum = null;
        String tcNum = null;
        Matcher m = EC_PATTERN.matcher(retVal);
        if (m.matches()) {
            retVal = MagicObject.join_text(m.group(1), m.group(3));
            ecNum = m.group(2);
        }
        m = TC_PATTERN.matcher(retVal);
        if (m.matches()) {
            retVal = MagicObject.join_text(m.group(1), m.group(3));
            tcNum = m.group(2);
        }
        // Convert to lower case so case doesn't matter.
        retVal = retVal.toLowerCase();
        // Fix spelling mistakes in "hypothetical".
        retVal = RegExUtils.replaceAll(retVal, HYPO_WORD_PATTERN, "hypothetical");
        // Remove extra spaces and quotes.
        retVal = RegExUtils.replaceAll(retVal, CR_PATTERN, " ");
        retVal = retVal.trim();
        if (retVal.startsWith("\"")) {
            retVal = retVal.substring(1);
        }
        if (retVal.endsWith("\"")) {
            retVal = StringUtils.chop(retVal);
        }
        retVal = RegExUtils.replaceAll(retVal, SPACE_PATTERN, " ");
        // If we have a hypothetical with a number, replace it.
        if (retVal.equals("hypothetical protein") || retVal.isEmpty()) {
            if (ecNum != null) {
                retVal = "putative protein " + ecNum;
            } else if (tcNum != null) {
                retVal = "putative transporter " + tcNum;
            }
        }
        // Now remove the extra spaces and punctuation.
        retVal = RegExUtils.replaceAll(retVal, EXTRA_SPACES, " ");
        return retVal;
    }

}
