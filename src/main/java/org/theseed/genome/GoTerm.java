/**
 *
 */
package org.theseed.genome;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.cliftonlabs.json_simple.JsonArray;

/**
 * This class represents a Gene Ontology term associated with a feature.  Each such term contains an ID number
 * and a description.  It is common for the description to be empty (in which case the client may look it up if
 * necessary).
 *
 * @author Bruce Parrello
 *
 */
public class GoTerm {

    private static final Pattern GO_PATTERN = Pattern.compile("GO:(\\d+)(?:\\|(.+))?");

    // FIELDS
    private int number;
    private String description;

    /**
     * Create a GO term without a description.
     *
     * @param number	GO number
     */
    public GoTerm(int number) {
        this.number = number;
        this.description = null;
    }

    /**
     * Create a GO term from a GO string.
     *
     * @param goString	a string containing "GO:", the GO number, plus optionally a bar ("|") and
     * 					the GO description
     */
    public GoTerm(String goString) {
        parseTermString(goString);
    }

    /**
     * Initialize this GO term from a GO string.
     *
     * @param goString	a string containing "GO:", the GO number, plus optionally a bar ("|") and
     * 					the GO description
     */
    protected void parseTermString(String goString) {
        Matcher m = GO_PATTERN.matcher(goString);
        if (m.matches()) {
            this.number = Integer.parseUnsignedInt(m.group(1));
            this.description = m.group(2);
        } else
            throw new IllegalArgumentException("Illegal GO string: " + goString);
    }

    /**
     * Create a GO term from an incoming JSON array. The array should contain one or two strings.  The first
     * is the GO term number (with the GO: prefix) and the second is the description (which is optional).
     *
     * @param goTermObj		JSON array containing gene ontology term
     */
    public GoTerm(JsonArray goTermObj) {
        String goString = goTermObj.getString(0);
        if (goTermObj.size() > 1)
            goString += "|" + goTermObj.getString(1);
        parseTermString(goString);
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description 	the new term description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the GO number
     */
    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        String retVal = String.format("GO:%07d", this.number);
        if (this.description != null)
            retVal += "|" + this.description;
        return retVal;
    }

    @Override
    public int hashCode() {
        return number;
    }

    @Override
    public boolean equals(Object obj) {
        boolean retVal = false;
        if (this == obj) {
            retVal = true;
        } else if (!(obj instanceof GoTerm)) {
            retVal = false;
        } else {
            GoTerm other = (GoTerm) obj;
            if (number != other.number)
                retVal = false;
            else if (description == null)
                retVal = (other.description == null);
            else
                retVal = (other.description != null &&
                        description.contentEquals(other.description));
        }
        return retVal;
    }
}
