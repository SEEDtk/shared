/**
 *
 */
package org.theseed.subsystems;

/**
 * This object identifies a subsystem variant.  The variant is identified by a subsystem name
 * and a variant code.  Note that this object does not describe a variant, it merely identifies
 * it.
 *
 * @author Bruce Parrello
 */
public class VariantId implements Comparable<VariantId> {

    // FIELDS

    /** name of the subsystem */
    private String name;

    /** variant code */
    private String code;

    /**
     * Create a new variant.
     *
     * @param name	parent subsystem name
     * @param code	variant code string
     */
    public VariantId(String name, String code) {
        this.name = name;
        this.code = code;
    }

    @Override
    public int compareTo(VariantId o) {
        int retVal = this.name.compareTo(o.name);
        if (retVal == 0)
            retVal = this.code.compareTo(o.code);
        return retVal;
    }

    /**
     * @return TRUE if the variant code indicates an active variant, else FALSE
     *
     * @param code	variant code of interest
     */
    public static boolean isActive(String code) {
        boolean retVal;
        if (code.startsWith("*"))
            code = code.substring(1);
        if (code.contentEquals("active"))
            retVal = true;
        else if (code.contentEquals("inactive"))
            retVal = false;
        else if (code.contentEquals("missing"))
            retVal = false;
        else if (code.startsWith("-"))
            retVal = false;
        else if (code.contentEquals("0"))
            retVal = false;
        else
            retVal = true;
        return retVal;
    }

    /**
     * @return TRUE if this is an active variant, else FALSE
     */
    public boolean isActive() {
        return VariantId.isActive(this.code);
    }

    /**
     * @return the subsystem name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the variant code
     */
    public String getCode() {
        return code;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((code == null) ? 0 : code.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VariantId)) {
            return false;
        }
        VariantId other = (VariantId) obj;
        if (code == null) {
            if (other.code != null) {
                return false;
            }
        } else if (!code.equals(other.code)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Update the variant code
     *
     * @param code	new variant code to use
     */
    public void setCode(String code) {
        this.code = code;
    }

}
