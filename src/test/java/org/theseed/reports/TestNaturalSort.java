/**
 *
 */
package org.theseed.reports;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.theseed.stats.Shuffler;

/**
 * @author Bruce Parrello
 *
 */
public class TestNaturalSort {

    /**
     * test the natural sort
     */
    @Test
    public void testSort() {
        NaturalSort comparator = new NaturalSort();
        assertThat(comparator.compare("abs1_10", "abs1_2"), greaterThan(0));
        assertThat(comparator.compare("abc1_10", "abs1_2"), lessThan(0));
        assertThat(comparator.compare("fig|8333.2.peg,12", "fig|8333.10.peg.10"), lessThan(0));
        assertThat(comparator.compare("NODE_10_covg_32", "NODE_2_covg_12"), greaterThan(0));
        assertThat(comparator.compare("abcde", "abcee"), lessThan(0));
        assertThat(comparator.compare("abcdf", "abcd"), greaterThan(0));
        assertThat(comparator.compare("abc10", "abc10"), equalTo(0));
        assertThat(comparator.compare("abc10", "abc100"), lessThan(0));
        assertThat(comparator.compare("abc10", "aBc10"), lessThan(0));
        assertThat(comparator.compare("abc10", "aAc10"), greaterThan(0));
        String[] sortArray = new String[] { "abcdefg1", "absdefg2", "abcdefg2", "abcdefg10", "abcdefg20", "abs1_10", "abs1_2",
                "1004", "", "abcdef", "200" };
        Shuffler<String> sortTest = new Shuffler<String>(Arrays.asList(sortArray));
        sortTest.shuffle(10);
        sortTest.sort(comparator);
        assertThat(sortTest, contains("", "200", "1004", "abcdef", "abcdefg1", "abcdefg2", "abcdefg10",
                "abcdefg20", "abs1_2", "abs1_10", "absdefg2"));
    }

}
