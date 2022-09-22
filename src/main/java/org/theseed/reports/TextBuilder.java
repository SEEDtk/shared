/**
 *
 */
package org.theseed.reports;

import org.apache.commons.text.TextStringBuilder;

/**
 * This is a subclass of TextStringBuilder that adds utilities for creating proper English.
 *
 * @author Bruce Parrello
 *
 */
public class TextBuilder extends TextStringBuilder {

    // FIELDS
    /** serialization identifier */
    private static final long serialVersionUID = 6578926530435425051L;

    /**
     * Construct a text builder with a default capacity of 100 characters.
     */
    public TextBuilder() {
        super(100);
    }

    /**
     * Construct a text builder with the specified capacity.
     *
     * @param initialCapacity	desired initial capacity
     */
    public TextBuilder(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Construct a text builder with an initial string plus 32 characters.
     *
     * @param str	initial string content
     */
    public TextBuilder(String str) {
        super(str);
    }

    /**
     * Append a count with a singular or plural word matching the proper case.
     *
     * @param count		count to append
     * @param singular	singular word to use
     * @param plural	plural word to use
     *
     * @return this object, for chaining
     */
    public TextBuilder append(int count, String singular, String plural) {
        this.append(count).append(' ');
        if (count == 1)
            this.append(singular);
        else
            this.append(plural);
        return this;
    }

    /**
     * Append a simple string.  This method is provided to ease chaining.
     *
     * @param str	string to append
     */
    public TextBuilder append(String str) {
        super.append(str);
        return this;
    }

}
