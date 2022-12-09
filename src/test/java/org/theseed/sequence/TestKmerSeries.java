/**
 *
 */
package org.theseed.sequence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestKmerSeries {

    private static final List<String> TEST_LIST = List.of("aaatttcccgggaattccggatcg", "acgt", "acgttgcacg");

    @Test
    void testKmerSeries() {
        KmerSeries kSeries = new KmerSeries(TEST_LIST, 9);
        List<String> testSet = new ArrayList<String>(kSeries.size());
        for (String kmer : kSeries)
            testSet.add(kmer);
        assertThat(kSeries.size(), equalTo(testSet.size()));
        assertThat(testSet, containsInAnyOrder("aaatttccc", "aatttcccg", "atttcccgg", "tttcccggg", "ttcccggga",
                "tcccgggaa", "cccgggaat", "ccgggaatt", "cgggaattc", "gggaattcc", "ggaattccg", "gaattccgg",
                "aattccgga", "attccggat", "ttccggatc", "tccggatcg", "acgttgcac", "cgttgcacg"));
    }

}
