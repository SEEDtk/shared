/**
 *
 */
package org.theseed.proteins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;

/**
 * @author Bruce Parrello
 *
 */
class TestFamilyType {

    @Test
    void testPfams() throws IOException {
        Genome testGenome = new Genome(new File("data", "1035377.13.gto"));
        Feature feat = testGenome.getFeature("fig|1035377.13.peg.3");
        assertThat(FamilyType.PGFAM.getFamily(feat), equalTo("PGF_04060585"));
        assertThat(FamilyType.PLFAM.getFamily(feat), equalTo("PLF_629_00003984"));
        feat = testGenome.getFeature("fig|1035377.13.peg.2");
        assertThat(FamilyType.PGFAM.getFamily(feat), nullValue());
        assertThat(FamilyType.PLFAM.getFamily(feat), equalTo("FIG01429041"));
    }

}
