/**
 *
 */
package org.theseed.stats;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.counters.CountMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Bruce Parrello
 *
 */
class TestShuffler {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TestShuffler.class);

    public static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Test
    void testSelectPart() {
        // Build an alphabet list.
        Collection<String> alphaList = IntStream.range(0, ALPHABET.length()).mapToObj(i -> ALPHABET.substring(i, i+1)).collect(Collectors.toList());
        // Test the vacuous case.
        Collection<String> subList = Shuffler.selectPart(alphaList, 100);
        assertThat(subList, equalTo(alphaList));
        // Create a count map for the letters.
        CountMap<String> counters = new CountMap<String>();
        // Create 1000 subsets, validating each one.
        for (int i = 0; i < 1000; i++) {
            subList = Shuffler.selectPart(alphaList, 10);
            // Create a name for this subset.
            String name = "#" + i + StringUtils.join(subList, "");
            assertThat(name, subList.size(), equalTo(10));
            for (String element : subList) {
                assertThat(name, element, in(alphaList));
                counters.count(element);
            }
        }
        // Now show the counts.
        for (var count : counters.sortedCounts())
            log.info("Count for \"{}\" is {}.", count.getKey(), count.getCount());
    }

}
