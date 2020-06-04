/**
 *
 */
package org.theseed.shared;

import java.io.File;
import java.io.IOException;

import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.locations.Location;
import org.theseed.locations.OrfLocation;
import org.theseed.proteins.DnaTranslator;

import junit.framework.TestCase;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Bruce Parrello
 *
 */
public class TestOrfLocations extends TestCase {

    /**
     * test orf-location processing in a genome
     *
     * @throws IOException
     */
    public void testGenomeOrfs() throws IOException {
        Genome gto = new Genome(new File("src/test/gto_test", "1313.7001.gto"));
        // Loop through the pegs.  We have to restrict ourselves to pegs without errors in them.
        String[] pegs = new String[] { "fig|1313.7001.peg.68", "fig|1313.7001.peg.147", "fig|1313.7001.peg.1682",
                "fig|1313.7001.peg.1680", "fig|1313.7001.peg.16", "fig|1313.7001.peg.1200" };
        for (String pegId : pegs) {
            Feature peg = gto.getFeature(pegId);
            Location pegLoc = peg.getLocation();
            OrfLocation orfLoc = pegLoc.createORF(gto);
            // Insure we have the same stop and the same translation.
            assertThat(pegId, orfLoc.orfStop(), equalTo(pegLoc.getEnd()));
            assertThat(pegId, orfLoc.pegTranslate(orfLoc.originalBegin()), equalTo(peg.getProteinTranslation()));
        }
        Feature peg = gto.getFeature("fig|1313.7001.peg.4");
        OrfLocation orfLoc = peg.getLocation().createORF(gto);
        String sequence = gto.getContig("1313.7001.con.0002").getSequence();
        DnaTranslator xlator = new DnaTranslator(11);
        int pos = 0;
        while (orfLoc.hasNext()) {
            pos = orfLoc.next();
            String label = String.format("pos%d", pos);
            assertThat(label, orfLoc.isStart(), equalTo(xlator.isStart(sequence, pos)));
            assertThat(label, pos, lessThan(1041));
        }
        assertThat(pos, equalTo(1039));
    }
}
