/**
 *
 */
package org.theseed.utils;

/**
 * This object represents an unordered string pair.  The strings are internally stored sorted so
 * comparisons work properly.
 *
 * @author Bruce Parrello
 *
 */
public class StringPair implements Comparable<StringPair> {

    // FIELDS
    /** first string */
    private String string1;
    /** second string */
    private String string2;

    /**
     * Construct a new unordered pair of strings.
     *
     * @param s1	first string to store
     * @param s2	second string to store
     */
    public StringPair(String s1, String s2) {
        if (s1 == null || s2 != null && s1.compareTo(s2) <= 0) {
            this.string1 = s1;
            this.string2 = s2;
        } else {
            this.string1 = s2;
            this.string2 = s1;
        }
    }

    @Override
    public int compareTo(StringPair o) {
        // The only tricky part here is sorting nulls to the front.
        int retVal;
        if (this.string1 == null) {
            if (o.string1 == null)
                retVal = 0;
            else
                retVal = -1;
        } else if (o.string2 == null)
            retVal = 1;
        else {
            retVal = this.string1.compareTo(o.string1);
            if (retVal == 0) {
                if (this.string2 != o.string2) {
                    if (this.string1 == null)
                        retVal = -1;
                    else if (this.string2 == null)
                        retVal = 1;
                    else
                        retVal = this.string2.compareTo(o.string2);
                }
            }
        }
        return retVal;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.string1 == null) ? 0 : this.string1.hashCode());
        result = prime * result + ((this.string2 == null) ? 0 : this.string2.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StringPair)) {
            return false;
        }
        StringPair other = (StringPair) obj;
        if (this.string1 == null) {
            if (other.string1 != null) {
                return false;
            }
        } else if (!this.string1.equals(other.string1)) {
            return false;
        }
        if (this.string2 == null) {
            if (other.string2 != null) {
                return false;
            }
        } else if (!this.string2.equals(other.string2)) {
            return false;
        }
        return true;
    }

    /**
     * @return the first string
     */
    public String getString1() {
        return this.string1;
    }

    /**
     * @return the second string
     */
    public String getString2() {
        return this.string2;
    }

}
