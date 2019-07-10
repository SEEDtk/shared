/**
 *
 */
package org.theseed.shared;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.theseed.counters.CountMap;
import org.theseed.counters.KeyPair;
import org.theseed.counters.PairCounter;
import org.theseed.counters.QualityCountMap;
import org.theseed.genomes.Contig;
import org.theseed.genomes.Feature;
import org.theseed.genomes.FeatureList;
import org.theseed.genomes.Genome;
import org.theseed.genomes.GenomeDirectory;
import org.theseed.locations.FLocation;
import org.theseed.locations.Frame;
import org.theseed.locations.Location;
import org.theseed.locations.LocationList;
import org.theseed.locations.Region;
import org.theseed.magic.MagicMap;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;

import junit.framework.TestCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Bruce Parrello
 *
 */
public class TestLibrary extends TestCase {

    /**
     * @param name
     */
    public TestLibrary(String name) throws IOException {
        super(name);
        this.myGto = new Genome("src/test/gto_test/1313.7001.gto");
    }

    private Genome myGto = null;

    /**
     * Test magic IDs.
     * @throws IOException
     */
    public void testMagic() throws IOException {
        File inFile = new File("src/test", "words.txt");
        Scanner thingScanner = new Scanner(inFile);
        thingScanner.useDelimiter("\t|\r\n|\n");
        while (thingScanner.hasNext()) {
            String condensed = thingScanner.next();
            String full = thingScanner.next();
            assertEquals("String did not condense.", condensed, MagicMap.condense(full));
        }
        thingScanner.close();
        // Test registration
        ThingMap magicTable = new ThingMap();
        inFile = new File("src/test", "things.txt");
        thingScanner = new Scanner(inFile);
        thingScanner.useDelimiter("\t|\r\n|\n");
        while (thingScanner.hasNext()) {
            String thingId = thingScanner.next();
            thingScanner.next();
            assertNull("Wrong ID found", magicTable.get(thingId));
            String thingDesc = thingScanner.next();
            Thing newThing = new Thing(thingId, thingDesc);
            magicTable.register(newThing);
            assertEquals("Registered ID did not read back.", thingDesc, magicTable.getName(thingId));
        }
        thingScanner.close();
        assertTrue("PheS not found.", magicTable.containsKey("PhenTrnaSyntAlph"));
        assertFalse("Known bad key found.", magicTable.containsKey("PhenTrnaSyntGamm"));
        String modifiedThing = "3-keto-L-gulonate-6-phosphate decarboxylase UlaK putative (L-ascorbate utilization protein D) (EC 4.1.1.85)";
        Thing newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for modified thing.", "3KetoLGulo6PhosDeca6", newThing.getId());
        assertSame("Modified thing did not read back.", newThing, magicTable.get("3KetoLGulo6PhosDeca6"));
        modifiedThing = "Unique (new) thing string without numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for unique thing.", "UniqThinStriWith", newThing.getId());
        assertSame("Unique thing did not read back.", newThing, magicTable.get("UniqThinStriWith"));
        Thing findThing = magicTable.findOrInsert(modifiedThing);
        assertSame("Unique thing was re-inserted.", newThing, findThing);
        modifiedThing = "Unique (old) thing string without numbers";
        findThing = magicTable.findOrInsert(modifiedThing);
        assertTrue("Parenthetical did not change thing ID.", findThing != newThing);
        assertEquals("Wrong ID assigned for parenthetical thing.", "UniqThinStriWith2", findThing.getId());
        assertSame("Parenthetical thing did not read back.", findThing, magicTable.get("UniqThinStriWith2"));
        modifiedThing = "Unique (newer) thing string without numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for newer thing.", "UniqThinStriWith3", newThing.getId());
        assertSame("Parenthetical thing did not read back.", newThing, magicTable.get("UniqThinStriWith3"));
        modifiedThing = "Unique thing string 12345 with numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Name not stored in thing.", modifiedThing, newThing.getName());
        assertEquals("Wrong ID assigned for numbered thing.", "UniqThinStri1234n1", newThing.getId());
        modifiedThing = "Unique thing string 12345 with more numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for second numbered thing.", "UniqThinStri1234n2", newThing.getId());
        // Test save and load.
        File saveFile = new File("src/test", "things.ser");
        magicTable.save(saveFile);
        ThingMap newTable = ThingMap.load(saveFile);
        for (Thing oldThing : magicTable.values()) {
            newThing = newTable.get(oldThing.getId());
            assertNotNull("Could not find thing in loaded table.", newThing);
            assertEquals("Loaded table has wrong thing name.", newThing.getName(), oldThing.getName());
            assertEquals("Loaded thing has wrong checksum.", newThing, oldThing);
            newThing = newTable.getByName(oldThing.getName());
            assertNotNull("Could not find thing by name in loaded table.", newThing);
            assertEquals("Found incorrect object by name in loaded table.", oldThing.getId(), newThing.getId());
        }
    }

    public void testCounts() {
        QualityCountMap<String> testMap = new QualityCountMap<String>();
        testMap.setGood("AAA");
        testMap.setGood("AAA");
        testMap.setGood("AAA");
        testMap.setGood("AAA");
        testMap.setBad("AAA");
        assertEquals("Incorrect fraction good for AAA.", 0.8, testMap.fractionGood("AAA"));
        testMap.setBad("BBB");
        assertEquals("Incorrect bad count for BBB.", 1, testMap.bad("BBB"));
        testMap.setGood("CCC");
        assertEquals("Incorrect good count for BBB.", 1, testMap.good("CCC"));
        testMap.setGood("DDD");
        testMap.setBad("DDD");
        SortedSet<String> keys = testMap.keys();
        String key1 = keys.first();
        assertEquals("Incorrect smallest key.", "AAA", key1);
        assertEquals("Incorrect number of keys.", 4, keys.size());
        List<String> sorted = testMap.bestKeys();
        assertThat("Incorrect sort key results", sorted, contains("AAA", "CCC", "DDD", "BBB"));
    }

    private static final String myProtein = "MNERYQCLKTKEYQALLSSKGRQIFAKRKIDMKSVFGQIKVCLGYKRCHLRGKRQVRIDMGFILMANNLLKYNKRKRQN";
    private static final String myDna1 = "atgaatgaacgttaccagtgtttaaaaactaaagaatatcaggcacttttatcttccaagggtagacaaattttcgctaaacgtaagattgatatgaaatctgtctttgggcagataaaggtttgtttgggttataagagatgtcatctgagaggtaagcgtcaagtgagaattgacatgggattcatactcatggccaacaacctgctgaaatataataagagaaagaggcaaaattaa";
    private static final String myDna2 = "aaatagatttcaaaatgataaaaacgcatcctatcaggtttgagtgaacttgataggatgcgttttagaatgtcaaaattaattgagtttg";


    /**
     * Main test of genomes.
     * @throws FileNotFoundException
     * @throws JsonException
     */
    public void testGenome()
    {
        assertEquals("Genome ID not correct.", "1313.7001", this.myGto.getId());
        assertEquals("Genome name not correct.", "Streptococcus pneumoniae P210774-233", this.myGto.getName());
        assertNull("Nonexistent feature found.", this.myGto.getFeature("fig|1313.7001.cds.75"));
        // Now we need to pull out a PEG and ask about it.
        Feature myFeature = this.myGto.getFeature("fig|1313.7001.peg.758");
        assertNotNull("Sample feature not found.", myFeature);
        assertEquals("Incorrect feature found.", "fig|1313.7001.peg.758", myFeature.getId());
        assertEquals("Incorrect function in sample feature.", "Transposase, IS4 family", myFeature.getFunction());
        assertEquals("Incorrect protein for sample feature.", myProtein, myFeature.getProteinTranslation());
        assertEquals("Incorrect DNA for sample feature.", myDna1, this.myGto.getDna("fig|1313.7001.peg.758"));
        // Next the location.
        Location myLoc = myFeature.getLocation();
        assertEquals("Incorrect contig for feature.", "1313.7001.con.0017", myLoc.getContigId());
        assertEquals("Incorrect left for feature.", 30698, myLoc.getLeft());
        assertEquals("Incorrect right for feature", 30937, myLoc.getRight());
        assertEquals("Incorrect begin for feature.", 30937, myLoc.getBegin());
        assertEquals("Incorrect length for feature.", 240, myLoc.getLength());
        assertEquals("Incorrect strand for feature.", '-', myLoc.getDir());
        assertFalse("Segmentation flag failure.", myLoc.isSegmented());
        // Now we check a segmented location.
        myFeature = this.myGto.getFeature("fig|1313.7001.repeat_unit.238");
        assertEquals("Incorrect DNA for segmented feature.", myDna2, this.myGto.getDna(myFeature.getId()));
        myLoc = myFeature.getLocation();
        assertEquals("Incorrect contig for segmented location.", "1313.7001.con.0018", myLoc.getContigId());
        assertEquals("Incorrect left for segmented location.", 11908, myLoc.getLeft());
        assertEquals("Incorrect right for segmented location.", 12116, myLoc.getRight());
        assertEquals("Incorrect begin for feature.", 11908, myLoc.getBegin());
        assertEquals("Incorrect length for feature.", 209, myLoc.getLength());
        assertEquals("Incorrect strand for segmented location.", '+', myLoc.getDir());
        assertTrue("Segmentation flag failure.", myLoc.isSegmented());
        // Now iterate over the proteins.
        for (Feature feat : this.myGto.getPegs()) {
            assertEquals("Feature" + feat.getId() + " is not a PEG.", "CDS", feat.getType());
        }
    }

    /**
     * Test of plus-locations.
     */
    public void testPlusLocations() {
        Location plusLoc = Location.create("mySequence1", "+");
        plusLoc.addRegion(402, 500);
        assertFalse("Segmentation error on single segment.", plusLoc.isSegmented());
        assertTrue("Location does not default to valid.", plusLoc.isValid());
        assertEquals("Invalid begin in plusLoc.", 402, plusLoc.getBegin());
        plusLoc.addRegion(10, 200);
        assertEquals("Invalid left position in plusLoc.", 10, plusLoc.getLeft());
        assertEquals("Invalid right position in plusLoc.", 901, plusLoc.getRight());
        assertEquals("Invalid contig in plusLoc.", "mySequence1", plusLoc.getContigId());
        assertEquals("Invalid strand in plusLoc.", '+', plusLoc.getDir());
        assertEquals("Invalid length in plusLoc.", 892, plusLoc.getLength());
        assertEquals("Invalid begin in plusLoc after add.", 10, plusLoc.getBegin());
        plusLoc.putRegion(1, 6);
        assertEquals("Invalid left position in plusLoc after put.", 1, plusLoc.getLeft());
        assertEquals("Invalid right position in plusLoc after put.", 901, plusLoc.getRight());
        plusLoc.putRegion(250, 349);
        assertEquals("Invalid left position in plusLoc after internal put.", 1, plusLoc.getLeft());
        assertEquals("Invalid right position in plusLoc after internal put.", 901, plusLoc.getRight());
        assertTrue("Segmentation error on multiple segments.", plusLoc.isSegmented());
        plusLoc.invalidate();
        assertFalse("Location did not invalidate.", plusLoc.isValid());
        plusLoc.setLeft(260);
        Collection<Region> plusRegions = plusLoc.getRegions();
        for (Region region : plusRegions) {
            int left = region.getLeft();
            assertTrue("Invalid region " + region + " extends past 260.", left >= 260);
        }
        Location cloneLoc = (Location) plusLoc.clone();
        assertEquals("Clone contig does not match.", plusLoc.getContigId(), cloneLoc.getContigId());
        assertEquals("Clone direction does not match.", plusLoc.getDir(), cloneLoc.getDir());
        assertEquals("Clone left does not match.", plusLoc.getLeft(), cloneLoc.getLeft());
        assertEquals("Clone length does not match.", plusLoc.getLength(), cloneLoc.getLength());
        assertTrue("Segmentation error on clone.", cloneLoc.isSegmented());
        assertFalse("Validation error on clone.", cloneLoc.isValid());
        Collection<Region> cloneRegions = cloneLoc.getRegions();
        for (Region region : plusRegions) {
            assertTrue("Region " + region + " not found in clone.", region.containedIn(cloneRegions));
        }
    }

    /**
     * Test of minus-locations.
     */
    public void testMinusLocations() {
        Location minusLoc = Location.create("mySequence1", "-");
        minusLoc.addRegion(901, 500);
        assertFalse("Segmentation error on single segment.", minusLoc.isSegmented());
        assertTrue("Location does not default to valid.", minusLoc.isValid());
        assertEquals("Invalid begin in minusLoc.", 901, minusLoc.getBegin());
        minusLoc.addRegion(209, 200);
        assertEquals("Invalid left position in minusLoc.", 10, minusLoc.getLeft());
        assertEquals("Invalid right position in minusLoc.", 901, minusLoc.getRight());
        assertEquals("Invalid contig in minusLoc.", "mySequence1", minusLoc.getContigId());
        assertEquals("Invalid strand in minusLoc.", '-', minusLoc.getDir());
        assertEquals("Invalid length in minusLoc.", 892, minusLoc.getLength());
        assertEquals("Invalid begin in minusLoc after add.", 901, minusLoc.getBegin());
        minusLoc.putRegion(1, 6);
        assertEquals("Invalid left position in minusLoc after put.", 1, minusLoc.getLeft());
        assertEquals("Invalid right position in minusLoc after put.", 901, minusLoc.getRight());
        minusLoc.putRegion(250, 349);
        assertEquals("Invalid left position in minusLoc after internal put.", 1, minusLoc.getLeft());
        assertEquals("Invalid right position in minusLoc after internal put.", 901, minusLoc.getRight());
        assertTrue("Segmentation error on multiple segments.", minusLoc.isSegmented());
        minusLoc.invalidate();
        assertFalse("Location did not invalidate.", minusLoc.isValid());
        minusLoc.setRight(260);
        Collection<Region> minusRegions = minusLoc.getRegions();
        for (Region region : minusRegions) {
            int right = region.getRight();
            assertTrue("Invalid region extends to " + right + " past 260.", right <= 260);
        }
        Location cloneLoc = (Location) minusLoc.clone();
        assertEquals("Clone contig does not match.", minusLoc.getContigId(), cloneLoc.getContigId());
        assertEquals("Clone direction does not match.", minusLoc.getDir(), cloneLoc.getDir());
        assertEquals("Clone left does not match.", minusLoc.getLeft(), cloneLoc.getLeft());
        assertEquals("Clone length does not match.", minusLoc.getLength(), cloneLoc.getLength());
        assertTrue("Segmentation error on clone.", cloneLoc.isSegmented());
        assertFalse("Validation error on clone.", cloneLoc.isValid());
        Collection<Region> cloneRegions = cloneLoc.getRegions();
        for (Region region :minusRegions) {
            assertTrue("Region " + region + " not found in clone.", region.containedIn(cloneRegions));
        }
    }

    /**
     * Location comparison test
     */
    public void testLocations() {
        Region region1 = new Region(1000, 2000);
        Region region2 = new Region(1000, 2000);
        Region region3 = new Region(2000, 3000);
        assertTrue("region1 not equal to region 2.", region1.equals(region2));
        assertFalse("region1 equal to region 3.", region1.equals(region3));
        assertTrue("Region equals is not commutative.", region2.equals(region1));
        assertEquals("Equal regions have different hash codes.", region1.hashCode(), region2.hashCode());
        Location loc1 = Location.create("myContig", "+", 1000, 1999);
        Location loc2 = Location.create("myContig", "-", 1100, 1199);
        Location loc3 = Location.create("myContig", "-", 1150, 1249);
        Location loc4 = Location.create("yourContig", "-", 1150, 1249);
        assertTrue("loc1 is not less than loc2.", loc1.compareTo(loc2) < 0);
        assertTrue("loc2 is not less than loc3.", loc2.compareTo(loc3) < 0);
        assertTrue("loc1 does not contain loc2.", loc1.contains(loc2));
        assertFalse("loc2 contains loc3.", loc2.contains(loc3));
        assertFalse("loc3 contains loc2.", loc3.contains(loc2));
        assertFalse("loc1 contains loc4.", loc1.contains(loc4));
        Location loc5 = Location.create("myContig", "+",  1000, 2000, 3000, 4000);
        Location loc6 = Location.create("myContig", "+", 3000, 4000, 1000, 2000);
        Location loc7 = Location.create("yourContig", "+", 1000, 2000, 3000, 4000);
        Location loc8 = Location.create("myContig", "-", 1000, 2000, 3000, 4000);
        Location loc9 = Location.create("myContig",  "+",  1000, 1999, 3000, 4000);
        assertTrue("loc5 not equal to loc6.", loc5.equals(loc6));
        assertEquals("Equal locations have different hash codes.", loc5.hashCode(), loc6.hashCode());
        assertFalse("Different contigs compare equal.", loc5.equals(loc7));
        assertFalse("Different strands compare equal.", loc5.equals(loc8));
        assertFalse("Different region counts compare equal.", loc5.equals(loc1));
        assertFalse("Different region extents compare equal.", loc5.equals(loc9));
    }

    /**
     * Main location list test
     */
    public void testLocationList() {
        LocationList newList = new LocationList("myContig");
        Location[] locs = { Location.create("myContig", "+", 10, 99, 102, 199),
                            Location.create("myContig", "-", 100, 400),
                            Location.create("myContig", "-", 500, 999),
                            Location.create("myContig", "-", 3000, 3199),
                            Location.create("myContig", "+", 4000, 4999),
                            Location.create("myContig", "-", 4100, 4199),
                            Location.create("myContig", "-", 6000, 6199),
                            Location.create("myContig", "+", 5000, 5099),
                            Location.create("myContig", "-", 5000, 5099),
                            Location.create("myContig", "+", 5200, 5299),
                            Location.create("myContig", "-", 5250, 5299),
                            Location.create("myContig", "+", 5350, 5399),
                            Location.create("myContig", "-", 5300, 5399),
                            Location.create("myContig", "+", 6800, 7299),
                            Location.create("myContig", "+", 8000, 8199),
                            Location.create("myContig", "+", 8400, 8599),
                            Location.create("myContig", "-", 8100, 8449),
                            Location.create("myContig", "+", 9100, 9199),
                            Location.create("myContig", "+", 9200, 9299),
                            Location.create("myContig", "-", 9300, 9399),
                            Location.create("myContig", "+", 9400, 9499),
                            Location.create("myContig", "-", 9000, 9999),
                          };
        // Add all these locations to the list.
        for (Location loc : locs) {
            assertTrue("Failed to add " + loc + " to list.", newList.addLocation(loc));
        }
        Location badLoc = Location.create("yourContig", "+", 1000, 4999);
        assertFalse("Added wrong contig successfully.", newList.addLocation(badLoc));
        assertEquals("Invalid contig ID in location list.", "myContig", newList.getContigId());
        // Now we need to verify that none of the stored locations overlap.
        Iterator<Location> iter = newList.iterator();
        Location prev = iter.next();
        while (iter.hasNext()) {
            Location next = iter.next();
            assertTrue(prev + " overlaps " + next, prev.getRight() < next.getLeft());
        }
        // Next, we want to show that every location in the location list is wholly
        // contained in one or more locations of the input list.  If it is contained
        // in more than one, or it is contained in a segmented one, it should be
        // invalid.  Otherwise, it should be on the same strand.
        for (Location loc : newList) {
            // This will remember the strand of the last location found.
            char strand = '0';
            // This will be set to TRUE if a location is segmented.
            boolean invalid = false;
            // This will count the locations found.
            int found = 0;
            for (Location loc2 : locs) {
                if (loc2.contains(loc)) {
                    strand = loc2.getDir();
                    if (loc2.isSegmented()) {
                        invalid = true;
                    }
                    found++;
                }
            }
            if (found > 1) {
                assertFalse(loc + " is valid but it is in " + found + " input locations.", loc.isValid());
            } else if (invalid) {
                assertFalse(loc + " is valid but it is in a segmented input location.", loc.isValid());
            } else if (found == 0) {
                fail(loc + " was not found in the input locations.");
            } else {
                assertEquals(loc + " is not on the correct strand.", strand, loc.getDir());
            }
        }
        // Now for each input location, we need to show that every position is in a location
        // of the location list.
        for (Location loc : locs) {
            for (int pos = loc.getLeft(); pos <= loc.getRight(); pos++) {
                assertTrue("Position " + pos + " of location " + loc + " is not in the list.",
                        newList.computeStrand(pos) != '0');
            }
        }
        // Finally, we want to test the frame computation.
        assertEquals("Invalid frame for pre-loc position.", Frame.XX, newList.computeRegionFrame(1, 15));
        assertEquals("Invalid frame for segmented position.", Frame.XX, newList.computeRegionFrame(40, 45));
        assertEquals("Invalid frame for simple minus position.", Frame.M1, newList.computeRegionFrame(390, 399));
        assertEquals("Invalid frame for simple plus position.", Frame.P0, newList.computeRegionFrame(4009, 4054));
        assertEquals("Invalid frame for near-overlap position.", Frame.P2, newList.computeRegionFrame(5235, 5245));
        assertEquals("Invalid frame for overlap position.", Frame.XX, newList.computeRegionFrame(5235, 5255));
        assertEquals("Invalid frame for extron position.", Frame.F0, newList.computeRegionFrame(7306, 7316));
    }

    /**
     * Basic test for location-list maps.
     */
    public void testContigMapping() {
        Map<String, LocationList> gList = LocationList.createGenomeCodingMap(this.myGto);
        LocationList contig0036 = gList.get("1313.7001.con.0036");
        assertNotNull("Contig 0036 not found.", contig0036);
        assertEquals("Incorrect strand found for test position 33996.", '+', contig0036.computeStrand(33996));
        assertEquals("Incorrect strand found for test position 30980.", '-', contig0036.computeStrand(30980));
        assertEquals("Incorrect strand found for test position 30984.", '0', contig0036.computeStrand(30984));
    }

    /**
     * Test genome directories
     *
     * @throws IOException
     */
    public void testGenomeDir() throws IOException {
        GenomeDirectory gDir = new GenomeDirectory("src/test/gto_test");
        assertEquals("Wrong number of genomes found.", 4, gDir.size());
        // Run through an iterator.  We know the genome IDs, we just need to find them in order.
        String[] expected = new String[] { "1005394.4", "1313.7001", "1313.7002", "1313.7016" };
        int i = 0;
        for (Genome genome : gDir) {
            assertEquals("Incorrect result for genome at position " + i + ".", expected[i], genome.getId());
            i++;
        }
    }

    /**
     * Test the feature-by-location sort
     * @throws IOException
     * @throws NumberFormatException
     */
    public void testContigFeatures() throws NumberFormatException, IOException {
        Collection<Contig> contigs = myGto.getContigs();
        int featureCount = 0;
        for (Contig contig : contigs) {
            FeatureList contigFeatures = myGto.getContigFeatures(contig.getId());
            featureCount += contigFeatures.size();
            Location previous = new FLocation(contig.getId());
            previous.addRegion(0, 0);
            for (Feature feat : contigFeatures) {
                Location current = feat.getLocation();
                assertEquals("Feature is on wrong contig.", contig.getId(), current.getContigId());
                assertTrue("Feature is out of order.", current.getBegin() >= previous.getBegin());
            }
        }
        assertEquals("Feature count incorrect.", myGto.getFeatureCount(), featureCount);
        Genome gto2 = new Genome("src/test/testLocs.gto");
        FeatureList contigFeatures = new FeatureList(gto2, "1313.7001.con.0029");
        Location region = Location.create("1313.7001.con.0029", "+", 160, 6860);
        Collection<Feature> inRegion = contigFeatures.inRegion(160, 6860);
        ArrayList<String> fids = new ArrayList<String>(inRegion.size());
        for (Feature feat : inRegion) {
            fids.add(feat.getId());
            assertEquals("Feature " + feat + " not in region.", -1, region.distance(feat.getLocation()));
        }
        assertThat("Not all expected features found.", fids,
                hasItems("fig|1313.7001.peg.1244", "fig|1313.7001.peg.1245", "fig|1313.7001.peg.1246",
                         "fig|1313.7001.peg.1249", "fig|1313.7001.peg.1250", "fig|1313.7001.peg.1251"));
        inRegion = contigFeatures.inRegion(0, 100);
        assertEquals("Error at left extreme.", 1, inRegion.size());
        inRegion = contigFeatures.inRegion(14000, 15000);
        assertEquals("Error at right extreme.", 0, inRegion.size());
        inRegion = contigFeatures.inRegion(12000, 15000);
        assertEquals("Count error at right edge.", 2, inRegion.size());
        fids.clear();
        for (Feature feat : inRegion) fids.add(feat.getId());
        assertThat("Incorrect feature found at right edge.", fids,
                containsInAnyOrder("fig|1313.7001.peg.1256", "fig|1313.7001.peg.1257"));
        contigFeatures = new FeatureList(gto2, "1313.7001.con.0003");
        inRegion = contigFeatures.inRegion(2000, 16000);
        assertEquals("Features found in empty contig.", 0, inRegion.size());
        contigFeatures = new FeatureList(gto2, "1313.7001.con.0025");
        inRegion = contigFeatures.inRegion(0, 2000);
        assertEquals("Features found in empty left edge.", 0, inRegion.size());

    }

    /**
     * Test the key pairs.
     */
    public void testKeyPair() {
        Thing t1 = new Thing("T1", "first thing");
        Thing t2 = new Thing("T2", "second thing");
        Thing t3 = new Thing("T3", "third thing");
        KeyPair<Thing> p12 = new KeyPair<Thing>(t1, t2);
        KeyPair<Thing> p21 = new KeyPair<Thing>(t2, t1);
        KeyPair<Thing> p13 = new KeyPair<Thing>(t1, t3);
        assertEquals("Equality is not commutative.", p12, p21);
        assertEquals("Hash codes are not commutative.", p12.hashCode(), p21.hashCode());
        assertFalse("Different things compare equal.", p13.equals(p12));
        assertSame("Wrong left value.", t1, p12.getLeft());
        assertSame("Wrong right value.", t2, p12.getRight());
    }

    /**
     * Test counting maps.
     */
    public void testCounters() {
        Thing t1 = new Thing("T1", "first thing");
        Thing t2 = new Thing("T2", "second thing");
        Thing t3 = new Thing("T3", "third thing");
        Thing t4 = new Thing("T4", "fourth thing");
        Thing t5 = new Thing("T5", "fifth thing");
        CountMap<Thing> thingCounter = new CountMap<Thing>();
        assertEquals("New thingcounter not empty (items).", 0, thingCounter.size());
        assertEquals("Thing 5 count not zero.", 0, thingCounter.getCount(t5));
        assertEquals("Asking about thing 5 added to map.", 0, thingCounter.size());
        thingCounter.count(t1);
        thingCounter.count(t4);
        thingCounter.count(t2);
        thingCounter.count(t3);
        thingCounter.count(t2);
        thingCounter.count(t3);
        thingCounter.count(t4);
        thingCounter.count(t3);
        thingCounter.count(t4, 2);
        assertEquals("Wrong count for thing 1.", 1, thingCounter.getCount(t1));
        assertEquals("Wrong count for thing 2.", 2, thingCounter.getCount(t2));
        assertEquals("Wrong count for thing 3.", 3, thingCounter.getCount(t3));
        assertEquals("Wrong count for thing 4.", 4, thingCounter.getCount(t4));
        assertEquals("Wrong count for thing 5.", 0, thingCounter.getCount(t5));
        Collection<Thing> keysFound = thingCounter.keys();
        assertThat("Wrong keys returned.", keysFound, contains(t1, t4, t2, t3));
        assertEquals("Too many keys returned.", 4, keysFound.size());
        assertEquals("Wrong number of entries in counter.", 4, thingCounter.size());
        Collection<CountMap<Thing>.Count> countsFound = thingCounter.sortedCounts();
        Collection<Thing> keysCounted = new ArrayList<Thing>(4);
        int prev = Integer.MAX_VALUE;
        for (CountMap<Thing>.Count result : countsFound) {
            assertThat("Counts out of order for " + result.getKey() + ".", result.getCount(), lessThan(prev));
            prev = result.getCount();
            keysCounted.add(result.getKey());
        }
        assertThat("Wrong keys returned in result list.", keysCounted, contains(t4, t3, t2, t1));
        PairCounter<Thing> pairCounter = new PairCounter<Thing>();
        assertEquals("Pair counter not empty after creation (pairs).", 0, pairCounter.size());
        assertEquals("Pair counter not empty after creation (items).", 0, pairCounter.itemSize());
        assertEquals("Pair counter nonzero at creation.", 0, pairCounter.getCount(t1, t2));
        pairCounter.recordOccurrence(t1);
        assertEquals("Pair count for t1 wrong.", 1, pairCounter.getCount(t1));
        assertEquals("Pair counter not empty after item record.", 0, pairCounter.size());
        assertEquals("Pair item counter not 1 after first count.", 1, pairCounter.itemSize());
        List<Thing> myList = Arrays.asList(t2, t3);
        pairCounter.recordOccurrence(t4, myList);
        pairCounter.recordPairing(t2, t4);
        // We have recorded t1 with no pairs and t4 with t2 and t3. We also have a pairing of t2 and t4.
        // The counts we expect are t1 = 1, t4 = 1, t2/t4 = 2, t3/t4 = 1, t4/t3 = 1, t4/t2 = 1.
        assertEquals("Pair counter pairs wrong.", 2, pairCounter.size());
        assertEquals("t1 counter wrong.", 1, pairCounter.getCount(t1));
        assertEquals("t2 counter wrong.", 0, pairCounter.getCount(t2));
        assertEquals("t4 counter wrong.", 1, pairCounter.getCount(t4));
        assertEquals("t2/t4 counter wrong.", 2, pairCounter.getCount(t2, t4));
        assertEquals("t3/t4 counter wrong.", 1, pairCounter.getCount(t3, t4));
        assertEquals("t4/t3 counter wrong.", 1, pairCounter.getCount(t4, t3));
        assertEquals("t4/t2 counter wrong.", 2, pairCounter.getCount(t4, t2));
        myList = Arrays.asList(t4, t1, t3, t3);
        pairCounter.recordOccurrence(t2, myList);
        pairCounter.recordOccurrence(t4, myList);
        assertEquals("Pair counter pairs wrong after second pass.", 6, pairCounter.size());
        assertEquals("Pair counter items wrong after second pass.", 3, pairCounter.itemSize());
        assertEquals("t1 counter wrong after second pass.", 1, pairCounter.getCount(t1));
        assertEquals("t2 counter wrong after second pass.", 1, pairCounter.getCount(t2));
        assertEquals("t4 counter wrong after second pass.", 2, pairCounter.getCount(t4));
        assertEquals("t2/t1 counter wrong after second pass.", 1, pairCounter.getCount(t2, t1));
        assertEquals("t1/t2 counter wrong after second pass.", 1, pairCounter.getCount(t1, t2));
        assertEquals("t2/t3 counter wrong after second pass.", 2, pairCounter.getCount(t2, t3));
        assertEquals("t3/t2 counter wrong after second pass.", 2, pairCounter.getCount(t3, t2));
        assertEquals("t1/t4 counter wrong after second pass.", 1, pairCounter.getCount(t1, t4));
        assertEquals("t2/t4 counter wrong after second pass.", 3, pairCounter.getCount(t2, t4));
        assertEquals("t3/t4 counter wrong after second pass.", 3, pairCounter.getCount(t3, t4));
        assertEquals("t4/t4 counter wrong after second pass.", 1, pairCounter.getCount(t4, t4));
        assertEquals("t4/t3 counter wrong after second pass.", 3, pairCounter.getCount(t4, t3));
        assertEquals("t4/t2 counter wrong after second pass.", 3, pairCounter.getCount(t4, t2));
        assertEquals("t4/t1 counter wrong after second pass.", 1, pairCounter.getCount(t4, t1));
        PairCounter<Thing>.Count testCounter = pairCounter.getPairCount(t4, t2);
        assertThat("Wrong counter returned (k1).", testCounter.getKey1(), is(oneOf(t2, t4)));
        assertThat("Wrong counter returned (k2).", testCounter.getKey2(), is(oneOf(t2, t4)));
        assertEquals("Wrong counter value.", 3, testCounter.getCount());
        assertEquals("Wrong togetherness.", 1.0, testCounter.togetherness(), 0.001);
        // Get the sorted list of counts.
        Collection<PairCounter<Thing>.Count> sortedPairs = pairCounter.sortedCounts(0.0, 0);
        prev = Integer.MAX_VALUE;
        for (PairCounter<Thing>.Count counter : sortedPairs) {
            assertThat("Counts out of order.", prev, greaterThanOrEqualTo(counter.getCount()));
            prev = counter.getCount();
        }
        // Get the sorted list of items counted.
        Collection<CountMap<Thing>.Count> sortedItems = pairCounter.sortedItemCounts();
        assertThat("Wrong list of items counted.",
                sortedItems.stream().map(CountMap<Thing>.Count::getKey).collect(Collectors.toList()),
                containsInAnyOrder(t1, t2, t4));
        prev = Integer.MAX_VALUE;
        for (CountMap<Thing>.Count counter : sortedItems) {
            assertThat("Counts out of order.", prev, greaterThanOrEqualTo(counter.getCount()));
            prev = counter.getCount();
        }
        // Get a better togetherness test.
        pairCounter.recordOccurrences(t2, 3);
        pairCounter.recordOccurrence(t4);
        testCounter = pairCounter.getPairCount(t4, t2);
        assertThat("Wrong counter returned (k1).", testCounter.getKey1(), is(oneOf(t2, t4)));
        assertThat("Wrong counter returned (k2).", testCounter.getKey2(), is(oneOf(t2, t4)));
        assertEquals("Wrong togetherness value.", 0.75, testCounter.togetherness(), 0.001);
        // Add some pairings.
        pairCounter.recordPairings(t1, t5, 4);
        assertEquals("Wrong counter returned after multi-count", 4, pairCounter.getCount(t5, t1));
        // Bump the item counts.
        pairCounter.recordOccurrences(t1, 6);
        pairCounter.recordOccurrences(t3, 10);
        pairCounter.recordOccurrences(t5, 8);
        // Try pair retrieval again with threshholds.
        sortedPairs = pairCounter.sortedCounts(0.30, 2);
        assertEquals("Threshold returned wrong number of pairs.", 3, sortedPairs.size());
        prev = Integer.MAX_VALUE;
        for (PairCounter<Thing>.Count counter : sortedPairs) {
            assertThat("Counts out of order in threshold list.", prev, greaterThanOrEqualTo(counter.getCount()));
            prev = counter.getCount();
            assertThat("Togetherness threshold failed.", counter.togetherness(), greaterThanOrEqualTo(0.30));
            assertThat("Mincount threshold failed.", counter.getCount(), greaterThanOrEqualTo(2));
        }
    }

    public void testDistance() {
        Location t1 = Location.create("myContig", "+", 10, 20);
        Location t2 = Location.create("myContig", "-", 25, 45);
        assertEquals("Wrong initial distance.", 4, t1.distance(t2));
        assertEquals("Distance not commutative", 4, t2.distance(t1));
        t1.setRight(24);
        assertEquals("Distance wrong when left adjacent.", 0, t1.distance(t2));
        t1.setRight(25);
        assertEquals("Distance wrong at overlap left edge.", -1, t1.distance(t2));
        t1.setRight(100);
        assertEquals("Distance wrong when contained.", -1, t1.distance(t2));
        t1.setLeft(45);
        assertEquals("Distance wrong at overlap right edge.", -1, t1.distance(t2));
        t1.setLeft(46);
        assertEquals("Distance wrong at right adjacent.", 0, t1.distance(t2));
        t1.setLeft(51);
        assertEquals("Distance wrong at end.", 5, t1.distance(t2));
        Feature f1 = myGto.getFeature("fig|1313.7001.peg.841");
        Feature f2 = myGto.getFeature("fig|1313.7001.peg.847");
        assertEquals("Feature distance not equal location distance.", f1.getLocation().distance(f2.getLocation()),
                f1.distance(f2));
        Feature f3 = myGto.getFeature("fig|1313.7001.peg.1113");
        assertEquals("Contig mismatch not caught.", Integer.MAX_VALUE, f1.distance(f3));
    }

    /**
     * Test the Role object
     */
    public void testRole() {
        // We need to verify that the normalization works.  We create
        // equivalent roles and insure their checksums are equal.
        String rDesc = "(R)-2-hydroxyacid dehydrogenase, similar to L-sulfolactate dehydrogenase (EC 1.1.1.272)";
        Role rObj1 = new Role("2HydrDehySimiLSulf", rDesc);
        assertEquals("Role ID not stored properly.", "2HydrDehySimiLSulf", rObj1.getId());
        Role rObj2 = new Role("2HydrDehySimiLSulf2", "(R)-2-hydroxyacid dehydrogenase, similar to L-sulfolactate dehydrogenase");
        assertEquals("EC number affects checksum.", rObj1.getChecksum(), rObj2.getChecksum());
        assertEquals("Equal checksums is not equal roles.", rObj1, rObj2);
        assertEquals("Equal checksums does not compare-0 roles.", 0, rObj1.compareTo(rObj2));
        assertEquals("Equal roles have different hashcodes.", rObj1.hashCode(), rObj2.hashCode());
        Role rObj3 = new Role("2HydrDehySimiLSulf3", "(r)-2-hydroxyacid dehydrogenase, similar to L-sulfolactate dehydrogenase");
        assertEquals("Role checksums are case sensitive.", rObj1, rObj3);
        Role rObj4 = new Role("2HydrDehySimiLSulf4", "(R)-2-hydroxyacid dehydrogenase (EC 1.1.1.272), similar to L-sulfolactate dehydrogenase");
        assertEquals("Role checksums affected by EC position.", rObj1, rObj4);
        Role rObj5 = new Role("2HydrDehySimiLSulf5", "(R)-2-hydroxyacid dehydrogenase similar to L-sulfolactate dehydrogenase");
        assertEquals("Role checksum affected by comma.", rObj1, rObj5);
    }


    /**
     * Test roles IDs.
     * @throws IOException
     */
    public void testRoleMagic() throws IOException {
        File inFile = new File("src/test", "words.txt");
        Scanner roleScanner = new Scanner(inFile);
        roleScanner.useDelimiter("\t|\r\n|\n");
        while (roleScanner.hasNext()) {
            String condensed = roleScanner.next();
            String full = roleScanner.next();
            assertEquals("String did not condense.", condensed, MagicMap.condense(full));
        }
        roleScanner.close();
        // Test registration
        RoleMap magicTable = new RoleMap();
        inFile = new File("src/test", "roles.txt");
        roleScanner = new Scanner(inFile);
        roleScanner.useDelimiter("\t|\r\n|\n");
        while (roleScanner.hasNext()) {
            String roleId = roleScanner.next();
            roleScanner.next();
            assertNull("Wrong ID found", magicTable.get(roleId));
            String roleDesc = roleScanner.next();
            Role newRole = new Role(roleId, roleDesc);
            magicTable.register(newRole);
            assertEquals("Registered ID did not read back.", roleDesc, magicTable.getName(roleId));
        }
        roleScanner.close();
        assertTrue("PheS not found.", magicTable.containsKey("PhenTrnaSyntAlph"));
        assertFalse("Known bad key found.", magicTable.containsKey("PhenTrnaSyntGamm"));
        String modifiedRole = "3-keto-L-gulonate-6-phosphate decarboxylase UlaK putative (L-ascorbate utilization protein D) (EC 4.1.1.85)";
        Role newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for modified role.", "3KetoLGulo6PhosDeca6", newRole.getId());
        assertSame("Modified role did not read back.", newRole, magicTable.get("3KetoLGulo6PhosDeca6"));
        modifiedRole = "Unique (new) role string without numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for unique role.", "UniqRoleStriWith", newRole.getId());
        assertSame("Unique role did not read back.", newRole, magicTable.get("UniqRoleStriWith"));
        Role findRole = magicTable.findOrInsert(modifiedRole);
        assertSame("Unique role was re-inserted.", newRole, findRole);
        modifiedRole = "Unique (old) role string without numbers";
        findRole = magicTable.findOrInsert(modifiedRole);
        assertTrue("Parenthetical did not change role ID.", findRole != newRole);
        assertEquals("Wrong ID assigned for parenthetical role.", "UniqRoleStriWith2", findRole.getId());
        assertSame("Parenthetical role did not read back.", findRole, magicTable.get("UniqRoleStriWith2"));
        modifiedRole = "Unique (newer) role string without numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for newer role.", "UniqRoleStriWith3", newRole.getId());
        assertSame("Parenthetical role did not read back.", newRole, magicTable.get("UniqRoleStriWith3"));
        modifiedRole = "Unique role string 12345 with numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Name not stored in role.", modifiedRole, newRole.getName());
        assertEquals("Wrong ID assigned for numbered role.", "UniqRoleStri1234n1", newRole.getId());
        modifiedRole = "Unique role string 12345 with more numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for second numbered role.", "UniqRoleStri1234n2", newRole.getId());
        // Test save and load.
        File saveFile = new File("src/test", "roles.ser");
        magicTable.save(saveFile);
        RoleMap newTable = RoleMap.load(saveFile);
        for (Role oldRole : magicTable.values()) {
            newRole = newTable.get(oldRole.getId());
            assertNotNull("Could not find role in loaded table.", newRole);
            assertEquals("Loaded table has wrong role name.", newRole.getName(), oldRole.getName());
            assertEquals("Loaded role has wrong checksum.", newRole, oldRole);
        }
        // Test on-the-fly registration.
        RoleMap goodRoles = new RoleMap();
        goodRoles.register("Role 1", "Role 2", "Role 3");
        assertNotNull("Role 1 not found", goodRoles.containsName("role 1"));
        assertNotNull("Role 2 not found", goodRoles.containsName("role 2"));
        assertNotNull("Role 3 not found", goodRoles.containsName("role 3"));
    }

    /**
     * Create a genome and check it for couplings.
     */
    public void testNeighbors() {
        Genome fakeGenome = new Genome("12345.6", "Bacillus praestrigiae Narnia", "Bacteria", 11);
        fakeGenome.addContig(new Contig("con1", "agct", 11));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.1",  "Role 1", "con1", "+",  100,  300));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.2",  "Role 2", "con1", "-",  100,  400));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.3",  "Role 3", "con1", "+",  200,  500));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.4",  "Role 4", "con1", "-", 1000, 1200));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.5",  "Role 5", "con1", "+", 1010, 1300));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.6",  "Role 6 / Role 1", "con1", "-", 3300, 4000));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.7",  "Role 2 # comment", "con1", "-", 5000, 5100));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.8",  "Role 3", "con1", "+", 5150, 5200));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.9",  "Role 1", "con1", "+", 5250, 5400));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.10", "Role 2", "con1", "-", 5401, 5450));
        assertEquals("Incorrect ID in fake genome.", "12345.6", fakeGenome.getId());
        assertEquals("Incorrect name in fake genome.", "Bacillus praestrigiae Narnia", fakeGenome.getName());
        assertEquals("Incorrect domain in fake genome.", "Bacteria", fakeGenome.getDomain());
        assertEquals("Incorrect genetic code in fake genome.", 11, fakeGenome.getGeneticCode());
        Collection<Contig> contigs0 = fakeGenome.getContigs();
        assertEquals("Wrong number of contigs in fake genome.", 1, contigs0.size());
        Contig[] contigs = new Contig[1];
        contigs = contigs0.toArray(contigs);
        assertEquals("Incorrect contig ID in fake contig.", "con1", contigs[0].getId());
        assertEquals("Incorrect genetic code in fake contig.", 11, contigs[0].getGeneticCode());
        assertEquals("Incorrect sequence in fake contig.", "agct", contigs[0].getSequence());
        Collection<Feature> features = fakeGenome.getFeatures();
        assertEquals("Incorrect number of features in fake genome.", 10, features.size());
        int found = 0;
        for (Feature feat : features) {
            assertTrue("Incorrect contig in " + feat + ".", feat.getLocation().isContig("con1"));
            if (feat.getId().contentEquals("fig|12345.6.peg.1")) {
                assertEquals("Wrong role in peg.1.", "Role 1", feat.getFunction());
                Location loc = feat.getLocation();
                assertEquals("Wrong strand in peg 1.", '+', loc.getDir());
                assertEquals("Wrong left in peg 1.", 100, loc.getLeft());
                assertEquals("Wrong right in peg 1.", 300, loc.getRight());
                found++;
            }
        }
        assertEquals("Test feature not found.", 1, found);
        // Create a feature list for the contig and do a coupling iteration.
        FeatureList contigFeatures = fakeGenome.getContigFeatures("con1");
        FeatureList.Position pos = contigFeatures.new Position();
        assertTrue("End-of-list at beginning.", pos.hasNext());
        Feature current = pos.next();
        assertEquals("Wrong feature first.", "fig|12345.6.peg.1", current.getId());
        Collection<Feature> neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg1.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                containsInAnyOrder("fig|12345.6.peg.2", "fig|12345.6.peg.3"));
        neighbors = pos.within(1000);
        assertThat("Wrong neighbors found at 1000 for peg1.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                containsInAnyOrder("fig|12345.6.peg.2", "fig|12345.6.peg.3",
                        "fig|12345.6.peg.4", "fig|12345.6.peg.5"));
        assertTrue("End of list after peg 1", pos.hasNext());
        current = pos.next();
        assertEquals("Wrong feature after peg 1.", "fig|12345.6.peg.2", current.getId());
        neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg2.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.3"));
        current = pos.next();
        assertEquals("Wrong feature after peg 2.", "fig|12345.6.peg.3", current.getId());
        neighbors = pos.within(100);
        assertEquals("Found neighbors at 100 for peg3.", 0, neighbors.size());
        current = pos.next();
        assertEquals("Wrong feature after peg 3.", "fig|12345.6.peg.4", current.getId());
        neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg4.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.5"));
        assertTrue("End-of-list after peg 4.", pos.hasNext());
        current = pos.next();
        assertEquals("Wrong feature after peg 4.", "fig|12345.6.peg.5", current.getId());
        neighbors = pos.within(100);
        assertEquals("Found neighbors at 100 for peg5.", 0, neighbors.size());
        current = pos.next();
        assertEquals("Wrong feature after peg 5.", "fig|12345.6.peg.6", current.getId());
        neighbors = pos.within(100);
        assertEquals("Found neighbors at 100 for peg6.", 0, neighbors.size());
        current = pos.next();
        assertEquals("Wrong feature after peg 6.", "fig|12345.6.peg.7", current.getId());
        neighbors = pos.within(48);
        assertEquals("Found neighbors at 48 for peg7.", 0, neighbors.size());
        neighbors = pos.within(49);
        assertThat("Wrong neighbors found at 49 for peg7.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.8"));
        neighbors = pos.within(50);
        assertThat("Wrong neighbors found at 50 for peg7.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.8"));
        neighbors = pos.within(10000);
        assertThat("Wrong neighbors found at extreme distance for peg7.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                containsInAnyOrder("fig|12345.6.peg.8", "fig|12345.6.peg.9",
                        "fig|12345.6.peg.10"));
        assertTrue("End-of-list after peg 7.", pos.hasNext());
        current = pos.next();
        assertEquals("Wrong feature after peg 7.", "fig|12345.6.peg.8", current.getId());
        neighbors = pos.within(48);
        assertEquals("Found neighbors at 48 for peg8.", 0, neighbors.size());
        neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg8.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.9"));
        current = pos.next();
        assertEquals("Wrong feature after peg 8.", "fig|12345.6.peg.9", current.getId());
        neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg9.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.10"));
        current = pos.next();
        neighbors = pos.within(1000);
        assertEquals("Found neighbors for peg10.", 0, neighbors.size());
        assertFalse("No end-of-list after peg10.", pos.hasNext());
    }

    /**
     * test roles-of-function
     */

    public void testFunctions() {
        String fun1 = "fun serum/thing / slash role @ at-role; semi role   # pound comment ## pounding";
        String[] roles = Feature.rolesOfFunction(fun1);
        assertEquals("Wrong role count.", 4, roles.length);
        assertEquals("first role wrong", "fun serum/thing", roles[0]);
        assertEquals("second role wrong", "slash role", roles[1]);
        assertEquals("third role wrong", "at-role", roles[2]);
        assertEquals("fourth role wrong", "semi role", roles[3]);
        fun1 = "unitary role with comment ! bang comment";
        roles = Feature.rolesOfFunction(fun1);
        assertEquals("Wrong unit role count.", 1, roles.length);
        assertEquals("Wrong unit role", "unitary role with comment", roles[0]);
        roles = Feature.rolesOfFunction("");
        assertEquals("Failure on empty function", 0, roles.length);
        roles = Feature.rolesOfFunction(null);
        assertEquals("Failure on null function", 0, roles.length);
        roles = Feature.rolesOfFunction("simple");
        assertEquals("Failure on simple role", 1, roles.length);
        assertEquals("Wrong simple role", "simple", roles[0]);
        Feature testFeat = myGto.getFeature("fig|1313.7001.peg.1189");
        roles = testFeat.getRoles();
        assertEquals("Wrong number of roles in feature", 2, roles.length);
        assertEquals("Wrong first role of feature", "IMP cyclohydrolase (EC 3.5.4.10)", roles[0]);
        assertEquals("Wrong second role of feature", "Phosphoribosylaminoimidazolecarboxamide formyltransferase (EC 2.1.2.3)",
                roles[1]);
        // Test useful roles.
        RoleMap goodRoles = new RoleMap();
        goodRoles.register("Phosphoribosylaminoimidazolecarboxamide formyltransferase (EC 2.1.2.3)");
        List<Role> found = testFeat.getUsefulRoles(goodRoles);
        assertEquals("Useful role count wrong.", 1, found.size());
        assertEquals("Wrong useful role returned", "PhosForm", found.get(0).getId());
        testFeat = myGto.getFeature("fig|1313.7001.peg.1190");
        found = testFeat.getUsefulRoles(goodRoles);
        assertEquals("Useful role found in useless peg.", 0, found.size());
    }

}
