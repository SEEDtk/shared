/**
 *
 */
package org.theseed.test;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * This is a Hamcrest matcher for booleans.
 *
 * @author Bruce Parrello
 *
 */
public class BooleanMatcher extends TypeSafeMatcher<Boolean> {

    private boolean target;

    public BooleanMatcher(Boolean target) {
        this.target = target;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(this.target ? "<true>" : "<false>");
    }

    @Override
    protected boolean matchesSafely(Boolean item) {
        return (item == this.target);
    }

}
