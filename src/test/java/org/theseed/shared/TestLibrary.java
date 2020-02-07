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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.theseed.counters.CountMap;
import org.theseed.counters.KeyPair;
import org.theseed.counters.PairCounter;
import org.theseed.counters.QualityCountMap;
import org.theseed.genome.Annotation;
import org.theseed.genome.CloseGenome;
import org.theseed.genome.Contig;
import org.theseed.genome.Contig.ContigKeys;
import org.theseed.genome.Feature;
import org.theseed.genome.FeatureList;
import org.theseed.genome.Genome;
import org.theseed.genome.GenomeDirectory;
import org.theseed.genome.TaxItem;
import org.theseed.io.BalancedOutputStream;
import org.theseed.io.Shuffler;
import org.theseed.io.TabbedLineReader;
import org.theseed.io.TabbedLineReader.Line;
import org.theseed.locations.FLocation;
import org.theseed.locations.Frame;
import org.theseed.locations.Location;
import org.theseed.locations.DiscreteLocationList;
import org.theseed.locations.DiscreteLocationList.Edge;
import org.theseed.locations.Region;
import org.theseed.locations.SortedLocationList;
import org.theseed.magic.MagicMap;
import org.theseed.proteins.CodonSet;
import org.theseed.proteins.DnaTranslator;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.proteins.kmers.KmerCollectionGroup;
import org.theseed.proteins.kmers.ProteinKmers;
import org.theseed.sequence.FastaInputStream;
import org.theseed.sequence.FastaOutputStream;
import org.theseed.sequence.Sequence;
import org.theseed.utils.FloatList;
import org.theseed.utils.IntegerList;
import org.theseed.utils.Parms;

import com.github.cliftonlabs.json_simple.JsonObject;
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
        this.myGto = new Genome(new File("src/test/gto_test", "1313.7001.gto"));
    }

    private Genome myGto = null;

    /**
     * test sequence group kmer distance
     *
     * @throws IOException
     */
    public void testKmerCollectionGroup() throws IOException {
        ProteinKmers.setKmerSize(8);
        KmerCollectionGroup kGroup = new KmerCollectionGroup();
        File inFile = new File("src/test", "seq_list.fa");
        FastaInputStream inStream = new FastaInputStream(inFile);
        for (Sequence inSeq : inStream) {
            String label = inSeq.getComment();
            kGroup.addSequence(inSeq, label);
        }
        Collection<String> groups = kGroup.getKeys();
        assertThat(groups, containsInAnyOrder("AntiHiga", "ToxiHigb"));
        assertThat(kGroup.size(), equalTo(2));
        inStream.close();
        Sequence testSeq = new Sequence("test1", "", "MILLRRLLGDVLRRQRQRQGRTLREVSSSARVSLGYLSEVERGQKEASSELLSAICDALD" +
                "VRMSELMREVSDELALAELARSAAATPSETVPAPVRPMLGSVSVTGVPPERVTIKAPAEA" +
                "VDVVAA");
        Sequence testSeq2 = new Sequence("test2", "", "MTIQTFLCQDTDIYEGKHPRRFRNIEAVAERKLQMLDAAVELKDLRSPPGNRLEALIGD" +
                "RAGQHSIRINDQWRICFVWTGPDRVEIVDYH");
        double dist = kGroup.getDistance(testSeq, "AntiHiga");
        assertThat(dist, closeTo(0.4200, 0.0001));
        dist = kGroup.getDistance(testSeq, "ToxiHigb");
        assertThat(dist, closeTo(1.0, 0.0001));
        dist = kGroup.getDistance(testSeq2, "AntiHiga");
        assertThat(dist, closeTo(1.0, 0.0001));
        dist = kGroup.getDistance(testSeq2, "ToxiHigb");
        assertThat(dist, closeTo(0.2929, 0.0001));
        KmerCollectionGroup.Result ret = kGroup.getBest(testSeq);
        assertThat(ret.getDistance(), closeTo(0.4200, 0.0001));
        assertThat(ret.getGroup(), equalTo("AntiHiga"));
        ret = kGroup.getBest(testSeq2);
        assertThat(ret.getDistance(), closeTo(0.2929, 0.0001));
        assertThat(ret.getGroup(), equalTo("ToxiHigb"));
    }


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
        // Add two aliases.
        magicTable.addAlias("PhenTrnaSyntDoma", "alias second Phenylalanyl role string");
        modifiedThing = "ALIAS Second phenylalanyl role string";
        Thing myThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Alias 1 did not work.", "PhenTrnaSyntDoma", myThing.getId());
        assertEquals("Alias 1 overrode text.", "Phenylalanyl-tRNA synthetase domain protein (Bsu YtpR)", myThing.getName());
        magicTable.addAlias("PhenTrnaSyntDoma", "alias third Phenylalanyl role string");
        modifiedThing = "Phenylalanyl-tRNA synthetase domain protein (Bsu YtpR)";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertSame("Original string did not work.", newThing, myThing);
        modifiedThing = "alias second Phenylalanyl role string";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertSame("Alias 1 string did not work.", newThing, myThing);
        modifiedThing = "alias third Phenylalanyl role string";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertSame("Alias 2 string did not work.", newThing, myThing);

        // Test save and load.
        File saveFile = new File("src/test", "things.ser");
        magicTable.save(saveFile);
        ThingMap newTable = ThingMap.load(saveFile);
        for (Thing oldThing : magicTable.values()) {
            newThing = newTable.get(oldThing.getId());
            assertNotNull("Could not find thing in loaded table.", newThing);
            if (! oldThing.getName().startsWith("alias")) {
                assertEquals("Loaded table has wrong thing name.", oldThing.getName(), newThing.getName());
                assertEquals("Loaded thing has wrong checksum.", oldThing, newThing);
            }
            newThing = newTable.getByName(oldThing.getName());
            assertNotNull("Could not find thing by name in loaded table.", newThing);
            assertEquals("Found incorrect object by name in loaded table.", oldThing.getId(), newThing.getId());
        }
        modifiedThing = "Phenylalanyl-tRNA synthetase domain protein (Bsu YtpR)";
        myThing = newTable.findOrInsert(modifiedThing);
        modifiedThing = "alias second Phenylalanyl role string";
        newThing = newTable.findOrInsert(modifiedThing);
        assertSame("Alias 1 string did not work.", newThing, myThing);
        modifiedThing = "alias third Phenylalanyl role string";
        newThing = newTable.findOrInsert(modifiedThing);
        assertSame("Alias 2 string did not work.", newThing, myThing);

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
        assertThat("Allkeys returned wrong set.", testMap.allKeys(),
                containsInAnyOrder("AAA", "BBB", "CCC", "DDD"));
        testMap.setGood("BBB", 4);
        assertEquals("Incremental set-good failed.", 4, testMap.good("BBB"));
        testMap.setBad("DDD", 6);
        assertEquals("Incremental set-bad failed.", 7, testMap.bad("DDD"));
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
        assertEquals("Incorrect contig count.", 52, this.myGto.getContigCount());
        assertEquals("Incorrect length.", 2101113, this.myGto.getLength());
        assertEquals("Genome home not correct", "PATRIC", this.myGto.getHome());
        String testLink = this.myGto.genomeLink().render();
        assertThat(testLink, containsString("patricbrc"));
        assertThat(testLink, containsString("1313.7001"));
        testLink = this.myGto.featureLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, containsString("patricbrc"));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        testLink = this.myGto.featureRegionLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, containsString("patricbrc"));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        this.myGto.setHome("CORE");
        testLink = this.myGto.genomeLink().render();
        assertThat(testLink, containsString("core.theseed"));
        assertThat(testLink, containsString("1313.7001"));
        testLink = this.myGto.featureLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, containsString("core.theseed"));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        testLink = this.myGto.featureRegionLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, containsString("core.theseed"));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        this.myGto.setHome("none");
        testLink = this.myGto.genomeLink().render();
        assertThat(testLink, not(containsString("<a")));
        assertThat(testLink, containsString("1313.7001"));
        testLink = this.myGto.featureLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, not(containsString("<a")));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        testLink = this.myGto.featureRegionLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, not(containsString("<a")));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        // Now we need to pull out a PEG and ask about it.
        Feature myFeature = this.myGto.getFeature("fig|1313.7001.peg.758");
        assertNotNull("Sample feature not found.", myFeature);
        assertEquals("Incorrect feature found.", "fig|1313.7001.peg.758", myFeature.getId());
        assertEquals("Incorrect function in sample feature.", "Transposase, IS4 family", myFeature.getFunction());
        assertEquals("Incorrect protein for sample feature.", myProtein, myFeature.getProteinTranslation());
        assertEquals("Incorrect protein length for sample feature.", myProtein.length(), myFeature.getProteinLength());
        assertEquals("Incorrect DNA for sample feature.", myDna1, this.myGto.getDna("fig|1313.7001.peg.758"));
        assertEquals("Incorrect local family for sample feature.", "PLF_1301_00010583", myFeature.getPlfam());
        assertEquals("Incorrect global family for sample feature.", "PGF_07475842", myFeature.getPgfam());
        assertTrue("PEG not a CDS", myFeature.isCDS());
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
        assertFalse("Non-peg typed as CDS", myFeature.isCDS());
        assertEquals("Incorrect DNA retrieval by location.", myDna2, this.myGto.getDna(myLoc));
        // Now iterate over the proteins.
        for (Feature feat : this.myGto.getPegs()) {
            assertEquals("Feature" + feat.getId() + " is not a PEG.", "CDS", feat.getType());
        }
        String[] taxonomy = new String[] {"1313", "1301", "1300", "186826", "91061",
                                          "1239", "1783272", "2", "131567"};
        int i = 0;
        Iterator<TaxItem> iter = this.myGto.taxonomy();
        while (iter.hasNext()) {
            TaxItem item = iter.next();
            assertThat(item.getId(), equalTo(taxonomy[i]));
            i++;
        }
        assertThat(i, equalTo(taxonomy.length));
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
        assertThat(region1.getBegin("+"), equalTo(1000));
        assertThat(region1.getBegin("-"), equalTo(2000));
        assertThat(region1.getLength(), equalTo(1001));
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
        // Test frames
        for (int i = 0; i < 200; i += 3) {
            assertThat(Location.create("Mycontig", "+", i, i+5).getFrame(), equalTo(Frame.P0));
            assertThat(Location.create("Mycontig", "+", i+1, i+6).getFrame(), equalTo(Frame.P1));
            assertThat(Location.create("Mycontig", "+", i+2, i+7).getFrame(), equalTo(Frame.P2));
            assertThat(Location.create("Mycontig", "-", i, i+5).getFrame(), equalTo(Frame.M0));
            assertThat(Location.create("Mycontig", "-", i-1, i+4).getFrame(), equalTo(Frame.M1));
            assertThat(Location.create("Mycontig", "-", i-2, i+3).getFrame(), equalTo(Frame.M2));
        }
    }

    /**
     * Test location merging.
     */
    public void testLocationMerge() {
        Location loc1 = Location.create("MyContig", "+", 100, 110, 200, 210);
        Location loc2 = Location.create("MyContig", "+", 120, 130, 150, 250);
        loc2.merge(loc1);
        assertThat(loc2.getContigId(), equalTo("MyContig"));
        assertThat(loc2.getDir(), equalTo('+'));
        assertThat(loc2.getLeft(), equalTo(100));
        assertThat(loc2.getRight(), equalTo(250));
        assertThat(loc2.getBegin(), equalTo(100));
        assertThat(loc2.getEnd(), equalTo(250));
        loc1.setBegin(50);
        assertThat(loc1.getLeft(), equalTo(50));
        assertThat(loc1.getBegin(), equalTo(50));
        loc2 = Location.create("MyContig", "-", 1000, 2000);
        loc1 = Location.create("MyContig", "-", 500, 2500);
        loc2.merge(loc1);
        assertThat(loc2.getContigId(), equalTo("MyContig"));
        assertThat(loc2.getDir(), equalTo('-'));
        assertThat(loc2.getLeft(), equalTo(500));
        assertThat(loc2.getRight(), equalTo(2500));
        assertThat(loc2.getBegin(), equalTo(2500));
        assertThat(loc2.getEnd(), equalTo(500));
        loc1.setBegin(2000);
        assertThat(loc1.getRight(), equalTo(2000));
        assertThat(loc1.getBegin(), equalTo(2000));
    }

    /**
     * Main discrete location list test
     */
    public void testDiscreteLocationList() {
        DiscreteLocationList newList = new DiscreteLocationList("myContig");
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
        // Test edge computation.
        assertThat(newList.isEdge(10, false), equalTo(Edge.START));
        assertThat(newList.isEdge(50, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(197, false), equalTo(Edge.STOP));
        assertThat(newList.isEdge(400, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(102, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(999, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(502, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(8000, false), equalTo(Edge.START));
        assertThat(newList.isEdge(8197, false), equalTo(Edge.STOP));
        assertThat(newList.isEdge(10, true), equalTo(Edge.START));
        assertThat(newList.isEdge(50, true), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(197, true), equalTo(Edge.STOP));
        assertThat(newList.isEdge(400, true), equalTo(Edge.START));
        assertThat(newList.isEdge(102, true), equalTo(Edge.STOP));
        assertThat(newList.isEdge(999, true), equalTo(Edge.START));
        assertThat(newList.isEdge(502, true), equalTo(Edge.STOP));
        assertThat(newList.isEdge(8000, true), equalTo(Edge.START));
        assertThat(newList.isEdge(8197, true), equalTo(Edge.STOP));
        // Finally, we want to test the frame computation.
        assertEquals("Invalid frame for contig start.", Frame.F0, newList.computeRegionFrame(1, 9));
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
        Map<String, DiscreteLocationList> gList = DiscreteLocationList.createGenomeCodingMap(this.myGto);
        DiscreteLocationList contig0036 = gList.get("1313.7001.con.0036");
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
        assertEquals("Wrong number of genomes found.", 5, gDir.size());
        // Run through an iterator.  We know the genome IDs, we just need to find them in order.
        String[] expected = new String[] { "1005394.4", "1313.7001", "1313.7002", "1313.7016", "221988.1" };
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
            assertEquals("Contig has wrong length.", contig.getSequence().length(), contig.length());
        }
        assertEquals("Feature count incorrect.", myGto.getFeatureCount(), featureCount);
        Genome gto2 = new Genome(new File("src/test", "testLocs.gto"));
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
        List<CountMap<Thing>.Count> countsFound = thingCounter.sortedCounts();
        List<Thing> keysCounted = new ArrayList<Thing>(4);
        int prev = Integer.MAX_VALUE;
        for (CountMap<Thing>.Count result : countsFound) {
            assertThat("Counts out of order for " + result.getKey() + ".", result.getCount(), lessThan(prev));
            prev = result.getCount();
            keysCounted.add(result.getKey());
        }
        Collection<CountMap<Thing>.Count> allCounts = thingCounter.counts();
        assertThat("Unsorted counts came back wrong.", allCounts, containsInAnyOrder(countsFound.toArray()));
        assertThat("Wrong keys returned in result list.", keysCounted, contains(t4, t3, t2, t1));
        thingCounter.setCount(t4, 10);
        assertThat("Wrong count after set.", thingCounter.getCount(t4), equalTo(10));
        thingCounter.clear();
        assertThat("Wrong count for thing 2 after clear.", thingCounter.getCount(t2), equalTo(0));
        thingCounter.count(t1);
        assertThat("Wrong count for thing 1 after recount.", thingCounter.getCount(t1), equalTo(1));
        thingCounter.count(t4);
        Set<Thing> singletons = thingCounter.getSingletons();
        assertThat("Wrong set of singletons", singletons, containsInAnyOrder(t1, t4));
        thingCounter.deleteAll();
        assertThat("Keys left after deleteAll", thingCounter.keys().size(), equalTo(0));
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
     * Test role IDs.
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
     * test custom features
     */
    public void testFeature() {
        Feature feat = new Feature("fig|161.31.peg.10", "hypothetical function", "c1", "+", 100, 200);
        Location loc = feat.getLocation();
        assertThat(loc.getContigId(), equalTo("c1"));
        assertThat(loc.getDir(), equalTo('+'));
        assertThat(loc.getLeft(), equalTo(100));
        assertThat(loc.getRight(), equalTo(200));
        feat.setProteinTranslation("MABCDEFG");
        assertThat(feat.getProteinTranslation(), equalTo("MABCDEFG"));
        assertThat(feat.getProteinLength(), equalTo(8));
        feat.setPgfam("");
        assertNull(feat.getPgfam());
        feat.setPlfam("");
        assertNull(feat.getPlfam());
        feat.setPgfam("PG1");
        feat.setPlfam("PG2");
        assertThat(feat.getPlfam(), equalTo("PG2"));
        assertThat(feat.getPgfam(), equalTo("PG1"));
        Feature f2 = new Feature("fig|161.31.peg.10", "more hypothetical function", "c2", "+", 110, 220);
        assertThat(feat.compareTo(f2), equalTo(0));
        f2 = new Feature("fig|161.31.peg.2", "less hypothetical function", "c2", "+", 120, 240);
        assertThat(feat.compareTo(f2), greaterThan(0));
        f2 = new Feature("fig|161.31.peg.20", "strange hypothetical function", "c2", "-", 220, 540);
        assertThat(feat.compareTo(f2), lessThan(0));
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
        // Test hypotheticals
        assertTrue(Feature.isHypothetical("hypothetical protein"));
        assertTrue(Feature.isHypothetical(null));
        assertTrue(Feature.isHypothetical("  # comment only"));
        assertTrue(Feature.isHypothetical("Hypothetical protein # with comment"));
        assertFalse(Feature.isHypothetical("Normal function # hypothetical protein"));
        assertFalse(Feature.isHypothetical("May some day be a putative function"));
    }

    /**
     * Test the tabbed input file.
     *
     * @throws IOException
     */
    public void testTabbedFile() throws IOException {
        File inFile = new File("src/test", "tabbed.txt");
        TabbedLineReader tabReader = new TabbedLineReader(inFile);
        assertThat("Wrong number of columns.", tabReader.size(), equalTo(5));
        assertThat("Header line wrong.", tabReader.header(), equalTo("genome_id\tgenome.genome_name\tcounter.0\tfraction\tflag"));
        // Test the column finder.
        assertThat("Did not find genome name.", tabReader.findField("genome_name"), equalTo(1));
        assertThat("Did not find fraction.", tabReader.findField("fraction"), equalTo(3));
        assertThat("Could not find column 3.", tabReader.findField("3"), equalTo(2));
        assertThat("Cound not find last column.", tabReader.findField("0"), equalTo(4));
        int colIdx = 0;
        try {
            colIdx = tabReader.findField("genome");
            fail("Found genome column at " + colIdx);
        } catch (IOException e) {
            // this is good
        }
        try {
            colIdx = tabReader.findField("10");
            fail("Found out-of-bounds column at " + colIdx);
        } catch (IOException e) {
            // this is good
        }
        try {
            colIdx = tabReader.findField("-6");
            fail("Found out-of-bounds column at " + colIdx);
        } catch (IOException e) {
            // this is good
        }
        TabbedLineReader.Line line = tabReader.next();
        assertThat("Wrong value in column 0 of line 1", line.get(0), equalTo("100.99"));
        assertThat("Wrong value in column 2 of line 1", line.getInt(2), equalTo(10));
        assertThat("Wrong value in column 3 of line 1", line.getDouble(3), closeTo(0.8, 0.0001));
        assertFalse("Boolean adjustment fail in line 1", line.getFlag(4));
        assertThat("Line input not working", line.getAll(), equalTo("100.99\tname of 100.99\t10\t0.8"));
        line = tabReader.next();
        assertThat("Wrong value in column 2 of line 2", line.getInt(2), equalTo(-4));
        assertThat("Wrong value in column 3 of line 2", line.getDouble(3), equalTo(12.0));
        assertTrue("Wrong value in column 4 of line 2", line.getFlag(4));
        line = tabReader.next();
        assertFalse("Wrong value in column 4 of line 3", line.getFlag(4));
        line = tabReader.next();
        assertThat("Blank column failure.", line.getInt(2), equalTo(5));
        assertFalse("End of file not detected", tabReader.hasNext());
        assertNull("Error reading past end-of-file", tabReader.next());
        tabReader.close();
        // Reopen to test iteration.
        tabReader = new TabbedLineReader(inFile);
        String[] testLabels = new String[] { "100.99", "200.20", "1000.6", "4" };
        int i = 0;
        for (TabbedLineReader.Line l : tabReader) {
            assertThat("Wrong record at index " + i, l.get(0), equalTo(testLabels[i]));
            i++;
        }
        assertThat("Wrong number of records", i, equalTo(4));
        tabReader.close();
        // Test headerless files.
        File fixFile = new File("src/test", "fixed.txt");
        tabReader = new TabbedLineReader(fixFile, 5);
        line = tabReader.next();
        assertThat("Wrong value in column 0 of line 1", line.get(0), equalTo("100.99"));
        assertThat("Wrong value in column 2 of line 1", line.getInt(2), equalTo(10));
        assertThat("Wrong value in column 3 of line 1", line.getDouble(3), closeTo(0.8, 0.0001));
        assertFalse("Boolean adjustment fail in line 1", line.getFlag(4));
        assertThat("Line input not working", line.getAll(), equalTo("100.99\tname of 100.99\t10\t0.8"));
        line = tabReader.next();
        assertThat("Wrong value in column 2 of line 2", line.getInt(2), equalTo(-4));
        assertThat("Wrong value in column 3 of line 2", line.getDouble(3), equalTo(12.0));
        assertTrue("Wrong value in column 4 of line 2", line.getFlag(4));
        line = tabReader.next();
        assertFalse("Wrong value in column 4 of line 3", line.getFlag(4));
        assertFalse("End of file not detected", tabReader.hasNext());
        assertNull("Error reading past end-of-file", tabReader.next());
        tabReader.close();
    }

    /**
     * FASTA file test
     * @throws IOException
     */
    public void testFasta() throws IOException {
        File inFasta = new File("src/test", "empty.fa");
        FastaInputStream inStream = new FastaInputStream(inFasta);
        assertFalse("Error in empty fasta.", inStream.hasNext());
        inStream.close();
        inFasta = new File("src/test", "test.fa");
        inStream = new FastaInputStream(inFasta);
        ArrayList<Sequence> testSeqs = new ArrayList<Sequence>(5);
        for (Sequence input : inStream) {
            testSeqs.add(input);
        }
        inStream.close();
        assertEquals("Wrong number of sequences.", 5, testSeqs.size());
        Sequence seq = testSeqs.get(0);
        assertEquals("Wrong label for seq 1.", "label1", seq.getLabel());
        assertEquals("Wrong comment for seq 1.", "", seq.getComment());
        assertEquals("Wrong sequence for seq 1.", "tgtgcagcgagccctacagccttggagggaacaacacggactacctgccgctcgtctacccaaagggggtccccctccccaacacaacggttaccagcgtgccgagcg", seq.getSequence());
        seq = testSeqs.get(1);
        assertEquals("Wrong label for seq 2.", "label2", seq.getLabel());
        assertEquals("Wrong comment for seq 2.", "comment2 with spaces", seq.getComment());
        assertEquals("Wrong sequence for seq 2.", "ctcaatgggtccgtagtcggcatcggcagatgtgtataagcagcatgcccgccctctgcag", seq.getSequence());
        seq = testSeqs.get(2);
        assertEquals("Wrong label for seq 3.", "label3", seq.getLabel());
        assertEquals("Wrong comment for seq 3.", "comment3", seq.getComment());
        assertEquals("Wrong sequence for seq 3.", "gtataaagtattggcctgttgag", seq.getSequence());
        seq = testSeqs.get(3);
        assertEquals("Wrong label for seq 4.", "label4", seq.getLabel());
        assertEquals("Wrong comment for seq 4.", "comment4", seq.getComment());
        assertEquals("Wrong sequence for seq 4.", "", seq.getSequence());
        seq = testSeqs.get(4);
        assertEquals("Wrong label for seq 5.", "label5", seq.getLabel());
        assertEquals("Wrong comment for seq 5.", "comment5", seq.getComment());
        assertEquals("Wrong sequence for seq 5.", "ggggccctgaggtcctgagcaagtgggtcggcgagagcgagaaggcgataaggt", seq.getSequence());
        // Write the FASTA back out.
        File outFasta = new File("src/test", "fasta.ser");
        FastaOutputStream outStream = new FastaOutputStream(outFasta);
        outStream.write(testSeqs);
        outStream.close();
        // Read it in and verify.
        inStream = new FastaInputStream(outFasta);
        ArrayList<Sequence> readSeqs = new ArrayList<Sequence>(5);
        while (inStream.hasNext()) {
            readSeqs.add(inStream.next());
        }
        inStream.close();
        assertEquals("Wrong number of records on read back.", 5, readSeqs.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("Compare error at position " + i + " on readback.", testSeqs.get(i), readSeqs.get(i));
        }
    }

    /**
     * test frame comparison
     */
    public void testFrames() {
        assertThat("M0 compare fail.", Frame.M0.compareTo(Frame.F0), lessThan(0));
        assertThat("M1 compare fail.", Frame.M1.compareTo(Frame.F0), lessThan(0));
        assertThat("M2 compare fail.", Frame.M2.compareTo(Frame.F0), lessThan(0));
        assertThat("F0 compare fail.", Frame.F0.compareTo(Frame.F0), equalTo(0));
        assertThat("P0 compare fail.", Frame.P0.compareTo(Frame.F0), greaterThan(0));
        assertThat("P1 compare fail.", Frame.P1.compareTo(Frame.F0), greaterThan(0));
        assertThat("P2 compare fail.", Frame.P2.compareTo(Frame.F0), greaterThan(0));
        assertTrue("M0 negative fail.", Frame.M0.negative());
        assertTrue("M1 negative fail.", Frame.M1.negative());
        assertTrue("M2 negative fail.", Frame.M2.negative());
        assertFalse("F0 negative fail.", Frame.F0.negative());
        assertFalse("P0 negative fail.", Frame.P0.negative());
        assertFalse("P1 negative fail.", Frame.P1.negative());
        assertFalse("P2 negative fail.", Frame.P2.negative());
    }

    /**
     * test protein translation
     */
    public void testDnaTranslator() {
        // Start with GC 4.
        DnaTranslator xlator = new DnaTranslator(4);
        String dna1 = "atggctataccaccggaggtgcactcgggcctgttgagcgccgggtgcggtccgggatca" +
                "ttgcttgttgccgcgcagcagtggcaagaacttagtgatcagtacgcactcgcatgcgcc" +
                "gagttgggccaattgttgggcgaggttcaggccagcagctggcagggaaccgccgccacc" +
                "cagtacgtggctgcccatggcccctatctggcctggcttgagcaaaccgcgatcaacagc" +
                "gccgtcaccgccgcacagcacgtagcggctgccgctgcctactgcagcgccctggccgcg" +
                "atgcccaccccagcagagctggccgccaaccacgccattcatggcgttctgatcgccacc" +
                "aacttcttcgggatcaacaccgttccgatcgcgctcaacgaagccgattatgtccgcatg" +
                "tggctgcaagccgccgacaccatggccgcctaccaggccgtcgccgatgcggccacggtg" +
                "gccgtaccgtccacccaaccggcgccaccgatccgcgcgcccggcggcgatgccgcagat" +
                "acctggctagacgtattgagttcaattggtcagctcatccgggatatcttggatttcatt" +
                "gccaacccgtacaagtattttctggagtttttcgagcaattcggcttcagcccggccgta" +
                "acggtcgtccttgcccttgttgccctgcagctgtacgactttctttggtatccctattac" +
                "gcctcgtacggcctgctcctgcttccgttcttcactcccaccttgagcgcgttgaccgcc" +
                "ctaagcgcgctgatccatttgctgaacctgcccccggctggactgcttcctatcgccgca" +
                "gcgctcggtcccggcgaccaatggggcgcaaacttggctgtggctgtcacgccggccacg" +
                "gcggccgtgcccggcggaagcccgcccaccagcaaccccgcgcccgccgctcccagctcg" +
                "aactcggttggcagcgcttcggctgcacccggcatcagctatgccgtgccaggcctggcg" +
                "ccacccggggttagctctggccctaaagccggcaccaaatcacctgacaccgccgccgac" +
                "acccttgcaaccgcgggcgcagcacgaccgggcctcgcccgagcccaccgaagaaagcgc" +
                "agcgaaagcggcgtcgggatacgcggttaccgcgacgaatttttggacgcgaccgccacg" +
                "gtggacgccgctacggatgtgcccgctcccgccaacgcggctggcagtcaaggtgccggc" +
                "actctcggctttgccggtaccgcaccgacaaccagcggcgccgcggccggaatggttcaa" +
                "ctgtcgtcgcacagcacaagcactacagtcccgttgctgcccactacctggacaaccgac" +
                "gccgaacaatga";
        String prot1 = "MAIPPEVHSGLLSAGCGPGSLLVAAQQWQELSDQYALACAELGQLLGEVQASSWQGTAAT" +
                "QYVAAHGPYLAWLEQTAINSAVTAAQHVAAAAAYCSALAAMPTPAELAANHAIHGVLIAT" +
                "NFFGINTVPIALNEADYVRMWLQAADTMAAYQAVADAATVAVPSTQPAPPIRAPGGDAAD" +
                "TWLDVLSSIGQLIRDILDFIANPYKYFLEFFEQFGFSPAVTVVLALVALQLYDFLWYPYY" +
                "ASYGLLLLPFFTPTLSALTALSALIHLLNLPPAGLLPIAAALGPGDQWGANLAVAVTPAT" +
                "AAVPGGSPPTSNPAPAAPSSNSVGSASAAPGISYAVPGLAPPGVSSGPKAGTKSPDTAAD" +
                "TLATAGAARPGLARAHRRKRSESGVGIRGYRDEFLDATATVDAATDVPAPANAAGSQGAG" +
                "TLGFAGTAPTTSGAAAGMVQLSSHSTSTTVPLLPTTWTTDAEQW";
        assertThat(xlator.pegTranslate(dna1, 1, dna1.length()), equalTo(prot1));
        // Verify that we can switch to 11.
        xlator = new DnaTranslator(11);
        String prot2 = StringUtils.chop(prot1) + "*";
        assertThat(xlator.pegTranslate(dna1, 1, dna1.length()), equalTo(prot2));
        // Try a bad character.
        dna1 = "angcggatggcttggtcgaccgtgggggcgcacatcgggcagcgaccgggccaggccgca" +
                "taccagatgctggagacccgccgccgtggcagcgtgctgcgactcggcaatcccaagcgg" +
                "ggcatcgtcagccgccgccggtatcacaccctgaggggcgcccgaccaacccgcccgccg" +
                "ccgccgatgctcggctga";
        int len = dna1.length();
        prot1 = "XRMAWSTVGAHIGQRPGQAAYQMLETRRRGSVLRLGNPKRGIVSRRRYHTLRGARPTRPPPPMLG*";
        assertThat(xlator.pegTranslate(dna1, 1, len), equalTo(prot1));
        // Check the substring pathologies.
        dna1 = "abcd" + dna1;
        assertThat(xlator.pegTranslate(dna1, 5, len), equalTo(prot1));
        assertThat(xlator.pegTranslate(dna1, 5, len + 2), equalTo(prot1));
    }

    /**
     * test parm reader
     *
     * @throws IOException
     */
    public void testParms() throws IOException {
        File parmFile = new File("src/test", "parms.tbl");
        List<String> parms = Parms.fromFile(parmFile);
        assertThat("Invalid parms result.", parms, contains("-z", "--bins", "this is a long string",
                "-t", "10", "-tab", "used here"));
    }

    /**
     * test parm iterator
     *
     * @throws IOException
     */
    public void testParms2() throws IOException {
        File parmFile = new File("src/test", "parms2.tbl");
        Parms parmIterator = new Parms(parmFile);
        List<String> parms = parmIterator.next();
        assertThat(parms, contains("--digits", "1", "--constant", "X", "--letters", "a", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "1", "--constant", "X", "--letters", "b", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "1", "--constant", "X", "--letters", "c", "--batch"));
        assertTrue(parmIterator.hasNext());
        assertThat(parmIterator.toString(), equalTo("--digits 1; --letters c"));
        HashMap<String, String> varMap = parmIterator.getVariables();
        assertThat(varMap.get("--digits"), equalTo("1"));
        assertThat(varMap.get("--letters"), equalTo("c"));
        assertThat(varMap.size(), equalTo(2));
        assertThat(parmIterator.getOptions(), arrayContaining("--digits", "--letters"));
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "2", "--constant", "X", "--letters", "a", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "2", "--constant", "X", "--letters", "b", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "2", "--constant", "X", "--letters", "c", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "3", "--constant", "X", "--letters", "a", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "3", "--constant", "X", "--letters", "b", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "3", "--constant", "X", "--letters", "c", "--batch"));
        assertFalse(parmIterator.hasNext());
    }

    /**
     * test integer list
     */
    public void testIntList() {
        // Start with an empty list.
        IntegerList newList = new IntegerList();
        assertThat(newList.size(), equalTo(0));
        assertTrue(newList.isEmpty());
        assertFalse(newList.hasNext());
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        assertThat(newList.last(), equalTo(0));
        // Repeat with an empty string.
        newList = new IntegerList("");
        assertThat(newList.size(), equalTo(0));
        assertTrue(newList.isEmpty());
        assertFalse(newList.hasNext());
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        // Use the default feature.
        newList.setDefault(12);
        assertThat(newList.size(), equalTo(1));
        assertThat(newList.get(0), equalTo(12));
        assertThat(newList.toString(), equalTo("12"));
        assertThat(newList.original(), equalTo("12"));
        assertThat(newList.last(), equalTo(12));
        // Verify out-of-bounds works.
        assertThat(newList.get(-1), equalTo(0));
        assertThat(newList.get(1), equalTo(0));
        // Use a more sophisticated list.
        newList = new IntegerList("4,6,8,10");
        assertThat(newList.size(), equalTo(4));
        assertThat(newList.get(0), equalTo(4));
        assertThat(newList.get(1), equalTo(6));
        assertThat(newList.get(2), equalTo(8));
        assertThat(newList.get(3), equalTo(10));
        assertThat(newList.last(), equalTo(10));
        // Try traversal.
        assertThat(newList.next(), equalTo(4));
        assertThat(newList.next(), equalTo(6));
        assertThat(newList.next(), equalTo(8));
        assertThat(newList.next(), equalTo(10));
        assertThat(newList.next(), equalTo(0));
        // Reset to the first and traverse again, using hasnext checks.
        newList.reset();
        assertTrue(newList.hasNext());
        assertThat(newList.next(), equalTo(4));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), equalTo(6));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), equalTo(8));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), equalTo(10));
        assertFalse(newList.hasNext());
        assertThat(newList.next(), equalTo(0));
        assertThat(newList.toString(), equalTo("4, 6, 8, 10"));
        assertThat(newList.original(), equalTo("4,6,8,10"));
        // Iterate through the list.
        Iterator<Integer> iter = newList.new Iter();
        assertTrue(iter.hasNext());
        assertThat(iter.next(), equalTo(4));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), equalTo(6));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), equalTo(8));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), equalTo(10));
        assertFalse(iter.hasNext());
        // Test softnext.
        newList.reset();
        assertThat(newList.softNext(), equalTo(4));
        assertThat(newList.softNext(), equalTo(6));
        assertThat(newList.softNext(), equalTo(8));
        assertThat(newList.softNext(), equalTo(10));
        assertThat(newList.softNext(), equalTo(10));
        assertThat(newList.softNext(), equalTo(10));
        // Use an array initializer.
        int[] test = new int[] { 3, 5, 7 };
        newList = new IntegerList(test);
        assertThat(newList.size(), equalTo(3));
        assertThat(newList.get(0), equalTo(3));
        assertThat(newList.get(1), equalTo(5));
        assertThat(newList.get(2), equalTo(7));
        assertThat(newList.last(), equalTo(7));
    }

    /**
     * test float list
     */
    public void testFloatList() {
        // Start with an empty list.
        FloatList newList = new FloatList();
        assertThat(newList.size(), equalTo(0));
        assertTrue(newList.isEmpty());
        assertFalse(newList.hasNext());
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        assertThat(newList.last(), equalTo(0.0));
        // Repeat with an empty string.
        newList = new FloatList("");
        assertThat(newList.size(), equalTo(0));
        assertTrue(newList.isEmpty());
        assertFalse(newList.hasNext());
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        // Use the default feature.
        newList.setDefault(1.5);
        assertThat(newList.size(), equalTo(1));
        assertThat(newList.get(0), equalTo(1.5));
        assertThat(newList.toString(), equalTo("1.5"));
        assertThat(newList.original(), equalTo("1.5"));
        assertThat(newList.last(), equalTo(1.5));
        // Verify out-of-bounds works.
        assertThat(newList.get(-1), equalTo(0.0));
        assertThat(newList.get(1), equalTo(0.0));
        // Use a more sophisticated list.
        newList = new FloatList("0.4,0.6,0.8,0.1");
        assertThat(newList.size(), equalTo(4));
        assertThat(newList.get(0), closeTo(0.4, 1e-6));
        assertThat(newList.get(1), closeTo(0.6, 1e-6));
        assertThat(newList.get(2), closeTo(0.8, 1e-6));
        assertThat(newList.get(3), closeTo(0.1, 1e-6));
        assertThat(newList.last(), closeTo(0.1, 1e-6));
        // Try traversal.
        assertThat(newList.next(), closeTo(0.4, 1e-6));
        assertThat(newList.next(), closeTo(0.6, 1e-6));
        assertThat(newList.next(), closeTo(0.8, 1e-6));
        assertThat(newList.next(), closeTo(0.1, 1e-6));
        assertThat(newList.next(), equalTo(0.0));
        // Reset to the first and traverse again, using hasnext checks.
        newList.reset();
        assertTrue(newList.hasNext());
        assertThat(newList.next(), closeTo(0.4, 1e-6));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), closeTo(0.6, 1e-6));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), closeTo(0.8, 1e-6));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), closeTo(0.1, 1e-6));
        assertFalse(newList.hasNext());
        assertThat(newList.next(), equalTo(0.0));
        assertThat(newList.toString(), equalTo("0.4, 0.6, 0.8, 0.1"));
        assertThat(newList.original(), equalTo("0.4,0.6,0.8,0.1"));
        // Iterate through the list.
        Iterator<Double> iter = newList.new Iter();
        assertTrue(iter.hasNext());
        assertThat(iter.next(), closeTo(0.4, 1e-6));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), closeTo(0.6, 1e-6));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), closeTo(0.8, 1e-6));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), closeTo(0.1, 1e-6));
        assertFalse(iter.hasNext());
        // Test softnext.
        newList.reset();
        assertThat(newList.softNext(), closeTo(0.4, 1e-6));
        assertThat(newList.softNext(), closeTo(0.6, 1e-6));
        assertThat(newList.softNext(), closeTo(0.8, 1e-6));
        assertThat(newList.softNext(), closeTo(0.1, 1e-6));
        assertThat(newList.softNext(), closeTo(0.1, 1e-6));
        assertThat(newList.softNext(), closeTo(0.1, 1e-6));
        // Test getValues.
        double[] test = newList.getValues(6);
        assertThat(test.length, equalTo(6));
        assertThat(test[0], closeTo(0.4, 1e-6));
        assertThat(test[1], closeTo(0.6, 1e-6));
        assertThat(test[2], closeTo(0.8, 1e-6));
        assertThat(test[3], closeTo(0.1, 1e-6));
        assertThat(test[4], closeTo(0.1, 1e-6));
        assertThat(test[5], closeTo(0.1, 1e-6));
        test = newList.getValues(2);
        assertThat(test.length, equalTo(2));
        assertThat(test[0], closeTo(0.4, 1e-6));
        assertThat(test[1], closeTo(0.6, 1e-6));
        // Use an array initializer.
        test = new double[] { 3, 5.5, 7 };
        newList = new FloatList(test);
        assertThat(newList.size(), equalTo(3));
        assertThat(newList.get(0), equalTo(3.0));
        assertThat(newList.get(1), equalTo(5.5));
        assertThat(newList.get(2), equalTo(7.0));
        assertThat(newList.last(), equalTo(7.0));
        double[] test2 = newList.getValues();
        assertThat(test2.length, equalTo(test.length));
        for (int i = 0; i < test.length; i++) {
            assertThat(test[i], equalTo(test2[i]));
        }
        // Use a fill initializer.
        newList = new FloatList(1.5, 4);
        assertThat(newList.size(), equalTo(4));
        for (int i = 0; i < 4; i++)
            assertThat(newList.get(i), equalTo(1.5));
    }

    /**
     * Test the balanced stream.
     *
     * @throws IOException
     */
    public void testBalancedStream() throws IOException {
        File testFile = new File("src/test", "balanced.ser");
        BalancedOutputStream outStream = new BalancedOutputStream(1.5, testFile);
        outStream.writeImmediate("type", "text");
        outStream.write("a", "a1");
        outStream.write("a", "a2");
        outStream.write("a", "a3");
        outStream.write("a", "a4");
        outStream.write("a", "a5");
        outStream.write("a", "a6");
        outStream.write("a", "a7");
        outStream.write("b", "b1");
        outStream.write("b", "b2");
        outStream.write("b", "b3");
        outStream.write("b", "b4");
        outStream.write("b", "b5");
        outStream.write("b", "b6");
        outStream.write("b", "b7");
        outStream.write("b", "b8");
        outStream.write("b", "b9");
        outStream.write("b", "b10");
        outStream.write("b", "b11");
        outStream.write("b", "b12");
        outStream.write("b", "b13");
        outStream.write("b", "b14");
        outStream.write("b", "b15");
        outStream.write("b", "b16");
        outStream.write("b", "b17");
        outStream.write("b", "b18");
        outStream.write("b", "b19");
        outStream.write("b", "b20");
        outStream.write("c", "c1");
        outStream.write("c", "c2");
        outStream.write("c", "c3");
        outStream.write("c", "c4");
        outStream.write("c", "c5");
        outStream.write("c", "c6");
        outStream.write("c", "c7");
        outStream.write("c", "c8");
        outStream.close();
        // The file should contain 7 a, 10 b, and 8 c.
        TabbedLineReader reader = new TabbedLineReader(testFile);
        assertThat(reader.findField("type"), equalTo(0));
        assertThat(reader.findField("text"), equalTo(1));
        assertThat(reader.size(), equalTo(2));
        CountMap<String> counts = new CountMap<String>();
        for (Line line : reader) {
            String label = line.get(0);
            String text = line.get(1);
            assertTrue("Text has wrong label.", StringUtils.startsWith(text, label));
            counts.count(label);
        }
        assertThat(counts.getCount("a"), equalTo(7));
        assertThat(counts.getCount("b"), equalTo(10));
        assertThat(counts.getCount("c"), equalTo(8));
        reader.close();
        // Verify the first half of the file has half the objects.
        reader = new TabbedLineReader(testFile);
        counts = new CountMap<String>();
        for (int i = 0; i < 13; i++) {
            Line line = reader.next();
            String label = line.get(0);
            counts.count(label);
        }
        assertThat(counts.getCount("a"), equalTo(4));
        assertThat(counts.getCount("b"), equalTo(5));
        assertThat(counts.getCount("c"), equalTo(4));
        reader.close();
        outStream = new BalancedOutputStream(0, testFile);
        outStream.writeImmediate("type", "text");
        outStream.write("a", "a1");
        outStream.write("a", "a2");
        outStream.write("a", "a3");
        outStream.write("a", "a4");
        outStream.write("b", "b1");
        outStream.write("b", "b2");
        outStream.close();
        reader = new TabbedLineReader(testFile);
        Line line = reader.next();
        assertThat(line.get(0), equalTo("a"));
        assertThat(line.get(1), equalTo("a1"));
        line = reader.next();
        assertThat(line.get(0), equalTo("a"));
        assertThat(line.get(1), equalTo("a2"));
        line = reader.next();
        assertThat(line.get(0), equalTo("a"));
        assertThat(line.get(1), equalTo("a3"));
        line = reader.next();
        assertThat(line.get(0), equalTo("a"));
        assertThat(line.get(1), equalTo("a4"));
        line = reader.next();
        assertThat(line.get(0), equalTo("b"));
        assertThat(line.get(1), equalTo("b1"));
        line = reader.next();
        assertThat(line.get(0), equalTo("b"));
        assertThat(line.get(1), equalTo("b2"));
        assertFalse(reader.hasNext());
        reader.close();
        outStream = new BalancedOutputStream(1.2, testFile);
        BalancedOutputStream.setBufferMax(600);
        outStream.writeImmediate("type", "text");
        for (int i = 0; i < 200; i++) {
            outStream.write("a", "a1");
            outStream.write("b", "b1");
            outStream.write("b", "b1");
        }
        outStream.write("a", "a2");
        outStream.close();
        reader = new TabbedLineReader(testFile);
        int aCount = 0;
        int bCount = 0;
        for (int i = 0; i < 440; i++) {
            line = reader.next();
            String label = line.get(0);
            if (label.equals("a")) {
                aCount++;
                assertThat(line.get(1), equalTo("a1"));
            } else if (label.equals("b")) {
                bCount++;
                assertThat(line.get(1), equalTo("b1"));
            }
        }
        assertThat(aCount, equalTo(200));
        assertThat(bCount, equalTo(240));
        line = reader.next();
        assertThat(line.get(1), equalTo("a2"));
        reader.close();
    }

    /**
     * Test the shuffler
     */
    public void testShuffler() {
        Shuffler<Integer> test1 = new Shuffler<Integer>(100);
        for (int i = 0; i < 100; i++)
            test1.add(i);
        for (int i = 0; i < 100; i++)
            assertThat(test1.get(i), equalTo(i));
        // Test the limited iterator.
        Iterator<Integer> limited = test1.limitedIter(5);
        assertTrue(limited.hasNext());
        assertThat(limited.next(), equalTo(0));
        assertTrue(limited.hasNext());
        assertThat(limited.next(), equalTo(1));
        assertTrue(limited.hasNext());
        assertThat(limited.next(), equalTo(2));
        assertTrue(limited.hasNext());
        assertThat(limited.next(), equalTo(3));
        assertTrue(limited.hasNext());
        assertThat(limited.next(), equalTo(4));
        assertFalse(limited.hasNext());
        // Test the shuffling.
        test1.shuffle(50);
        for (int i = 0; i < 100; i++) {
            assertThat(test1.get(i), lessThan(100));
            assertThat(test1.get(i), greaterThanOrEqualTo(0));
            for (int j = 0; j < 100; j++) {
                if (i != j) assertThat(test1.get(i), not(equalTo(test1.get(j))));
            }
        }
        // Insure the edge cases don't crash.
        test1.shuffle(200);
        test1.shuffle(101);
        test1.shuffle(99);
        // Test adding an iterable.
        ArrayList<Integer> array1 = new ArrayList<Integer>(5);
        array1.add(200);
        array1.add(201);
        array1.add(202);
        array1.add(203);
        array1.add(204);
        Shuffler<Integer> test2 = test1.addSequence(array1);
        assertSame(test1, test2);
        assertThat(test1.get(100), equalTo(200));
        assertThat(test1.get(101), equalTo(201));
        assertThat(test1.get(102), equalTo(202));
        assertThat(test1.get(103), equalTo(203));
        assertThat(test1.get(104), equalTo(204));
        assertThat(test1.size(), equalTo(105));
    }

    /**
     * test protein kmers
     */
    public void testKmers() {
        ProteinKmers.setKmerSize(10);
        String myProt1 = "MGMLVPLISKISDLSEEAKACVAACSSVEELDEVRGRYIGRAGALTALLA"; // 50 AA
        String myProt2 = "MDINLFKEELEELAKKAKHMLNETASKNDLEQVKVSLLGKKGLLTLQSAA";
        String myProt3 = "MDINLFKEELKHMLNETASKKGLLTLQSA"; // 30 AA
        ProteinKmers kmer1 = new ProteinKmers(myProt1);
        ProteinKmers kmer2 = new ProteinKmers(myProt2);
        ProteinKmers kmer3 = new ProteinKmers(myProt3);
        assertEquals("Kmer1 has wrong protein.", myProt1, kmer1.getProtein());
        assertEquals("Kmer1 has wrong count.", 41, kmer1.size());
        assertEquals("Kmer1/kmer3 wrong similarity.", 3, kmer2.similarity(kmer3));
        assertEquals("Similarity not commutative.", 3, kmer3.similarity(kmer2));
        assertEquals("Kmer1 too close to kmer2.", 1.0, kmer1.distance(kmer2), 0.0);
        assertEquals("Kmer1 too close to kmer3.", 0.95, kmer2.distance(kmer3), 0.005);
    }

    /**
     * test GTO updating
     *
     * @throws IOException
     * @throws NumberFormatException
     */
    public void testGTO() throws NumberFormatException, IOException {
        Genome smallGenome = new Genome(new File("src/test", "small.gto"));
        Collection<Contig> contigs = smallGenome.getContigs();
        assertThat(contigs.size(), equalTo(1));
        Contig contig = smallGenome.getContig("161.31.con.0001");
        assertThat(contig.length(), equalTo(1139123));
        Collection<Feature> features = smallGenome.getFeatures();
        assertThat(features.size(), equalTo(1));
        Feature feat = smallGenome.getFeature("fig|161.31.peg.985");
        assertThat(feat.getPlfam(), equalTo("PLF_157_00003322"));
        assertThat(feat.getJsonField("test_extra"), equalTo("extra datum"));
        feat.addAnnotation("Analyze json", "TestLibrary");
        List<Annotation> annotations = feat.getAnnotations();
        assertThat(annotations.get(0).getComment(), equalTo("Add feature from PATRIC"));
        assertThat(annotations.get(0).getAnnotator(), equalTo("PATRIC"));
        assertThat(annotations.get(0).getAnnotationTime(), equalTo(1500218027.5));
        assertThat(annotations.get(1).getComment(), equalTo("Set function to hypothetical protein"));
        assertThat(annotations.get(1).getAnnotator(), equalTo("RAST"));
        assertThat(annotations.get(1).getAnnotationTime(), equalTo(1500218027.75));
        assertThat(annotations.get(2).getComment(), equalTo("Analyze json"));
        assertThat(annotations.get(2).getAnnotator(), equalTo("TestLibrary"));
        String[] lineage = smallGenome.getLineage();
        assertThat(lineage, arrayContaining("131567", "2", "203691", "203692", "136", "137", "157", "160", "161"));
        SortedSet<CloseGenome> closeGenomes = smallGenome.getCloseGenomes();
        CloseGenome curr = closeGenomes.first();
        assertThat(curr.getGenomeId(), equalTo("160.24"));
        assertThat(curr.getCloseness(), equalTo(999.0));
        assertThat(curr.getMethod(), equalTo("minHash"));
        assertThat(curr.getGenomeName(), equalTo("Treponema pallidum strain UW-148B"));
        assertThat(closeGenomes.size(), equalTo(8));
        Iterator<CloseGenome> iter = closeGenomes.iterator();
        curr = iter.next();
        while (iter.hasNext()) {
            CloseGenome next = iter.next();
            assertTrue(curr.compareTo(next) < 0);
        }
        // Now we will add a feature and a contig.
        Feature rna = new Feature("fig|161.31.rna.1", "brand new RNA", "161.31.con.0001", "-", 89101, 90100);
        rna.setPgfam("PGF_RNA1");
        smallGenome.addFeature(rna);
        Contig fakeContig = new Contig("161.31.con.0002", "aaaccctttggg", 11);
        smallGenome.addContig(fakeContig);
        File testFile = new File("src/test", "gto.ser");
        smallGenome.update(testFile);
        Genome testGenome = new Genome(testFile);
        features = testGenome.getFeatures();
        assertThat(features.size(), equalTo(2));
        assertThat(testGenome.getId(), equalTo(smallGenome.getId()));
        assertThat(testGenome.getName(), equalTo(smallGenome.getName()));
        Contig contig2 = testGenome.getContig("161.31.con.0001");
        assertThat(contig2.length(), equalTo(contig.length()));
        contig2 = testGenome.getContig("161.31.con.0002");
        assertThat(contig2.length(), equalTo(12));
        assertThat(contig2.getSequence(), equalTo("aaaccctttggg"));
        feat = testGenome.getFeature("fig|161.31.rna.1");
        assertThat(feat.getPgfam(), equalTo("PGF_RNA1"));
        assertNull(feat.getPlfam());
        Location loc = feat.getLocation();
        assertThat(loc.getContigId(), equalTo("161.31.con.0001"));
        assertThat(loc.getBegin(), equalTo(90100));
        assertThat(loc.getDir(), equalTo('-'));
        assertThat(loc.getLength(), equalTo(1000));
        feat = testGenome.getFeature("fig|161.31.peg.985");
        assertThat(feat.getJsonField("test_extra"), equalTo("extra datum"));
        // Now we test our ability to create other json objects.
        for (CloseGenome close : closeGenomes) {
            JsonObject json = close.toJson();
            assertThat(json.getString(CloseGenome.CloseGenomeKeys.GENOME), equalTo(close.getGenomeId()));
            assertThat(json.getString(CloseGenome.CloseGenomeKeys.GENOME_NAME), equalTo(close.getGenomeName()));
            assertThat(json.getString(CloseGenome.CloseGenomeKeys.ANALYSIS_METHOD), equalTo(close.getMethod()));
            assertThat(json.getDouble(CloseGenome.CloseGenomeKeys.CLOSENESS_MEASURE), equalTo(close.getCloseness()));
        }
        contig2 = new Contig("161.31.con.A", "aaaccctttggg", 11);
        JsonObject contigJson = contig2.toJson();
        assertThat(contigJson.getString(ContigKeys.ID), equalTo("161.31.con.A"));
        assertThat(contigJson.getInteger(ContigKeys.GENETIC_CODE), equalTo(11));
        assertThat(contigJson.getString(ContigKeys.DNA), equalTo("aaaccctttggg"));
        assertThat(contigJson.getInteger(ContigKeys.LENGTH), equalTo(12));
        // Finally test feature deletion.
        testGenome.deAnnotate();
        assertThat(testGenome.getFeatureCount(), equalTo(0));
        int fCount = 0;
        for (@SuppressWarnings("unused") Feature f : testGenome.getFeatures()) fCount++;
        assertThat(fCount, equalTo(0));
    }

    /**
     * test codon sets
     */
    public void testCodonSet() {
        String myDna = "aaattgcaaggacaactgatggagcgctaatagtgactg";
        CodonSet newSet = new CodonSet("ttg", "ctg", "atg");
        assertFalse(newSet.contains(myDna, 1));
        assertTrue(newSet.contains(myDna, 4));
        assertFalse(newSet.contains(myDna, 7));
        assertFalse(newSet.contains(myDna, 10));
        assertFalse(newSet.contains(myDna, 13));
        assertTrue(newSet.contains(myDna, 16));
        assertTrue(newSet.contains(myDna, 19));
        assertFalse(newSet.contains(myDna, 22));
        assertFalse(newSet.contains(myDna, 25));
        assertFalse(newSet.contains(myDna, 28));
        assertFalse(newSet.contains(myDna, 31));
        assertFalse(newSet.contains(myDna, 34));
        assertTrue(newSet.contains(myDna, 37));
    }

    /**
     * test location extension
     * @throws IOException
     */
    public void testExtend() throws IOException {
        // We need a fake genome to test the edge cases.
        Genome fakeGenome = new Genome("12345.6", "Mycobacteria praestrigiae Narnia", "Bacteria", 4);
        String seq = "aaaaaaaaatagaaaatgaaaaaataaaaaaaaaaataaaaaaaactgaaaaaaaaaaaa";
        fakeGenome.addContig(new Contig("c2", seq, 4));
        seq = "aaattaaaaaaaaaactaaaaaaaaaacataaattgaaaaaaaaataaaaaaaaaaatagaaaaaa";
        fakeGenome.addContig(new Contig("c3", seq, 4));
        seq = "aaaaaaaaaaaacagaaaaaattattgaaatgaaaaaaaaaatagttaaaaaaaaaaaaaaaa";
        fakeGenome.addContig(new Contig("c1", seq, 4));
        // Test codon scan.
        assertFalse(Location.containsCodon(new CodonSet("caa", "cag", "cat"), seq, 16, 54));
        assertTrue(Location.containsCodon(new CodonSet("cag", "acg"), seq, 1, 22));
        assertFalse(Location.containsCodon(new CodonSet("cag", "acg"), seq, 1, 15));
        assertFalse(Location.containsCodon(new CodonSet("cag", "acg"), seq, 2, 44));
        // Test edge before stop on - strand.
        Location loc = Location.create("c1", "-", 4, 15);
        assertNull(loc.extend(fakeGenome));
        // Insure that a GC 11 stop is not found in GC 4.
        loc = Location.create("c1", "+", 37, 39);
        Location loc2 = loc.extend(fakeGenome);
        assertThat(loc2.getLeft(), equalTo(25));
        assertThat(loc2.getRight(), equalTo(45));
        // Test edge before start on - strand.
        loc = Location.create("c1",  "-", 52, 54);
        assertNull(loc.extend(fakeGenome));
        // Test edge before stop on + strand.
        loc = Location.create("c2", "+", 52, 54);
        assertNull(loc.extend(fakeGenome));
        // Test edge before start on + strand.
        loc = Location.create("c2", "+", 10, 12);
        // Test internal stops.
        loc = Location.create("c3",  "+",  34, 63);
        assertNull(loc.extend(fakeGenome));
        loc = Location.create("c3", "-", 7, 27);
        assertNull(loc.extend(fakeGenome));
        // Stress test a real genome.
        Genome gto = new Genome(new File("src/test", "testLocs.gto"));
        // Test the plus strand.
        loc = Location.create("1313.7001.con.0049", "+", 63492, 64652); /* peg 1978 */
        loc2 = loc.extend(gto);
        assertThat(loc2.getBegin(), equalTo(63480));
        assertThat(loc2.getLength(), equalTo(1461));
        // Confirm that plus extensions are stable.
        Location loc3 = loc2.extend(gto);
        assertThat(loc3.getBegin(), equalTo(63480));
        assertThat(loc3.getLength(), equalTo(1461));
        // Test the minus strand.
        loc = Location.create("1313.7001.con.0034", "-", 32559, 33290); /* peg 1409 */
        loc2 = loc.extend(gto);
        assertThat(loc2.getBegin(), equalTo(33302));
        assertThat(loc2.getLength(), equalTo(846));
        // Confirm that minus extensions are stable.
        loc3 = loc2.extend(gto);
        assertThat(loc3.getBegin(), equalTo(33302));
        assertThat(loc3.getLength(), equalTo(846));
        loc = Location.create("1313.7001.con.0034", "-", 44250, 45737); /* peg 1423 */
        loc2 = loc.extend(gto);
        assertThat(loc2.getBegin(), equalTo(45749));
        assertThat(loc2.getLength(), equalTo(1701));
    }

    /**
     * test the sorted location list
     */
    public void testSortedLocations() {
        // Create some test locations.
        Location loc1 = Location.create("c1", "+", 100, 200);
        Location loc2 = Location.create("c1", "-", 150, 250);
        Location loc3 = Location.create("c1", "+", 1000, 2000);
        Location loc4 = Location.create("c1", "+", 1100, 1500);
        Location loc5 = Location.create("c1", "-", 1100, 1500);
        Location loc6 = Location.create("c2", "+", 600, 700);
        Location loc7 = Location.create("c2", "-", 600, 800);
        // Start building the list.
        SortedLocationList locList = new SortedLocationList(loc7, loc2, loc3, loc4, loc1);
        assertThat(locList.get(0), sameInstance(loc1));
        assertThat(locList.get(1), sameInstance(loc2));
        assertThat(locList.get(2), sameInstance(loc3));
        assertThat(locList.get(3), sameInstance(loc4));
        assertThat(locList.get(4), sameInstance(loc7));
        assertNull(locList.get(5));
        locList.addAll(loc5, loc6, loc3);
        assertThat(locList.get(0), sameInstance(loc1));
        assertThat(locList.get(1), sameInstance(loc2));
        assertThat(locList.get(2), sameInstance(loc3));
        assertThat(locList.get(3), sameInstance(loc3));
        assertThat(locList.get(4), sameInstance(loc4));
        assertThat(locList.get(5), sameInstance(loc5));
        assertThat(locList.get(6), sameInstance(loc6));
        assertThat(locList.get(7), sameInstance(loc7));
        Iterator<Location> iter = locList.iterator();
        Location old = iter.next();
        while (iter.hasNext()) {
            Location loc = iter.next();
            assertTrue(old.compareTo(loc) <= 0);
            old = loc;
        }
        // Test sublists.
        List<Location> subList = locList.contigRange(3);
        assertThat(subList, contains(loc3, loc4, loc5));
        subList = locList.contigRange(6);
        assertThat(subList, contains(loc6, loc7));
        // Test contains.  This location is a dup of loc1.
        old = Location.create("c1", "+", 100, 200);
        assertTrue(locList.contains(old));
        assertThat(locList.size(), equalTo(8));
        Location[] locs = locList.toArray();
        assertThat(locs[0], sameInstance(loc1));
        assertThat(locs[1], sameInstance(loc2));
        assertThat(locs[2], sameInstance(loc3));
        assertThat(locs[3], sameInstance(loc3));
        assertThat(locs[4], sameInstance(loc4));
        assertThat(locs[5], sameInstance(loc5));
        assertThat(locs[6], sameInstance(loc6));
        assertThat(locs[7], sameInstance(loc7));
        locList.clear();
        assertThat(locList.size(), equalTo(0));
        iter = locList.iterator();
        assertFalse(iter.hasNext());
        locList.add(loc4);
        assertThat(locList.size(), equalTo(1));
        assertThat(locList.get(0), equalTo(loc4));
    }
}
