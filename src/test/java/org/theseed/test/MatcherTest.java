/**
 *
 */
package org.theseed.test;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.theseed.test.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tester for custom hamcrest matchers.
 *
 * @author Bruce Parrello
 *
 */
public class MatcherTest {

    @Test
    public void testBoolean() {
        String frog = "frog";
        assertThat(frog.isEmpty(), isFalse());
        assertThat(frog.isEmpty(), equalTo(false));
        assertThat(true, isTrue());
        try {
            assertThat(frog.isEmpty(), isTrue());
            fail();
        } catch (AssertionError e) {
            assertThat(e.toString(), containsString("Expected: <true>"));
        }
        try {
            assertThat(! frog.isEmpty(), isFalse());
            fail();
        } catch (AssertionError e) {
            assertThat(e.toString(), containsString("Expected: <false>"));
        }
    }
}
