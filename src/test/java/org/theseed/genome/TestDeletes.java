/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.theseed.locations.Location;

/**
 * @author Bruce Parrello
 *
 */
class TestDeletes {



    @Test
    void testDeletes() throws IOException {
        File gFile = new File("data", "test.good.gto");
        Genome testGenome = new Genome(gFile);
        // Delete a safe feature.
        String fid = "fig|1381080.25.peg.1111";
        Feature feat = testGenome.getFeature(fid);
        boolean ok = testGenome.deleteFeature(feat);
        assertThat(ok, equalTo(true));
        // Verify that the feature is gone.
        feat = testGenome.getFeature(fid);
        assertThat(feat, nullValue());
        // Verify that it is not in any subsystems.
        for (SubsystemRow row : testGenome.getSubsystems()) {
            String subName = row.getName();
            for (SubsystemRow.Role role : row.getRoles())
                assertThat(role.getName() + " in " + subName, role.isUsed(fid), equalTo(false));
        }
        // Verify it is not in any couplings.
        for (Feature f : testGenome.getFeatures()) {
            for (Coupling coupling : f.getCouplings())
                assertThat("Coupling to " + f.getId(), coupling.getTarget(), not(equalTo(fid)));
        }
        // Delete an unsafe feature.
        fid = "fig|1381080.25.peg.714";
        feat = testGenome.getFeature(fid);
        ok = testGenome.deleteFeature(feat);
        assertThat(ok, equalTo(false));
        // Reload the genome and delete a safe contig.
        testGenome = new Genome(gFile);
        String cid = "NODE_698_length_13397_cov_5.64481";
        Contig contig = testGenome.getContig(cid);
        ok = testGenome.deleteContig(contig);
        assertThat(ok, equalTo(true));
        // Verify that the contig is gone.
        contig = testGenome.getContig(cid);
        assertThat(contig, nullValue());
        // Verify that no features are on the contig.
        for (Feature f : testGenome.getFeatures()) {
            // Assure the feature is not on the contig.
            Location loc = f.getLocation();
            assertThat(f.getId(), loc.getContigId(), not(equalTo(cid)));
            // Assure no coupled features are on the contig.
            for (Coupling coupling : f.getCouplings()) {
                fid = coupling.getTarget();
                Feature f2 = testGenome.getFeature(fid);
                assertThat(f.getId() + " to " + fid, f2.getLocation().getContigId(), not(equalTo(cid)));
            }
        }
        // Verify that the contig's features are purged from subsystems.
        for (SubsystemRow row : testGenome.getSubsystems()) {
            String subName = row.getName();
            for (SubsystemRow.Role role : row.getRoles()) {
                for (Feature f : role.getFeatures())
                    assertThat(f.getId() + " in " + subName, f.getLocation().getContigId(), not(equalTo(cid)));
            }
        }
        // Test an unsafe contig.
        cid = "NODE_89_length_118793_cov_279.317";
        contig = testGenome.getContig(cid);
        ok = testGenome.deleteContig(contig);
        assertThat(ok, equalTo(false));
    }

}
