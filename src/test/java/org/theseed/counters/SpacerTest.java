/**
 *
 */
package org.theseed.counters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bruce Parrello
 *
 */
class SpacerTest {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SpacerTest.class);


    @Test
    void test() {
        // We create lists of different sizes and use different counts.
        for (int size = 10; size < 100; size += 3) {
            List<String> testList = IntStream.range(0, size).mapToObj(i -> String.format("I%d", i)).collect(Collectors.toList());
            for (int count = 2; count <= size; count++) {
                var iter = new Spacer<String>(testList, count);
                String last = "";
                int found = 0;
                // Logging gives us an idea how we are doing.
                StringBuilder buffer = new StringBuilder(count * 8);
                while (iter.hasNext()) {
                    String message = String.format("Count %d and size %d, item %d.", count, size, found);
                    String curr = iter.next();
                    assertThat(message, curr, not(equalTo(last)));
                    last = curr;
                    buffer.append(' ').append(curr);
                    found++;
                }
                assertThat(String.format("Count %d and size %d.", count, size), found, equalTo(count));
                log.info("Count {} and size {}: {}", count, size, buffer.toString());
            }
        }
    }

}
