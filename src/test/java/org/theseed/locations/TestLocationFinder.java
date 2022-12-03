/**
 *
 */
package org.theseed.locations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;

/**
 * @author Bruce Parrello
 *
 */
class TestLocationFinder {

    @Test
    void testLocationFinder() throws IOException {
        // Simple test of a genome finding its own features.
        File gFile = new File("data", "1262806.3.gto");
        Genome genome = new Genome(gFile);
        LocationFinder finder = new LocationFinder(genome);
        Set<Feature> found;
        for (Feature feat : genome.getFeatures()) {
            String fid = feat.getId();
            found = finder.getFeatures(feat.getLocation());
            assertThat(fid, found, hasItem(feat));
            Location loc = feat.getLocation();
            for (Feature feat2 : found) {
                Location loc2 = feat2.getLocation();
                assertThat(loc.toString() + " vs. " + loc2.toString(), loc.isOverlapping(loc2));
            }
        }
        Location loc = Location.create("1262806.3.con.0006", 16000, 15000);
        found = finder.getFeatures(loc);
        List<String> fids = found.stream().map(x -> x.getId()).collect(Collectors.toList());
        assertThat(fids, containsInAnyOrder("fig|1262806.3.peg.125", "fig|1262806.3.peg.126", "fig|1262806.3.peg.124"));
    }

}
