/**
 *
 */
package org.theseed.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Bruce Parrello
 *
 */
class TestPatternMap {

    String[] ACTUAL_STRINGS = new String[]  { "Quartering", "Quintus Parrello", "Lucky", "Boots Parrello", "Poo-bah Kallas", "LuckyKallas", "Quitting" };
    Integer[] ACTUAL_OBJECTS = new Integer[] { 4,            1,                  3,       2,                null,             3,             1         };
    String[] TEST_PATTERNS = new String[] { "Qui.+", ".+Parrello", "Luc\\w+", "Qu.+" };
    Integer[] TEST_OBJECTS = new Integer[] { 1,       2,            3,        4 };

    @Test
    void test() {
        PatternMap<Integer> testMap = new PatternMap<Integer>();
        assertThat("Map not initialized empty.", testMap.isEmpty());
        assertThat(testMap.size(), equalTo(0));
        for (int i = 0; i < TEST_PATTERNS.length; i++)
            testMap.add(TEST_PATTERNS[i], TEST_OBJECTS[i]);
        assertThat(testMap.size(), equalTo(4));
        for (int i = 0; i < ACTUAL_STRINGS.length; i++) {
            Integer found = testMap.get(ACTUAL_STRINGS[i]);
            assertThat(ACTUAL_STRINGS[i], found, equalTo(ACTUAL_OBJECTS[i]));
        }
    }

}
