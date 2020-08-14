/**
 *
 */
package org.theseed.test;

/**
 * This is the import class for the new matchers.
 *
 * @author Bruce Parrello
 *
 */
public class Matchers {

    /**
     * Match if the input is TRUE.
     */
    public static BooleanMatcher isTrue() {
        return new BooleanMatcher(true);
    }

    /**
     * Match if the input is FALSE.
     */
    public static BooleanMatcher isFalse() {
        return new BooleanMatcher(false);
    }
}
