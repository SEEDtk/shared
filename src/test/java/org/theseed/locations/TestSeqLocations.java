/**
 *
 */
package org.theseed.locations;

import java.io.File;
import java.io.IOException;

import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.proteins.CodonSet;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * @author Bruce Parrello
 *
 */
public class TestSeqLocations {

    @Test
    public void testPegLocations() throws IOException {
        // This tests the reverse-complementing and conversing.
        Genome gto = new Genome(new File("data/gto_test", "1313.7001.gto"));
        // Loop through the pegs.  We have to restrict ourselves to pegs without errors in them.
        String[] pegs = new String[] { "fig|1313.7001.peg.68", "fig|1313.7001.peg.147", "fig|1313.7001.peg.1682",
                "fig|1313.7001.peg.1680", "fig|1313.7001.peg.16", "fig|1313.7001.peg.1200" };
        for (String pegId : pegs) {
            Feature peg = gto.getFeature(pegId);
            Location pegLoc = peg.getLocation();
            SequenceLocation seqLoc = pegLoc.createSequenceLocation(gto);
            int pos = seqLoc.setRelativePosition(1);
            assertThat(seqLoc.realLocation(pos, pos + pegLoc.getLength() - 1), equalTo(pegLoc));
            assertThat(seqLoc.getRealPosition(), equalTo(pegLoc.getBegin()));
            String xlate = seqLoc.pegTranslate(pos);
            assertThat(pegId, xlate, equalTo(peg.getProteinTranslation()));
        }
        // Now we need to test some basic properties.
        Location testLoc = gto.getFeature("fig|1313.7001.peg.4").getLocation();
        SequenceLocation testSeqLoc = testLoc.createSequenceLocation(gto);
        assertThat(testSeqLoc.getContigId(), equalTo("1313.7001.con.0002"));
        assertThat(testSeqLoc.getSeqLen(), equalTo(1043));
        assertThat(testSeqLoc.getEndPosition(), equalTo(1041));
        assertThat(testSeqLoc.endString(), equalTo("1313.7001.con.0002+1041"));
        // Test the iteration.
        CodonSet fCodons = new CodonSet("ttt", "ttc");
        assertThat(testSeqLoc.first(fCodons, 1), equalTo(37));
        assertThat(testSeqLoc.getPosition(), equalTo(37));
        assertThat(testSeqLoc.positionString(), equalTo("1313.7001.con.0002+37"));
        assertThat(testSeqLoc.next(), equalTo(187));
        assertThat(testSeqLoc.next(), equalTo(262));
        assertThat(testSeqLoc.next(), equalTo(343));
        assertThat(testSeqLoc.next(), equalTo(466));
        assertThat(testSeqLoc.next(), equalTo(796));
        assertThat(testSeqLoc.next(), equalTo(805));
        assertThat(testSeqLoc.next(), equalTo(916));
        assertThat(testSeqLoc.next(), equalTo(0));
        assertThat(testSeqLoc.next(), equalTo(0));
        assertThat(testSeqLoc.getBegin(testLoc), equalTo(1));
        // Test basic properties for backward locations.
        testLoc = gto.getFeature("fig|1313.7001.peg.1156").getLocation();
        testSeqLoc = testLoc.createSequenceLocation(gto);
        assertThat(testSeqLoc.getContigId(), equalTo("1313.7001.con.0024"));
        assertThat(testSeqLoc.getSeqLen(), equalTo(5602));
        assertThat(testSeqLoc.getEndPosition(), equalTo(1793));
        assertThat(testSeqLoc.endString(), equalTo("1313.7001.con.0024-3810"));
        // Test the iteration for backward locations.
        assertThat(testSeqLoc.first(fCodons, 1), equalTo(1269));
        assertThat(testSeqLoc.getPosition(), equalTo(1269));
        assertThat(testSeqLoc.getRealPosition(), equalTo(4334));
        assertThat("F codon check", testSeqLoc.isCodon(fCodons, 1269));
        assertThat(testSeqLoc.positionString(), equalTo("1313.7001.con.0024-4334"));
        assertThat(testSeqLoc.pegTranslate(1269), equalTo("FQTFISRHNSNFFSDKLVLTSVTPASSAPVLQTPKATSSTLYFDSLTVNAGNGGFLHCIQMDTSVNAANQVVSVGADIAFDADPKFFACLVRFESSSVPTTLPTAYDVYPLDGRHDGGYYTVKDCVTIDVLPRTPGNNVYVGFMVWSNFTATKCRGLVSLNQVIKEIICLQPLK"));
        assertThat(testSeqLoc.next(), equalTo(1278));
        assertThat(testSeqLoc.pegTranslate(1278), equalTo("FISRHNSNFFSDKLVLTSVTPASSAPVLQTPKATSSTLYFDSLTVNAGNGGFLHCIQMDTSVNAANQVVSVGADIAFDADPKFFACLVRFESSSVPTTLPTAYDVYPLDGRHDGGYYTVKDCVTIDVLPRTPGNNVYVGFMVWSNFTATKCRGLVSLNQVIKEIICLQPLK"));
        assertThat(testSeqLoc.next(), equalTo(1302));
        assertThat(testSeqLoc.next(), equalTo(1305));
        assertThat(testSeqLoc.next(), equalTo(1395));
        assertThat(testSeqLoc.next(), equalTo(1431));
        assertThat(testSeqLoc.getBegin(testLoc), equalTo(1266));
        char[] neighbors = testSeqLoc.getNeighborhood(20, 10);
        assertThat(new String(neighbors), equalTo("ttaatgctggtaatggtggttttcttcattg"));
        neighbors = testSeqLoc.getNeighborhood(1421, 20, 10);
        assertThat(new String(neighbors), equalTo("agtttgacggttaatgctggtaatggtggtt"));
        testSeqLoc.setRelativePosition(4);
        assertThat(testSeqLoc.getPosition(), equalTo(1269));
        assertThat(testSeqLoc.getRealPosition(), equalTo(4334));
        assertThat("F codon check 2", testSeqLoc.isCodon(fCodons, 1269));
        assertThat("F codon check 3", ! testSeqLoc.isCodon(fCodons, 1272));
    }

    /**
     * test ORF extension
     * @throws IOException
     */
    @Test
    public void testOrfHandling() throws IOException {
        Genome gto = new Genome(new File("data/gto_test", "1313.7001.gto"));
        Location loc = Location.create("1313.7001.con.0024", 20, 31);
        Location loc2 = loc.extendToOrf(gto);
        assertThat(loc2.getBegin(), equalTo(2));
        assertThat(loc2.getEnd(), equalTo(37));
        loc = Location.create("1313.7001.con.0024", 44, 52);
        loc2 = loc.extendToOrf(gto);
        assertThat(loc2.getBegin(), equalTo(41));
        assertThat(loc2.getEnd(), equalTo(112));
        loc = Location.create("1313.7001.con.0024", 5321, 5350);
        loc2 = loc.extendToOrf(gto);
        assertThat(loc2.getBegin(), equalTo(5069));
        assertThat(loc2.getEnd(), equalTo(5602));
        loc = Location.create("1313.7001.con.0024", 5599, 5548).extendToOrf(gto);
        assertThat(loc.getBegin(), equalTo(5602));
        assertThat(loc.getEnd(), equalTo(5539));
        loc = Location.create("1313.7001.con.0024", 345, 12).extendToOrf(gto);
        assertThat(loc.getBegin(), equalTo(348));
        assertThat(loc.getEnd(), equalTo(3));
        loc = Location.create("1313.7001.con.0024", 2811, 1219).extendToOrf(gto);
        assertThat(loc.getBegin(), equalTo(2814));
        assertThat(loc.getEnd(), equalTo(1213));
    }

}
