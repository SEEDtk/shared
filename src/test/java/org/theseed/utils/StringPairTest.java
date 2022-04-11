/**
 *
 */
package org.theseed.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

class StringPairTest {

    @Test
    void test() {
        var pair1 = new StringPair("ABC", "DEF");
        var pair2 = new StringPair("DEF", "ABC");
        var pair3 = new StringPair(null, null);
        var pair4 = new StringPair("ABC", null);
        var pair5 = new StringPair(null, "ABC");
        var pair6 = new StringPair("ABC", "GHI");
        assertThat(pair1, equalTo(pair2));
        assertThat(pair1.compareTo(pair2), equalTo(0));
        assertThat(pair3.compareTo(pair1), lessThan(0));
        assertThat(pair4, equalTo(pair5));
        assertThat(pair5.compareTo(pair4), equalTo(0));
        assertThat(pair6, not(equalTo(pair1)));
        assertThat(pair1.compareTo(pair6), not(equalTo(0)));
   }

}
