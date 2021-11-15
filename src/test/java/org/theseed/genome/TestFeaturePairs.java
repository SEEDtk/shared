/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestFeaturePairs {

    @Test
    void testFeaturePairs() {
        String pair = Feature.pairKey("fig|83333.1.peg.160", "fig|83333.1.peg.1736");
        assertThat(pair, equalTo("fig|83333.1.peg:160/1736"));
        pair = Feature.pairKey("fig|83333.1.peg.1736", "fig|83333.1.peg.160");
        assertThat(pair, equalTo("fig|83333.1.peg:160/1736"));
        pair = Feature.pairKey("fig|102266.1.peg.200", "fig|83333.1.peg.100");
        assertThat(pair, equalTo("fig:102266.1.peg.200/83333.1.peg.100"));
        pair = Feature.pairKey("fig|83333.1.peg.100", "fig|102266.1.peg.200");
        assertThat(pair, equalTo("fig:102266.1.peg.200/83333.1.peg.100"));
        pair = Feature.pairKey("fig|83333.1.rna.4", "fig|83333.1.peg.4");
        assertThat(pair, equalTo("fig|83333.1:peg.4/rna.4"));
        pair = Feature.pairKey("fig|83333.1.peg.4", "fig|83333.1.rna.4");
        assertThat(pair, equalTo("fig|83333.1:peg.4/rna.4"));
        pair = Feature.pairKey("fig|83333.4.peg.4", "fig|83333.1.rna.4");
        assertThat(pair, equalTo("fig|83333:1.rna.4/4.peg.4"));
    }

}
