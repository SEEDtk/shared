/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.theseed.test.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestFeatureCategories {

    @Test
    public void testCategories() throws IOException {
        Genome testGenome = new Genome(new File("data", "1002870.3.gto"));
        Feature feat = testGenome.getFeature("fig|1002870.3.rna.3");
        assertThat(FeatureCategory.HYPOTHETICAL.qualifies(feat), isFalse());
        assertThat(FeatureCategory.KNOWN.qualifies(feat), isTrue());
        assertThat(FeatureCategory.IN_SUBSYSTEM.qualifies(feat), isFalse());
        assertThat(FeatureCategory.OUT_SUBSYSTEM.qualifies(feat), isTrue());
        feat = testGenome.getFeature("fig|1002870.3.peg.301");
        assertThat(FeatureCategory.HYPOTHETICAL.qualifies(feat), isTrue());
        assertThat(FeatureCategory.KNOWN.qualifies(feat), isFalse());
        assertThat(FeatureCategory.IN_SUBSYSTEM.qualifies(feat), isFalse());
        assertThat(FeatureCategory.OUT_SUBSYSTEM.qualifies(feat), isTrue());
        feat = testGenome.getFeature("fig|1002870.3.peg.2031");
        assertThat(FeatureCategory.HYPOTHETICAL.qualifies(feat), isTrue());
        assertThat(FeatureCategory.KNOWN.qualifies(feat), isFalse());
        assertThat(FeatureCategory.IN_SUBSYSTEM.qualifies(feat), isFalse());
        assertThat(FeatureCategory.OUT_SUBSYSTEM.qualifies(feat), isTrue());
        feat = testGenome.getFeature("fig|1002870.3.peg.269");
        assertThat(FeatureCategory.HYPOTHETICAL.qualifies(feat), isTrue());
        assertThat(FeatureCategory.KNOWN.qualifies(feat), isFalse());
        assertThat(FeatureCategory.IN_SUBSYSTEM.qualifies(feat), isFalse());
        assertThat(FeatureCategory.OUT_SUBSYSTEM.qualifies(feat), isTrue());
        feat = testGenome.getFeature("fig|1002870.3.peg.2502");
        assertThat(FeatureCategory.HYPOTHETICAL.qualifies(feat), isFalse());
        assertThat(FeatureCategory.KNOWN.qualifies(feat), isTrue());
        assertThat(FeatureCategory.IN_SUBSYSTEM.qualifies(feat), isTrue());
        assertThat(FeatureCategory.OUT_SUBSYSTEM.qualifies(feat), isFalse());
        feat = testGenome.getFeature("fig|1002870.3.peg.1008");
        assertThat(FeatureCategory.HYPOTHETICAL.qualifies(feat), isFalse());
        assertThat(FeatureCategory.KNOWN.qualifies(feat), isTrue());
        assertThat(FeatureCategory.IN_SUBSYSTEM.qualifies(feat), isTrue());
        assertThat(FeatureCategory.OUT_SUBSYSTEM.qualifies(feat), isFalse());
        feat = testGenome.getFeature("fig|1002870.3.peg.2932");
        assertThat(FeatureCategory.HYPOTHETICAL.qualifies(feat), isFalse());
        assertThat(FeatureCategory.KNOWN.qualifies(feat), isTrue());
        assertThat(FeatureCategory.IN_SUBSYSTEM.qualifies(feat), isTrue());
        assertThat(FeatureCategory.OUT_SUBSYSTEM.qualifies(feat), isFalse());
    }

}
