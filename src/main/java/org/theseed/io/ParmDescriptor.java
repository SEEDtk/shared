/**
 *
 */
package org.theseed.io;

import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.text.TextStringBuilder;

/**
 * This class represents the description of a single parameter.
 */
public class ParmDescriptor implements Comparable<ParmDescriptor> {

    /** TRUE if we are commented out */
    private boolean commented;
    /** name of the parameter */
    private String name;
    /** value of the parameter */
    private String value;
    /** parameter comment */
    private String description;
    /** preceding full-line comments */
    private String[] lineComments;

    /**
     * Create a descriptor from input lines.
     *
     * @param m			matched input line
     * @param comments	preceding line comments
     */
    public ParmDescriptor(Matcher m, List<String> comments) {
        this.commented = (m.group(1) != null);
        this.name = m.group(2);
        this.value = (m.group(3) == null ? "" : m.group(3));
        this.description = (m.group(4) == null ? "" : m.group(4));
        this.lineComments = new String[comments.size()];
        this.lineComments = comments.toArray(this.lineComments);
    }

    /**
     * Create a simple parm descriptor.
     *
     * @param key		parameter name
     * @param value		parameter value
     * @param desc		parameter description
     */
    public ParmDescriptor(String key, String value, String desc) {
        this.commented = false;
        this.name = key;
        this.value = value;
        this.description = desc;
        this.lineComments = new String[0];
    }

    @Override
    public int compareTo(ParmDescriptor o) {
        return (this.name.compareTo(o.name));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ParmDescriptor)) {
            return false;
        }
        ParmDescriptor other = (ParmDescriptor) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        TextStringBuilder buffer = new TextStringBuilder(80);
        for (String lineComment : this.lineComments)
            buffer.appendln(lineComment);
        if (this.commented)
            buffer.append("# ");
        buffer.append("--").append(this.name);
        buffer.append(" ").append(this.value);
        if (! this.description.isEmpty())
            buffer.append("\t# ").append(this.description);
        return buffer.toString();
    }

    /**
     * @return TRUE if this parameter is commented out
     */
    public boolean isCommented() {
        return this.commented;
    }

    /**
     * @param commented 	TRUE to comment out this parameter, FALSE to enable it
     */
    public void setCommented(boolean commented) {
        this.commented = commented;
    }

    /**
     * @return the value of the parameter
     */
    public String getValue() {
        return this.value;
    }

    /**
     * @param value 	new value for the parameter
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the name of the parameter
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the description of the parameter
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return the lineComments
     */
    public String[] getLineComments() {
        return this.lineComments;
    }

}
