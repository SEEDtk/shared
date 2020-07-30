package org.theseed.proteins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.theseed.magic.MagicObject;

import murmur3.MurmurHash3.LongPair;

public class Role extends MagicObject implements Comparable<Role> {

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
        retVal = normalize(retVal);
        return retVal;
    }

    /**
     * @return the normalized form of a role description
     *
     * @param roleDesc		role description to normalize
     */
    protected String normalize(String roleDesc) {
        // Extract the EC and TC numbers.
        String ecNum = null;
        String tcNum = null;
        Matcher m = EC_PATTERN.matcher(roleDesc);
        if (m.matches()) {
            roleDesc = MagicObject.join_text(m.group(1), m.group(3));
            ecNum = m.group(2);
        }
        m = TC_PATTERN.matcher(roleDesc);
        if (m.matches()) {
            roleDesc = MagicObject.join_text(m.group(1), m.group(3));
            tcNum = m.group(2);
        }
        // Convert to lower case so case doesn't matter.
        roleDesc = roleDesc.toLowerCase();
        // Fix spelling mistakes in "hypothetical".
        roleDesc = RegExUtils.replaceAll(roleDesc, HYPO_WORD_PATTERN, "hypothetical");
        // Remove extra spaces and quotes.
        roleDesc = RegExUtils.replaceAll(roleDesc, CR_PATTERN, " ");
        roleDesc = roleDesc.trim();
        if (roleDesc.startsWith("\"")) {
            roleDesc = roleDesc.substring(1);
        }
        if (roleDesc.endsWith("\"")) {
            roleDesc = StringUtils.chop(roleDesc);
        }
        roleDesc = RegExUtils.replaceAll(roleDesc, SPACE_PATTERN, " ");
        // If we have a hypothetical with a number, replace it.
        if (roleDesc.equals("hypothetical protein") || roleDesc.isEmpty()) {
            if (ecNum != null) {
                roleDesc = "putative protein " + ecNum;
            } else if (tcNum != null) {
                roleDesc = "putative transporter " + tcNum;
            }
        }
        // Now remove the extra spaces and punctuation.
        roleDesc = RegExUtils.replaceAll(roleDesc, EXTRA_SPACES, " ");
        return roleDesc;
    }

    @Override
    public int compareTo(Role o) {
        return super.compareTo(o);
    }

    /**
     * @return TRUE if this role matches the specified name string
     *
     * @param roleDesc		role name to check
     */
    public boolean matches(String roleDesc) {
        LongPair check = this.checksumOf(this.normalize(roleDesc));
        return check.equals(this.getChecksum());
    }

}
