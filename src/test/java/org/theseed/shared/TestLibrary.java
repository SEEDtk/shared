/**
 *
 */
package org.theseed.shared;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;

import org.theseed.genomes.Feature;
import org.theseed.genomes.Genome;
import org.theseed.genomes.GenomeDirectory;
import org.theseed.locations.Frame;
import org.theseed.locations.Location;
import org.theseed.locations.LocationList;
import org.theseed.locations.Region;
import org.theseed.utils.CountMap;
import org.theseed.utils.MagicMap;

import junit.framework.TestCase;

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
        }
    }

    public void testCounts() {
        CountMap<String> testMap = new CountMap<String>();
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



}
