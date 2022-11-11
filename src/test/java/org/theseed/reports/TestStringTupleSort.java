/**
 *
 */
package org.theseed.reports;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestStringTupleSort {

    public static final String[] TEST1 = new String[] { "AAA", "BBB", "CCC" };
    public static final String[] TEST2 = new String[] { "BBB", "AAA", "CCC" };
    public static final String[] TEST2a = new String[] { "BBB", "AAA", "CCC" };
    public static final String[] TEST3 = new String[] { "BBB", "BBB" };
    public static final String[] TEST4 = new String[] { "BBB", "BBB", "BBB" };
    public static final String[] TEST5 = new String[] { "BBB", "BBB", "BBB", "BBB" };
    public static final String[] TEST6 = new String[] { "BBB", "BBB", "CCC" };

    @Test
    void test() {
        StringTupleSort sorter = new StringTupleSort();
        assertThat(sorter.compare(TEST2, TEST2a), equalTo(0));
        assertThat(sorter.compare(TEST1, TEST2), lessThan(0));
        assertThat(sorter.compare(TEST1, TEST3), lessThan(0));
        assertThat(sorter.compare(TEST1, TEST4), lessThan(0));
        assertThat(sorter.compare(TEST1, TEST5), lessThan(0));
        assertThat(sorter.compare(TEST1, TEST6), lessThan(0));
        assertThat(sorter.compare(TEST2, TEST1), greaterThan(0));
        assertThat(sorter.compare(TEST2, TEST3), lessThan(0));
        assertThat(sorter.compare(TEST2, TEST4), lessThan(0));
        assertThat(sorter.compare(TEST2, TEST5), lessThan(0));
        assertThat(sorter.compare(TEST2, TEST6), lessThan(0));
        assertThat(sorter.compare(TEST3, TEST1), greaterThan(0));
        assertThat(sorter.compare(TEST3, TEST2), greaterThan(0));
        assertThat(sorter.compare(TEST3, TEST4), lessThan(0));
        assertThat(sorter.compare(TEST3, TEST5), lessThan(0));
        assertThat(sorter.compare(TEST3, TEST6), lessThan(0));
        assertThat(sorter.compare(TEST4, TEST1), greaterThan(0));
        assertThat(sorter.compare(TEST4, TEST2), greaterThan(0));
        assertThat(sorter.compare(TEST4, TEST3), greaterThan(0));
        assertThat(sorter.compare(TEST4, TEST5), lessThan(0));
        assertThat(sorter.compare(TEST4, TEST6), lessThan(0));
        assertThat(sorter.compare(TEST5, TEST1), greaterThan(0));
        assertThat(sorter.compare(TEST5, TEST2), greaterThan(0));
        assertThat(sorter.compare(TEST5, TEST3), greaterThan(0));
        assertThat(sorter.compare(TEST5, TEST4), greaterThan(0));
        assertThat(sorter.compare(TEST5, TEST6), lessThan(0));
        assertThat(sorter.compare(TEST6, TEST1), greaterThan(0));
        assertThat(sorter.compare(TEST6, TEST2), greaterThan(0));
        assertThat(sorter.compare(TEST6, TEST3), greaterThan(0));
        assertThat(sorter.compare(TEST6, TEST4), greaterThan(0));
        assertThat(sorter.compare(TEST6, TEST5), greaterThan(0));
        var tuples = new ArrayList<String[]>(7);
        tuples.add(TEST6);
        tuples.add(TEST5);
        tuples.add(TEST4);
        tuples.add(TEST3);
        tuples.add(TEST2);
        tuples.add(TEST2a);
        tuples.add(TEST1);
        Collections.sort(tuples, sorter);
        assertThat(tuples, contains(TEST1, TEST2, TEST2, TEST3, TEST4, TEST5, TEST6));
    }

}
