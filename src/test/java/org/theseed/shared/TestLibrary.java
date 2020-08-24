/**
 *
 */
package org.theseed.shared;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.theseed.counters.KeyPair;
import org.theseed.genome.Annotation;
import org.theseed.genome.CloseGenome;
import org.theseed.genome.Contig;
import org.theseed.genome.Contig.ContigKeys;
import org.theseed.genome.Coupling;
import org.theseed.genome.Feature;
import org.theseed.genome.FeatureList;
import org.theseed.genome.Genome;
import org.theseed.genome.GenomeDirectory;
import org.theseed.genome.GoTerm;
import org.theseed.genome.SubsystemRow;
import org.theseed.genome.TaxItem;
import org.theseed.io.Shuffler;
import org.theseed.locations.FLocation;
import org.theseed.locations.Frame;
import org.theseed.locations.Location;
import org.theseed.magic.MagicMap;
import org.theseed.proteins.CodonSet;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.proteins.kmers.KmerCollectionGroup;
import org.theseed.sequence.FastaInputStream;
import org.theseed.sequence.FastaOutputStream;
import org.theseed.sequence.MD5Hex;
import org.theseed.sequence.ProteinKmers;
import org.theseed.sequence.Sequence;
import com.github.cliftonlabs.json_simple.JsonObject;
import junit.framework.TestCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Bruce Parrello
 *
 */
public class TestLibrary extends TestCase {

    private static final String GO_STRING = "GO:0033925|mannosyl-glycoprotein endo-beta-N-acetylglucosaminidase activity";

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
    @SuppressWarnings("unlikely-arg-type")
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
            assertNull("Wrong ID found", magicTable.getItem(thingId));
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
        assertSame("Modified thing did not read back.", newThing, magicTable.getItem("3KetoLGulo6PhosDeca6"));
        modifiedThing = "Unique (new) thing string without numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for unique thing.", "UniqThinStriWith", newThing.getId());
        assertSame("Unique thing did not read back.", newThing, magicTable.getItem("UniqThinStriWith"));
        Thing findThing = magicTable.findOrInsert(modifiedThing);
        assertSame("Unique thing was re-inserted.", newThing, findThing);
        modifiedThing = "Unique (old) thing string without numbers";
        findThing = magicTable.findOrInsert(modifiedThing);
        assertTrue("Parenthetical did not change thing ID.", findThing != newThing);
        assertEquals("Wrong ID assigned for parenthetical thing.", "UniqThinStriWith2", findThing.getId());
        assertSame("Parenthetical thing did not read back.", findThing, magicTable.getItem("UniqThinStriWith2"));
        modifiedThing = "Unique (newer) thing string without numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for newer thing.", "UniqThinStriWith3", newThing.getId());
        assertSame("Parenthetical thing did not read back.", newThing, magicTable.getItem("UniqThinStriWith3"));
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
        for (Thing oldThing : magicTable.objectValues()) {
            newThing = newTable.getItem(oldThing.getId());
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

        // Test map interface
        Map<String, String> thingMap = magicTable;
        assertFalse(thingMap.containsKey(myThing));
        assertTrue(thingMap.containsKey("PhenTrnaSyntAlph"));
        assertFalse(thingMap.containsKey("FrogsAndToads"));
        assertFalse(thingMap.containsValue(myThing));
        assertTrue(thingMap.containsValue("Unique thing string 12345 with more numbers"));
        assertFalse(thingMap.containsValue("Obviously fake role name"));
        assertThat(thingMap.get("PhenTrnaSyntAlph"), equalTo("Phenylalanyl-tRNA synthetase alpha chain (EC 6.1.1.20)"));
        assertNull(thingMap.get("FrogsAndToads"));
        assertNull(thingMap.get(myThing));
        thingMap.remove("PhenTrnaSyntAlph");
        assertFalse(thingMap.containsKey("PhenTrnaSyntAlph"));
        assertNull(thingMap.remove("FrogsAndToads"));
        Collection<String> values = thingMap.values();
        for (String value : values)
            assertNotNull(value, magicTable.getByName(value));
        for (String key : magicTable.keySet()) {
            Thing target = magicTable.getItem(key);
            assertTrue(key, values.contains(target.getName()));
        }
        Set<Map.Entry<String, String>> entries = thingMap.entrySet();
        assertThat(entries.size(), equalTo(magicTable.size()));
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            Thing target = magicTable.getItem(key);
            assertNotNull(key, target);
            assertThat(entry.getValue(), equalTo(target.getName()));
        }
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
        assertEquals("Incorrect source.", "PATRIC", this.myGto.getSource());
        assertEquals("Incorrect source ID.", "1313.7001", this.myGto.getSourceId());
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
        assertTrue("PEG not a CDS", myFeature.isProtein());
        assertThat(myFeature.getGoTerms().size(), equalTo(0));
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
        assertFalse("Non-peg typed as CDS", myFeature.isProtein());
        assertEquals("Incorrect DNA retrieval by location.", myDna2, this.myGto.getDna(myLoc));
        // For fun, we check a feature with GO terms.
        myFeature = this.myGto.getFeature("fig|1313.7001.peg.975");
        GoTerm[] goTerms = new GoTerm[2];
        assertThat(myFeature.getGoTerms().size(), equalTo(2));
        goTerms = myFeature.getGoTerms().toArray(goTerms);
        assertThat(goTerms[0].getNumber(), equalTo(46820));
        assertThat(goTerms[0].getDescription(), equalTo("4-amino-4-deoxychorismate synthase activity"));
        assertThat(goTerms[1].getNumber(), equalTo(8696));
        assertThat(goTerms[1].getDescription(), equalTo("4-amino-4-deoxychorismate lyase activity"));
        // Get this feature's subsystems.
        Collection<String> subsystems = myFeature.getSubsystems();
        assertThat(subsystems, contains("Folate Biosynthesis"));
        SubsystemRow subsystem = myGto.getSubsystem("Invalid subsystem");
        assertThat(subsystem, nullValue());
        subsystem = myGto.getSubsystem("Folate Biosynthesis");
        assertThat(subsystem.getName(), equalTo("Folate Biosynthesis"));
        assertThat(subsystem.getClassifications(), contains("Metabolism", "Cofactors, Vitamins, Prosthetic Groups", "Folate and pterines"));
        assertThat(subsystem.getVariantCode(), equalTo("active"));
        assertTrue(subsystem.isActive());
        List<SubsystemRow.Role> roles = subsystem.getRoles();
        assertThat(roles.size(), equalTo(23));
        assertThat(roles.get(0).getName(), equalTo("2-amino-4-hydroxy-6-hydroxymethyldihydropteridine pyrophosphokinase (EC 2.7.6.3)"));
        assertThat(roles.get(1).getName(), equalTo("5-formyltetrahydrofolate cyclo-ligase (EC 6.3.3.2)"));
        assertThat(roles.get(2).getName(), equalTo("ATPase component of general energizing module of ECF transporters"));
        Set<Feature> bound = roles.get(2).getFeatures();
        assertThat(bound.size(), equalTo(2));
        assertThat(bound, containsInAnyOrder(myGto.getFeature("fig|1313.7001.peg.1717"), myGto.getFeature("fig|1313.7001.peg.1718")));
        // Now iterate over the proteins.
        for (Feature feat : this.myGto.getPegs())
            assertTrue("Feature" + feat.getId() + " is not a PEG.", feat.isProtein());
        // Iterate over all features to make sure they have the correct parent.
        for (Feature feat : this.myGto.getFeatures())
            assertThat("Feature " + feat + " does not have the correct parent.", feat.getParent(), equalTo(myGto));
        // Test the taxonomy.
        int[] taxonomy = new int[] {1313, 1301, 1300, 186826, 91061,
                                       1239, 1783272, 2, 131567};
        int i = 0;
        Iterator<TaxItem> iter = this.myGto.taxonomy();
        while (iter.hasNext()) {
            TaxItem item = iter.next();
            assertThat(item.getId(), equalTo(taxonomy[i]));
            i++;
        }
        assertThat(i, equalTo(taxonomy.length));
        assertTrue(myGto.hasContigs());
    }

    /**
     * Test location DNA fetch
     */
    public void testLocationDna() {
        String dna = myGto.getContig("1313.7001.con.0001").getSequence();
        Location loc = myGto.getFeature("fig|1313.7001.peg.1").getLocation();
        String locDna = loc.getDna(dna);
        assertThat(locDna, equalTo("ttggttgcaggtctaacaaatggtgaattaatcgctccaatgacttacgaagagacgatg" +
                "acgagcgacttttttgaagtatggtttcagaaatttctcttaccaacattaaccacacca" +
                "tcggttattattatggataatgcaagattccatagaatgggtaagctagaactcttgtgt" +
                "gaagagtttgggcataaacttttacctcttcctccctactcacctgagtacaatcctatt" +
                "gagaaaacaggctctttgtcaactgtagtgggttga"));
        String dnaR = Contig.reverse(dna);
        Location loc2 = loc.converse(dna.length());
        assertThat(loc2.getDna(dnaR), equalTo(locDna));
        Location loc3 = loc2.converse(dna.length());
        assertThat(loc3.getDna(dna), equalTo(locDna));
        assertThat(loc3, equalTo(loc));

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
        String[] expected = new String[] { "1005394.4", "1313.7001", "1313.7002", "1313.7016", "243277.26" };
        int i = 0;
        for (Genome genome : gDir) {
            assertEquals("Incorrect result for genome at position " + i + ".", expected[i], genome.getId());
            assertEquals("Incorrect file for genome at position " + i + ".", expected[i] + ".gto", gDir.currFile().getName());
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
        assertTrue(contigFeatures.isOccupied(region));
        region = Location.create("1313.7001.con.0029", "-", 160, 6860);
        assertTrue(contigFeatures.isOccupied(region));
        assertThat("Not all expected features found.", fids,
                hasItems("fig|1313.7001.peg.1244", "fig|1313.7001.peg.1245", "fig|1313.7001.peg.1246",
                         "fig|1313.7001.peg.1249", "fig|1313.7001.peg.1250", "fig|1313.7001.peg.1251"));
        inRegion = contigFeatures.inRegion(0, 100);
        assertEquals("Error at left extreme.", 1, inRegion.size());
        inRegion = contigFeatures.inRegion(14000, 15000);
        assertEquals("Error at right extreme.", 0, inRegion.size());
        region = Location.create("1313.7001.con.0029", "+", 14000, 15000);
        assertFalse(contigFeatures.isOccupied(region));
        region = Location.create("1313.7001.con.0029", "-", 5100, 6500);
        assertTrue(contigFeatures.isOccupied(region));
        region = Location.create("1313.7001.con.0029", "+", 5100, 6500);
        assertFalse(contigFeatures.isOccupied(region));
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
        assertTrue(rObj5.matches("(r)-2-hydroxyacid dehydrogenase, similar to L-sulfolactate dehydrogenase"));
        assertTrue(rObj5.matches("(R)-2-hydroxyacid dehydrogenase similar to L-sulfolactate Dehydrogenase"));
        assertFalse(rObj5.matches("(R)-3-hydroxyacid dehydrogenase similar to L-sulfolactate Dehydrogenase"));
        assertFalse(rObj4.matches("Phenylalanyl tRNA synthetase alpha chain"));
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
            assertNull("Wrong ID found", magicTable.getItem(roleId));
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
        assertSame("Modified role did not read back.", newRole, magicTable.getItem("3KetoLGulo6PhosDeca6"));
        modifiedRole = "Unique (new) role string without numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for unique role.", "UniqRoleStriWith", newRole.getId());
        assertSame("Unique role did not read back.", newRole, magicTable.getItem("UniqRoleStriWith"));
        Role findRole = magicTable.findOrInsert(modifiedRole);
        assertSame("Unique role was re-inserted.", newRole, findRole);
        modifiedRole = "Unique (old) role string without numbers";
        findRole = magicTable.findOrInsert(modifiedRole);
        assertTrue("Parenthetical did not change role ID.", findRole != newRole);
        assertEquals("Wrong ID assigned for parenthetical role.", "UniqRoleStriWith2", findRole.getId());
        assertSame("Parenthetical role did not read back.", findRole, magicTable.getItem("UniqRoleStriWith2"));
        modifiedRole = "Unique (newer) role string without numbers";
        newRole = magicTable.findOrInsert(modifiedRole);
        assertEquals("Wrong ID assigned for newer role.", "UniqRoleStriWith3", newRole.getId());
        assertSame("Parenthetical role did not read back.", newRole, magicTable.getItem("UniqRoleStriWith3"));
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
        for (Role oldRole : magicTable.objectValues()) {
            newRole = newTable.getItem(oldRole.getId());
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
        assertThat(feat.getFunction(), equalTo("hypothetical function"));
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
        assertThat(feat.getGoTerms().size(), equalTo(0));
        feat.addGoTerm("GO:0008696");
        feat.addGoTerm("GO:0046820|4-amino-4-deoxychorismate synthase activity");
        assertThat(feat.getGoTerms().size(), equalTo(2));
        feat.formAlias("gi|", "");
        assertThat(feat.getAliases().size(), equalTo(0));
        feat.formAlias("gi|", "1234");
        feat.addAlias("geneAlias");
        feat.formAlias("", "swissThing");
        assertThat(feat.getAliases().size(), equalTo(3));
        String[] aliases = new String[3];
        aliases = feat.getAliases().toArray(aliases);
        assertThat(aliases[0], equalTo("gi|1234"));
        assertThat(aliases[1], equalTo("geneAlias"));
        assertThat(aliases[2], equalTo("swissThing"));
        GoTerm[] goTerms = new GoTerm[2];
        goTerms = feat.getGoTerms().toArray(goTerms);
        assertThat(goTerms[0].getNumber(), equalTo(8696));
        assertNull(goTerms[0].getDescription());
        assertThat(goTerms[1].getNumber(), equalTo(46820));
        assertThat(goTerms[1].getDescription(), equalTo("4-amino-4-deoxychorismate synthase activity"));
        Feature f2 = new Feature("fig|161.31.peg.10", "more hypothetical function", "c2", "+", 110, 220);
        assertThat(feat.compareTo(f2), equalTo(0));
        f2 = new Feature("fig|161.31.peg.2", "less hypothetical function", "c2", "+", 120, 240);
        assertThat(feat.compareTo(f2), greaterThan(0));
        f2 = new Feature("fig|161.31.peg.20", "strange hypothetical function", "c2", "-", 220, 540);
        assertThat(feat.compareTo(f2), lessThan(0));
        feat.setFunction("totally new function");
        assertThat(feat.getFunction(), equalTo("totally new function"));
        Location loc1 = Location.create("c1", "+", 200, 300);
        feat = new Feature("fig|161.31.peg.10", "hypothetical function", loc1);
        loc = feat.getLocation();
        assertThat(loc.getContigId(), equalTo("c1"));
        assertThat(loc.getDir(), equalTo('+'));
        assertThat(loc.getLeft(), equalTo(200));
        assertThat(loc.getRight(), equalTo(300));
        assertThat(loc, equalTo(loc1));
        assertThat(feat.getFunction(), equalTo("hypothetical function"));
        feat.addAnnotation("this is an annotation", 158257708, "RAST");
        feat.addAnnotation("this is another annotation", 158257710, "master");
        List<Annotation> annoList = feat.getAnnotations();
        Annotation anno = annoList.get(0);
        assertThat(anno.getAnnotationTime(), equalTo(158257708.0));
        assertThat(anno.getAnnotator(), equalTo("RAST"));
        assertThat(anno.getComment(), equalTo("this is an annotation"));
        anno = annoList.get(1);
        assertThat(anno.getAnnotationTime(), equalTo(158257710.0));
        assertThat(anno.getAnnotator(), equalTo("master"));
        assertThat(anno.getComment(), equalTo("this is another annotation"));
        assertThat(annoList.size(), equalTo(2));
        feat.addCoupling("fig|161.31.peg.20", 10, 44.3);
        feat.addCoupling("fig|161.31.peg.21", 5, 56.8);
        feat.addCoupling("fig|161.31.peg.21", 6, 56.5);
        feat.addCoupling("fig|161.31.peg.30", 40, 10.25);
        Coupling[] couplings = feat.getCouplings();
        assertThat(couplings.length, equalTo(3));
        assertThat(couplings[0].getTarget(), equalTo("fig|161.31.peg.21"));
        assertThat(couplings[0].getSize(), equalTo(6));
        assertThat(couplings[0].getStrength(), closeTo(56.5, 0.005));
        assertThat(couplings[1].getTarget(), equalTo("fig|161.31.peg.20"));
        assertThat(couplings[2].getTarget(), equalTo("fig|161.31.peg.30"));
        feat.clearCouplings();
        couplings = feat.getCouplings();
        assertThat(couplings.length, equalTo(0));
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
        Integer[] lineage = ArrayUtils.toObject(smallGenome.getLineage());
        assertThat(lineage, arrayContaining(131567, 2, 203691, 203692, 136, 137, 157, 160, 161));
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
        rna.addCoupling("fig|161.31.rna.2", 20, 20.5);
        rna.addCoupling("fig|161.31.rna.3", 30, 30.5);
        smallGenome.addFeature(rna);
        assertThat(rna.getParent(), equalTo(smallGenome));
        Contig fakeContig = new Contig("161.31.con.0002", "aaaccctttggg", 11);
        fakeContig.setAccession("fakeAcc");
        fakeContig.setDescription("fake description");
        assertThat(fakeContig.getAccession(), equalTo("fakeAcc"));
        assertThat(fakeContig.getDescription(), equalTo("fake description"));
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
        assertThat(contig2.getAccession(), equalTo("fakeAcc"));
        assertThat(contig2.getDescription(), equalTo("fake description"));
        feat = testGenome.getFeature("fig|161.31.rna.1");
        assertThat(feat.getPgfam(), equalTo("PGF_RNA1"));
        assertNull(feat.getPlfam());
        Coupling[] couplings = feat.getCouplings();
        assertThat(couplings.length, equalTo(2));
        assertThat(couplings[0].getTarget(), equalTo("fig|161.31.rna.3"));
        assertThat(couplings[0].getSize(), equalTo(30));
        assertThat(couplings[0].getStrength(), closeTo(30.5, 0.001));
        assertThat(couplings[1].getTarget(), equalTo("fig|161.31.rna.2"));
        assertThat(couplings[1].getSize(), equalTo(20));
        assertThat(couplings[1].getStrength(), closeTo(20.5, 0.001));
        Location loc = feat.getLocation();
        assertThat(loc.getContigId(), equalTo("161.31.con.0001"));
        assertThat(loc.getBegin(), equalTo(90100));
        assertThat(loc.getDir(), equalTo('-'));
        assertThat(loc.getLength(), equalTo(1000));
        feat = testGenome.getFeature("fig|161.31.peg.985");
        assertThat(feat.getJsonField("test_extra"), equalTo("extra datum"));
        assertThat(feat.getCouplings().length, equalTo(0));
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
        // Test feature deletion.
        testGenome.deAnnotate();
        assertThat(testGenome.getFeatureCount(), equalTo(0));
        int fCount = 0;
        for (@SuppressWarnings("unused") Feature f : testGenome.getFeatures()) fCount++;
        assertThat(fCount, equalTo(0));
        // Test metagenome binning properties.
        assertNull(testGenome.getBinRefGenomeId());
        assertThat(testGenome.getBinCoverage(), equalTo(0.0));
        testGenome = new Genome(new File("src/test", "bin3.gto"));
        assertThat(testGenome.getBinRefGenomeId(), equalTo("43675.15"));
        assertThat(testGenome.getBinCoverage(), closeTo(173.920, 0.0005));
        Genome testOther = new Genome(new File("src/test", "bin3a.gto"));
        assertTrue(testGenome.identical(testGenome));
        assertFalse(testGenome.identical(null));
        assertFalse(testGenome.identical(testOther));
        assertFalse(testGenome.identical(smallGenome));
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
     * Compare PERL MD5 to java
     *
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public void testMD5() throws NoSuchAlgorithmException, IOException {
        MD5Hex mdComputer = new MD5Hex();
        String prot = "MLHIKPYLVNQKTLHEIEKAIKKAKPNVTINSKDSELICSIPEPTAEIREEKIKNSKQVVEETRIALRTKRQDLLKKFKS";
        assertThat(mdComputer.checksum(prot), equalTo("54ce0e56c20f6eee543f1995ae7d7dcc"));
        prot = "MEHTFPEILALAFVGGLVLNLMPCVFPILSLKVLSIVRKSSKSRWSTAVDGVYYTAGVMSSMLLLSLVLILLRSAGHFLGWGFQMQSPALVIGLLHVTFLVGMSFSGFLDLSIKVPFVDAMTAHNVGSFFAGVLSALIGTPCSAPFMVSAVSFALLQPGLRSVAIFQVMGLGMALPYLIISCSPGLTRLLPKPGRWMEYLKQFLAFPMYATSAWLLHVLVSQKGTHVLLPTVLSIVAVSLGVWFLRVMSNVKMQASKALAVLLPLFVVGTAIYIGRFRGHDHAYGELAVVEFSEARLARLLRKDKTVFLSVGAEWCLTCKVNEKVLESASVQSFLRTHGVIYMKADWTNMDSTIAEYLSEHGGGGVPFYELYVNGKSVGPMPQIFSEKTLLEILGKHLNANPSSKASPE";
        assertThat(mdComputer.checksum(prot), equalTo("a5b29797d99fceda08c5bb913473529f"));
        prot = "MITHNPVIATVTDRVIRIDGGKIVEDYRNPNPVSIDSLTNL";
        assertThat(mdComputer.checksum(prot), equalTo("9e36013f41acf2443643cdcf178bda67"));
        Genome coreGenome = new Genome(new File("src/test", "360106.5.gto"));
        String md5 = mdComputer.sequenceMD5(coreGenome);
        assertThat(md5, equalTo("9606255e9c598c259f96a74083d87a35"));
        File testFile = File.createTempFile("test", ".fasta", new File("src/test"));
        coreGenome.saveDna(testFile);
        testFile.deleteOnExit();
        try (FastaInputStream gDnaStream = new FastaInputStream(testFile)) {
            assertThat(mdComputer.sequenceMD5(gDnaStream), equalTo(md5));
        }
    }

    /**
     * Test contig update.
     *
     * @throws IOException
     */

    private static final String OLD_CONTIG = "NODE_199_length_26254_cov_134.112";
    private static final String NEW_CONTIG = "NODE_NEW";
    private static final String OFF_PEG_ID = "fig|43675.763.peg.443";
    private static final String OFF_CONTIG = "NODE_291_length_19898_cov_244.229";

    public void testContigUpdate() throws IOException {
        Genome myGenome = new Genome(new File("src/test", "bin3.gto"));
        FeatureList contigFeatures = myGenome.getContigFeatures(OLD_CONTIG);
        Contig contig = myGenome.getContig(OLD_CONTIG);
        myGenome.updateContigId(contig, NEW_CONTIG);
        assertThat(myGenome.getFeature(OFF_PEG_ID).getLocation().getContigId(), equalTo(OFF_CONTIG));
        for (Feature feat : contigFeatures)
            assertThat(feat.getLocation().getContigId(), equalTo(NEW_CONTIG));
        assertThat(contig.getId(), equalTo(NEW_CONTIG));
    }

    /**
     * Test GO terms.
     */
    public void testGoTerms() {
        GoTerm go = new GoTerm(33925);
        assertThat(go.toString(), equalTo("GO:0033925"));
        assertThat(go.getNumber(), equalTo(33925));
        assertNull(go.getDescription());
        GoTerm goBig = new GoTerm(GO_STRING);
        GoTerm goBig2 = new GoTerm(GO_STRING);
        assertThat(goBig.toString(), equalTo(GO_STRING));
        assertThat(goBig.getDescription(), equalTo("mannosyl-glycoprotein endo-beta-N-acetylglucosaminidase activity"));
        assertThat(goBig, not(equalTo(go)));
        assertThat(goBig, equalTo(goBig2));
        goBig.setDescription("fake description");
        assertThat(goBig.getDescription(), equalTo("fake description"));
        assertThat(goBig, not(equalTo(goBig2)));
    }

    /**
     * Test GTO save/load
     * @throws IOException
     */
    public void testSave() throws IOException {
        File gtoFile = new File("src/test", "gto2.ser");
        Genome gto = new Genome(new File("src/test/gto_test", "243277.26.gto"));
        gto.purify();
        gto.update(gtoFile);
        Genome diskGenome = new Genome(gtoFile);
        assertThat(diskGenome.getId(), equalTo(gto.getId()));
        assertThat(diskGenome.getName(), equalTo(gto.getName()));
        assertThat(diskGenome.getDomain(), equalTo(gto.getDomain()));
        assertThat(ArrayUtils.toObject(diskGenome.getLineage()), arrayContaining(ArrayUtils.toObject(gto.getLineage())));
        assertThat(diskGenome.getGeneticCode(), equalTo(gto.getGeneticCode()));
        assertThat(diskGenome.getTaxonomyId(), equalTo(gto.getTaxonomyId()));
        assertThat(diskGenome.getFeatureCount(), equalTo(gto.getFeatureCount()));
        assertThat(diskGenome.getContigCount(), equalTo(gto.getContigCount()));
        Collection<Feature> fids = gto.getFeatures();
        for (Feature fid : fids) {
            Feature diskFid = diskGenome.getFeature(fid.getId());
            assertThat(diskFid.getFunction(), equalTo(fid.getFunction()));
            assertThat(diskFid.getLocation(), equalTo(fid.getLocation()));
            assertThat(diskFid.getPlfam(), equalTo(fid.getPlfam()));
            assertThat(diskFid.getType(), equalTo(fid.getType()));
            assertThat(diskFid.getProteinTranslation(), equalTo(fid.getProteinTranslation()));
            Collection<GoTerm> fidGoTerms = fid.getGoTerms();
            assertThat(diskFid.getGoTerms().size(), equalTo(fidGoTerms.size()));
            for (GoTerm diskGoTerm : diskFid.getGoTerms()) {
                assertThat(fidGoTerms, hasItem(diskGoTerm));
            }
            Collection<Annotation> fidAnnotations = fid.getAnnotations();
            assertThat(diskFid.getAnnotations().size(), equalTo(fidAnnotations.size()));
            for (Annotation diskAnnotation : diskFid.getAnnotations()) {
                assertThat(fidAnnotations, hasItem(diskAnnotation));
            }
            Collection<String> fidAliases = fid.getAliases();
            assertThat(diskFid.getAliases().size(), equalTo(fidAliases.size()));
            for (String diskAlias : diskFid.getAliases()) {
                assertThat(fidAliases, hasItem(diskAlias));
            }
        }
        Collection<Contig> contigs = gto.getContigs();
        for (Contig contig : contigs) {
            Contig diskContig = diskGenome.getContig(contig.getId());
            assertThat(diskContig.length(), equalTo(contig.length()));
            assertThat(diskContig.getSequence(), equalTo(contig.getSequence()));
        }
        Collection<SubsystemRow> subsystems = gto.getSubsystems();
        for (SubsystemRow subsystem : subsystems) {
            SubsystemRow diskSubsystem = diskGenome.getSubsystem(subsystem.getName());
            assertThat(diskSubsystem.getVariantCode(), equalTo(subsystem.getVariantCode()));
            assertThat(diskSubsystem.getClassifications(), contains(subsystem.getClassifications().toArray()));
            List<SubsystemRow.Role> roles = subsystem.getRoles();
            List<SubsystemRow.Role> diskRoles = diskSubsystem.getRoles();
            assertThat(roles.size(), equalTo(diskRoles.size()));
            for (int i = 0; i < roles.size(); i++) {
                SubsystemRow.Role role = roles.get(i);
                SubsystemRow.Role diskRole = diskRoles.get(i);
                assertThat(role.getName(), equalTo(diskRole.getName()));
                assertThat(diskRole.getSubName(), equalTo(diskSubsystem.getName()));
                Set<Feature> roleFeatures = role.getFeatures();
                Set<Feature> diskFeatures = diskRole.getFeatures();
                assertThat(roleFeatures.size(), equalTo(diskFeatures.size()));
                Set<String> roleFids = roleFeatures.stream().map(k -> k.getId()).collect(Collectors.toSet());
                for (Feature feat : diskFeatures)
                    assertThat(roleFids, hasItem(feat.getId()));
            }
        }

    }

    /**
     * Test ad hoc genome creation.
     */
    public void testGtoCreation() {
        Genome testGto = new Genome("1.1", "small test genome", "Virus", 1);
        assertThat(testGto.getId(), equalTo("1.1"));
        assertThat(testGto.getName(), equalTo("small test genome"));
        assertThat(testGto.getDomain(), equalTo("Virus"));
        assertThat(testGto.getGeneticCode(), equalTo(1));
        assertThat(testGto.getFeatureCount(), equalTo(0));
        assertThat(testGto.getContigCount(), equalTo(0));
        testGto.setName("New test name");
        assertThat(testGto.getName(), equalTo("New test name"));
        testGto.setId("2.2");
        assertThat(testGto.getId(), equalTo("2.2"));
        TaxItem[] dummyLineage = new TaxItem[] { new TaxItem(10239, "Virus", "superkingdom"), new TaxItem(2559587, "Riboviria", "no rank"),
                new TaxItem(76804, "Nidovirales", "order") };
        testGto.setLineage(dummyLineage);
        assertThat(testGto.getTaxonomyId(), equalTo(76804));
        Iterator<TaxItem> taxonomy = testGto.taxonomy();
        TaxItem taxon = taxonomy.next();
        assertThat(taxon.getId(), equalTo(76804));
        assertThat(taxon.getName(), equalTo("Nidovirales"));
        assertThat(taxon.getRank(), equalTo("order"));
        taxon = taxonomy.next();
        assertThat(taxon.getId(), equalTo(2559587));
        assertThat(taxon.getName(), equalTo("Riboviria"));
        assertThat(taxon.getRank(), equalTo("no rank"));
        taxon = taxonomy.next();
        assertThat(taxon.getId(), equalTo(10239));
        assertThat(taxon.getName(), equalTo("Virus"));
        assertThat(taxon.getRank(), equalTo("superkingdom"));
        Contig contig = new Contig("contig1", "AAAAAGGGGGCCCCCTTTTT", 1);
        testGto.addContig(contig);
        contig = new Contig("contig2", "AAAACCCCGGGGTTTT", 1);
        testGto.addContig(contig);
        testGto.setGeneticCode(11);
        assertThat(testGto.getGeneticCode(), equalTo(11));
        for (Contig contig0 : testGto.getContigs()) {
            assertThat(contig0.getGeneticCode(), equalTo(11));
            String seq = contig0.getSequence();
            assertThat(seq.toLowerCase(), equalTo(seq));
        }
        Feature peg1 = new Feature("fig|1.1.peg.1", "test function", "contig1", "+", 10, 100);
        Feature peg2 = new Feature("fig|1.1.peg.2", "test function 2", "contig1", "-", 200, 300);
        Feature peg3 = new Feature("fig|1.1.peg.3", "test function 2", "contig1", "+", 300, 400);
        testGto.addFeature(peg1);
        testGto.addFeature(peg2);
        testGto.addFeature(peg3);
        SubsystemRow subsystem = new SubsystemRow(testGto, "Funny subsystem");
        subsystem.setVariantCode("likely");
        assertThat(subsystem.getName(), equalTo("Funny subsystem"));
        assertThat(subsystem.getGenome(), equalTo(testGto));
        assertThat(testGto.getSubsystem("Funny subsystem"), equalTo(subsystem));
        assertThat(subsystem.getVariantCode(), equalTo("likely"));
        assertFalse(subsystem.isActive());
        subsystem.setClassifications("big stuff", "middle stuff", "");
        assertThat(subsystem.getClassifications(), contains("big stuff", "middle stuff", ""));
        Shuffler<String> newClasses = new Shuffler<String>(3).add1("super-class").add1("class").add1("sub-class");
        subsystem.setClassifications(newClasses);
        assertThat(subsystem.getClassifications(), contains("super-class", "class", "sub-class"));
        SubsystemRow.Role role1 = subsystem.addRole("Funny role 1");
        subsystem.addFeature("Funny role 1", "fig|1.1.peg.1");
        assertThat(peg2.getSubsystems().size(), equalTo(0));
        SubsystemRow.Role role2 = subsystem.addRole("Funny role 2");
        subsystem.addFeature("Funny role 2", "fig|1.1.peg.2");
        subsystem.addFeature("Funny role 2", "fig|1.1.peg.3");
        assertThat(peg1.getSubsystems(), contains("Funny subsystem"));
        assertThat(peg2.getSubsystems(), contains("Funny subsystem"));
        assertThat(peg3.getSubsystems(), contains("Funny subsystem"));
        Set<Feature> feats = role1.getFeatures();
        assertThat(feats.size(), equalTo(1));
        Set<String> fids = feats.stream().map(k -> k.getId()).collect(Collectors.toSet());
        assertThat(fids, contains("fig|1.1.peg.1"));
        feats = role2.getFeatures();
        assertThat(feats.size(), equalTo(2));
        fids = feats.stream().map(k -> k.getId()).collect(Collectors.toSet());
        assertThat(fids, containsInAnyOrder("fig|1.1.peg.2", "fig|1.1.peg.3"));
    }

    /*
     * test FASTA saving
     */
    public void testFastas() throws IOException {
        File fastaTemp = new File("src/test", "fasta.ser");
        myGto.saveDna(fastaTemp);
        int counter = 0;
        try (FastaInputStream fastaStream = new FastaInputStream(fastaTemp)) {
            for (Sequence contigSeq : fastaStream) {
                Contig genomeContig = myGto.getContig(contigSeq.getLabel());
                assertThat(contigSeq.getLabel(), genomeContig, not(nullValue()));
                assertThat(contigSeq.getLabel(), contigSeq.getSequence(), equalTo(genomeContig.getSequence()));
                assertThat(contigSeq.getLabel(), contigSeq.getComment(), equalTo(genomeContig.getDescription()));
                counter++;
            }
            assertThat(counter, equalTo(myGto.getContigCount()));
        }
        myGto.savePegs(fastaTemp);
        counter = 0;
        try (FastaInputStream fastaStream = new FastaInputStream(fastaTemp)) {
            for (Sequence pegSeq : fastaStream) {
                Feature genomePeg = myGto.getFeature(pegSeq.getLabel());
                assertThat(pegSeq.getLabel(), genomePeg, not(nullValue()));
                assertThat(pegSeq.getLabel(), pegSeq.getComment(), equalTo(genomePeg.getFunction()));
                assertThat(pegSeq.getLabel(), pegSeq.getSequence(), equalTo(genomePeg.getProteinTranslation()));
                counter++;
            }
            assertThat(counter, equalTo(myGto.getPegs().size()));
        }
        myGto.saveFeatures(fastaTemp);
        counter = 0;
        try (FastaInputStream fastaStream = new FastaInputStream(fastaTemp)) {
            for (Sequence fidSeq : fastaStream) {
                Feature genomeFid = myGto.getFeature(fidSeq.getLabel());
                assertThat(fidSeq.getLabel(), genomeFid, not(nullValue()));
                assertThat(fidSeq.getLabel(), fidSeq.getComment(), equalTo(genomeFid.getFunction()));
                String dna = myGto.getDna(genomeFid.getLocation());
                assertThat(fidSeq.getLabel(), fidSeq.getSequence(), equalTo(dna));
                counter++;
            }
            assertThat(counter, equalTo(myGto.getFeatures().size()));
        }
        myGto.saveFeatures(fastaTemp, "CDS");
        counter = 0;
        try (FastaInputStream fastaStream = new FastaInputStream(fastaTemp)) {
            for (Sequence fidSeq : fastaStream) {
                Feature genomeFid = myGto.getFeature(fidSeq.getLabel());
                assertThat(genomeFid.getType(), equalTo("CDS"));
                assertThat(fidSeq.getLabel(), genomeFid, not(nullValue()));
                assertThat(fidSeq.getLabel(), fidSeq.getComment(), equalTo(genomeFid.getFunction()));
                String dna = myGto.getDna(genomeFid.getLocation());
                assertThat(fidSeq.getLabel(), fidSeq.getSequence(), equalTo(dna));
                counter++;
            }
            assertThat(counter, equalTo(myGto.getPegs().size()));
        }
    }

    /**
     * test feature type indexing
     *
     * @throws NumberFormatException
     */
    public void testFTypeIdNums() throws NumberFormatException {
        int nextPeg = myGto.getNextIdNum("peg");
        int nextRna = myGto.getNextIdNum("rna");
        int nextPoi = myGto.getNextIdNum("poi");
        int nextRepeat = myGto.getNextIdNum("repeat");
        assertThat(nextPoi, equalTo(1));
        for (Feature feat : myGto.getFeatures()) {
            // Get the feature index.
            String fid = feat.getId();
            int idx = Integer.parseUnsignedInt(StringUtils.substringAfterLast(fid, "."));
            switch (feat.getType()) {
            case "CDS" :
                assertThat(fid, idx, lessThan(nextPeg));
                break;
            case "rna" :
                assertThat(fid, idx, lessThan(nextRna));
                break;
            case "repeat" :
                assertThat(fid, idx, lessThan(nextRepeat));
                break;
            }
        }
    }

}
