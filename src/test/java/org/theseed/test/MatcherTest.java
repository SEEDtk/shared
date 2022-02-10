/**
 *
 */
package org.theseed.test;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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
        assertThat(frog.isEmpty(), equalTo(false));
        assertThat(frog.isEmpty(), equalTo(false));
        assertThat(true, equalTo(true));
        try {
            assertThat(frog.isEmpty(), equalTo(true));
            fail();
        } catch (AssertionError e) {
            assertThat(e.toString(), containsString("Expected: <true>"));
        }
        try {
            assertThat(! frog.isEmpty(), equalTo(false));
            fail();
        } catch (AssertionError e) {
            assertThat(e.toString(), containsString("Expected: <false>"));
        }
    }
}
