/**
 *
 */
package org.theseed.shared;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
import org.theseed.magic.MagicObject;
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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Bruce Parrello
 *
 */
public class TestLibrary {

    private static final String GO_STRING = "GO:0033925|mannosyl-glycoprotein endo-beta-N-acetylglucosaminidase activity";

    private static Genome myGto = null;

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TestLibrary.class);


    public TestLibrary() throws IOException {
        log.info("Creating test genome.");
        myGto = new Genome(new File("data/gto_test", "1313.7001.gto"));
    }

    /**
     * test sequence group kmer distance
     *
     * @throws IOException
     */
    @Test
    public void testKmerCollectionGroup() throws IOException {
        ProteinKmers.setKmerSize(8);
        KmerCollectionGroup kGroup = new KmerCollectionGroup();
        File inFile = new File("data", "seq_list.fa");
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
    @Test
    public void testMagic() throws IOException {
        File inFile = new File("data", "words.txt");
        Scanner thingScanner = new Scanner(inFile);
        thingScanner.useDelimiter("\t|\r\n|\n");
        while (thingScanner.hasNext()) {
            String condensed = thingScanner.next();
            String full = thingScanner.next();
            assertThat("String did not condense.", MagicMap.condense(full), equalTo(condensed));
        }
        thingScanner.close();
        // Test registration
        ThingMap magicTable = new ThingMap();
        inFile = new File("data", "things.txt");
        thingScanner = new Scanner(inFile);
        thingScanner.useDelimiter("\t|\r\n|\n");
        while (thingScanner.hasNext()) {
            String thingId = thingScanner.next();
            thingScanner.next();
            assertThat("Wrong ID found", magicTable.getItem(thingId), nullValue());
            String thingDesc = thingScanner.next();
            Thing newThing = new Thing(thingId, thingDesc);
            magicTable.register(newThing);
            assertThat("Registered ID did not read back.", magicTable.getName(thingId), equalTo(thingDesc));
        }
        thingScanner.close();
        assertThat("PheS not found.", magicTable.containsKey("PhenTrnaSyntAlph"), equalTo(true));
        assertThat("Known bad key found.", magicTable.containsKey("PhenTrnaSyntGamm"), equalTo(false));
        String modifiedThing = "3-keto-L-gulonate-6-phosphate decarboxylase UlaK putative (L-ascorbate utilization protein D) (EC 4.1.1.85)";
        Thing newThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Wrong ID assigned for modified thing.", newThing.getId(), equalTo("3KetoLGulo6PhosDeca6"));
        assertThat("Modified thing did not read back.", magicTable.getItem("3KetoLGulo6PhosDeca6"),sameInstance(newThing));
        modifiedThing = "Unique (new) thing string without numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Wrong ID assigned for unique thing.", newThing.getId(), equalTo("UniqThinStriWith"));
        assertThat("Unique thing did not read back.", magicTable.getItem("UniqThinStriWith"),sameInstance(newThing));
        Thing findThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Unique thing was re-inserted.", findThing,sameInstance(newThing));
        modifiedThing = "Unique (old) thing string without numbers";
        findThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Parenthetical did not change thing ID.", findThing != newThing, equalTo(true));
        assertThat("Wrong ID assigned for parenthetical thing.", findThing.getId(), equalTo("UniqThinStriWith2"));
        assertThat("Parenthetical thing did not read back.", magicTable.getItem("UniqThinStriWith2"),sameInstance(findThing));
        modifiedThing = "Unique (newer) thing string without numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Wrong ID assigned for newer thing.", newThing.getId(), equalTo("UniqThinStriWith3"));
        assertThat("Parenthetical thing did not read back.", magicTable.getItem("UniqThinStriWith3"),sameInstance(newThing));
        modifiedThing = "Unique thing string 12345 with numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Name not stored in thing.", newThing.getName(), equalTo(modifiedThing));
        assertThat("Wrong ID assigned for numbered thing.", newThing.getId(), equalTo("UniqThinStri1234n1"));
        modifiedThing = "Unique thing string 12345 with more numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Wrong ID assigned for second numbered thing.", newThing.getId(), equalTo("UniqThinStri1234n2"));
        // Add two aliases.
        magicTable.addAlias("PhenTrnaSyntDoma", "alias second Phenylalanyl role string");
        modifiedThing = "ALIAS Second phenylalanyl role string";
        Thing myThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Alias 1 did not work.", myThing.getId(), equalTo("PhenTrnaSyntDoma"));
        magicTable.addAlias("PhenTrnaSyntDoma", "alias third Phenylalanyl role string");
        modifiedThing = "Phenylalanyl-tRNA synthetase domain protein (Bsu YtpR)";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Original string did not work.", myThing.getId(), equalTo(newThing.getId()));
        modifiedThing = "alias second Phenylalanyl role string";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Alias 1 string did not work.", myThing,sameInstance(newThing));
        modifiedThing = "alias third Phenylalanyl role string";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertThat("Alias 2 string did not work.", myThing.getId(), equalTo(newThing.getId()));

        // Test save and load.
        File saveFile = new File("data", "things.ser");
        magicTable.save(saveFile);
        ThingMap newTable = ThingMap.load(saveFile);
        for (Thing oldThing : magicTable.objectValues()) {
            newThing = newTable.getItem(oldThing.getId());
            assertThat("Could not find thing in loaded table.", newThing, not(nullValue()));
            if (! oldThing.getName().startsWith("alias")) {
                assertThat("Loaded table has wrong thing name.", newThing.getName(), equalTo(oldThing.getName()));
                assertThat("Loaded thing has wrong checksum.", newThing, equalTo(oldThing));
            }
            newThing = newTable.getByName(oldThing.getName());
            assertThat("Could not find thing by name in loaded table.", newThing, not(nullValue()));
            assertThat("Found incorrect object by name in loaded table.", newThing.getId(), equalTo(oldThing.getId()));
        }
        modifiedThing = "Phenylalanyl-tRNA synthetase domain protein (Bsu YtpR)";
        myThing = newTable.findOrInsert(modifiedThing);
        modifiedThing = "alias second Phenylalanyl role string";
        newThing = newTable.findOrInsert(modifiedThing);
        assertThat("Alias 1 string did not work.", myThing.getId(), equalTo(newThing.getId()));
        modifiedThing = "alias third Phenylalanyl role string";
        newThing = newTable.findOrInsert(modifiedThing);
        assertThat("Alias 2 string did not work.", myThing.getId(), equalTo(newThing.getId()));

        // Test map interface
        Map<String, String> thingMap = magicTable;
        assertThat(thingMap.containsKey(myThing), equalTo(false));
        assertThat(thingMap.containsKey("PhenTrnaSyntAlph"), equalTo(true));
        assertThat(thingMap.containsKey("FrogsAndToads"), equalTo(false));
        assertThat(thingMap.containsValue(myThing), equalTo(false));
        assertThat(thingMap.containsValue("Unique thing string 12345 with more numbers"), equalTo(true));
        assertThat(thingMap.containsValue("Obviously fake role name"), equalTo(false));
        assertThat(thingMap.get("PhenTrnaSyntAlph"), equalTo("Phenylalanyl-tRNA synthetase alpha chain (EC 6.1.1.20)"));
        assertThat(thingMap.get("FrogsAndToads"), nullValue());
        assertThat(thingMap.get(myThing), nullValue());
        thingMap.remove("PhenTrnaSyntAlph");
        assertThat(thingMap.containsKey("PhenTrnaSyntAlph"), equalTo(false));
        assertThat(thingMap.remove("FrogsAndToads"), nullValue());
        Collection<String> values = thingMap.values();
        for (String value : values)
            assertThat(value, magicTable.getByName(value), not(nullValue()));
        for (String key : magicTable.keySet()) {
            Thing target = magicTable.getItem(key);
            assertThat(key, values.contains(target.getName()), equalTo(true));
        }
        Set<Map.Entry<String, String>> entries = thingMap.entrySet();
        assertThat(entries.size(), equalTo(magicTable.size()));
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            Thing target = magicTable.getItem(key);
            assertThat(key, target, not(nullValue()));
            assertThat(entry.getValue(), equalTo(target.getName()));
        }
        // Test the iterator.
        int count = 0;
        Set<String> phenSet = new HashSet<String>();
        for (Thing thing : magicTable) {
            count++;
            if (thing.getId().contentEquals("PhenTrnaSyntDoma")) {
                phenSet.add(thing.getName());
            }
        }
        assertThat(phenSet, containsInAnyOrder("alias second Phenylalanyl role string", "alias third Phenylalanyl role string",
                "Phenylalanyl-tRNA synthetase domain protein (Bsu YtpR)"));
        assertThat(count, equalTo(11462));
    }

    private static final String myProtein = "MNERYQCLKTKEYQALLSSKGRQIFAKRKIDMKSVFGQIKVCLGYKRCHLRGKRQVRIDMGFILMANNLLKYNKRKRQN";
    private static final String myDna1 = "atgaatgaacgttaccagtgtttaaaaactaaagaatatcaggcacttttatcttccaagggtagacaaattttcgctaaacgtaagattgatatgaaatctgtctttgggcagataaaggtttgtttgggttataagagatgtcatctgagaggtaagcgtcaagtgagaattgacatgggattcatactcatggccaacaacctgctgaaatataataagagaaagaggcaaaattaa";
    private static final String myDna2 = "aaatagatttcaaaatgataaaaacgcatcctatcaggtttgagtgaacttgataggatgcgttttagaatgtcaaaattaattgagtttg";


    /**
     * Main test of genomes.
     * @throws FileNotFoundException
     * @throws JsonException
     */
    @Test
    public void testGenome()
    {
        assertThat("Genome ID not correct.", myGto.getId(), equalTo("1313.7001"));
        assertThat("Genome name not correct.", myGto.getName(), equalTo("Streptococcus pneumoniae P210774-233"));
        assertThat("Nonexistent feature found.", myGto.getFeature("fig|1313.7001.cds.75"), nullValue());
        assertThat("Incorrect contig count.", myGto.getContigCount(), equalTo(52));
        assertThat("Incorrect length.", myGto.getLength(), equalTo(2101113));
        assertThat("Genome home not correct", myGto.getHome(), equalTo("PATRIC"));
        assertThat("Incorrect source.", myGto.getSource(), equalTo("PATRIC"));
        assertThat("Incorrect source ID.", myGto.getSourceId(), equalTo("1313.7001"));
        Feature f1 = myGto.getByFunction("16S rRNA (guanine(966)-N(2))-methyltransferase");
        assertThat(f1.getId(), equalTo("fig|1313.7001.peg.145"));
        f1 = myGto.getByFunction("hypothetical nonexistent thing");
        assertThat(f1, nullValue());
        f1 = myGto.getByFunction("Oxidoreductase");
        assertThat(f1.getId(), equalTo("fig|1313.7001.peg.920"));
        String testLink = myGto.genomeLink().render();
        assertThat(testLink, containsString("bv-brc"));
        assertThat(testLink, containsString("1313.7001"));
        testLink = myGto.featureLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, containsString("bv-brc"));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        testLink = myGto.featureRegionLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, containsString("bv-brc"));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        myGto.setHome("CORE");
        testLink = myGto.genomeLink().render();
        assertThat(testLink, containsString("core.theseed"));
        assertThat(testLink, containsString("1313.7001"));
        testLink = myGto.featureLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, containsString("core.theseed"));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        testLink = myGto.featureRegionLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, containsString("core.theseed"));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        myGto.setHome("none");
        testLink = myGto.genomeLink().render();
        assertThat(testLink, not(containsString("<a")));
        assertThat(testLink, containsString("1313.7001"));
        testLink = myGto.featureLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, not(containsString("<a")));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        testLink = myGto.featureRegionLink("fig|1313.7001.peg.758").render();
        assertThat(testLink, not(containsString("<a")));
        assertThat(testLink, containsString("fig|1313.7001.peg.758"));
        // Now we need to pull out a PEG and ask about it.
        Feature myFeature = myGto.getFeature("fig|1313.7001.peg.758");
        assertThat("Sample feature not found.", myFeature, not(nullValue()));
        assertThat("Incorrect feature found.", myFeature.getId(), equalTo("fig|1313.7001.peg.758"));
        assertThat("Incorrect function in sample feature.", myFeature.getFunction(), equalTo("Transposase, IS4 family"));
        assertThat(myFeature.getPegFunction(), equalTo("Transposase, IS4 family"));
        assertThat("Incorrect protein for sample feature.", myFeature.getProteinTranslation(), equalTo(myProtein));
        assertThat("Incorrect protein for sample feature (seqtype).", Feature.SeqType.PROTEIN.get(myFeature), equalTo(myProtein));
        assertThat("Incorrect protein length for sample feature.", myFeature.getProteinLength(), equalTo(myProtein.length()));
        assertThat("Incorrect DNA for sample feature.", myGto.getDna("fig|1313.7001.peg.758"), equalTo(myDna1));
        assertThat("Incorrect DNA for sample feature.", Feature.SeqType.DNA.get(myFeature), equalTo(myDna1));
        assertThat("Incorrect local family for sample feature.", myFeature.getPlfam(), equalTo("PLF_1301_00010583"));
        assertThat("Incorrect global family for sample feature.", myFeature.getPgfam(), equalTo("PGF_07475842"));
        assertThat("PEG not a CDS", myFeature.isProtein(), equalTo(true));
        assertThat(myFeature.getGoTerms().size(), equalTo(0));
        // Next the location.
        Location myLoc = myFeature.getLocation();
        assertThat("Incorrect contig for feature.", myLoc.getContigId(), equalTo("1313.7001.con.0017"));
        assertThat("Incorrect left for feature.", myLoc.getLeft(), equalTo(30698));
        assertThat("Incorrect right for feature", myLoc.getRight(), equalTo(30937));
        assertThat("Incorrect begin for feature.", myLoc.getBegin(), equalTo(30937));
        assertThat("Incorrect length for feature.", myLoc.getLength(), equalTo(240));
        assertThat("Incorrect strand for feature.", myLoc.getDir(), equalTo('-'));
        assertThat("Segmentation flag failure.", myLoc.isSegmented(), equalTo(false));
        // Now we check a segmented location.
        myFeature = myGto.getFeature("fig|1313.7001.repeat_unit.238");
        assertThat("Incorrect DNA for segmented feature.", myGto.getDna(myFeature.getId()), equalTo(myDna2));
        myLoc = myFeature.getLocation();
        assertThat("Incorrect contig for segmented location.", myLoc.getContigId(), equalTo("1313.7001.con.0018"));
        assertThat("Incorrect left for segmented location.", myLoc.getLeft(), equalTo(11908));
        assertThat("Incorrect right for segmented location.", myLoc.getRight(), equalTo(12116));
        assertThat("Incorrect begin for feature.", myLoc.getBegin(), equalTo(11908));
        assertThat("Incorrect length for feature.", myLoc.getLength(), equalTo(209));
        assertThat("Incorrect strand for segmented location.", myLoc.getDir(), equalTo('+'));
        assertThat("Segmentation flag failure.", myLoc.isSegmented(), equalTo(true));
        assertThat("Non-peg typed as CDS", myFeature.isProtein(), equalTo(false));
        assertThat("Incorrect DNA retrieval by location.", myGto.getDna(myLoc), equalTo(myDna2));
        // For fun, we check a feature with GO terms.
        myFeature = myGto.getFeature("fig|1313.7001.peg.975");
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
        assertThat(subsystem.isActive(), equalTo(true));
        List<SubsystemRow.Role> roles = subsystem.getRoles();
        assertThat(roles.size(), equalTo(23));
        assertThat(roles.get(0).getName(), equalTo("2-amino-4-hydroxy-6-hydroxymethyldihydropteridine pyrophosphokinase (EC 2.7.6.3)"));
        assertThat(roles.get(1).getName(), equalTo("5-formyltetrahydrofolate cyclo-ligase (EC 6.3.3.2)"));
        assertThat(roles.get(2).getName(), equalTo("ATPase component of general energizing module of ECF transporters"));
        Set<Feature> bound = roles.get(2).getFeatures();
        assertThat(bound.size(), equalTo(2));
        assertThat(bound, containsInAnyOrder(myGto.getFeature("fig|1313.7001.peg.1717"), myGto.getFeature("fig|1313.7001.peg.1718")));
        // Now iterate over the proteins.
        for (Feature feat : myGto.getPegs())
            assertThat("Feature" + feat.getId() + " is not a PEG.", feat.isProtein(), equalTo(true));
        // Iterate over all features to make sure they have the correct parent.
        for (Feature feat : myGto.getFeatures())
            assertThat("Feature " + feat + " does not have the correct parent.", feat.getParent(), equalTo(myGto));
        // Test the taxonomy.
        int[] taxonomy = new int[] {1313, 1301, 1300, 186826, 91061,
                                       1239, 1783272, 2, 131567};
        int i = 0;
        Iterator<TaxItem> iter = myGto.taxonomy();
        while (iter.hasNext()) {
            TaxItem item = iter.next();
            assertThat(item.getId(), equalTo(taxonomy[i]));
            i++;
        }
        assertThat(i, equalTo(taxonomy.length));
        assertThat(myGto.hasContigs(), equalTo(true));
        // Test contig sorting.
        List<Contig> contigs = myGto.getContigs().stream().sorted().collect(Collectors.toList());
        for (int j = 1; j < contigs.size(); j++)
            assertThat(contigs.get(0).getId(), lessThan(contigs.get(1).getId()));
    }

    /**
     * Test location DNA fetch
     */
    @Test
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
     * Test peg-function method.
     *
     * @throws IOException
     */
    @Test
    public void testPegFunctions() throws IOException {
        Genome gto = new Genome(new File("data", "123214.3.gto"));
        for (Feature peg : gto.getPegs()) {
            String fun = peg.getFunction();
            if (fun != null && ! fun.isEmpty())
                assertThat(peg.getPegFunction(), equalTo(fun));
        }
        Feature feat = gto.getFeature("fig|123214.3.peg.179");
        feat.setFunction("");
        assertThat(feat.getPegFunction(), equalTo("hypothetical protein"));
    }

    /**
     * Test genome directories
     *
     * @throws IOException
     */
    @Test
    public void testGenomeDir() throws IOException {
        GenomeDirectory gDir = new GenomeDirectory("data/gto_test");
        assertThat("Wrong number of genomes found.", gDir.size(), equalTo(5));
        // Run through an iterator.  We know the genome IDs, we just need to find them in order.
        String[] expected = new String[] { "1005394.4", "1313.7001", "1313.7002", "1313.7016", "243277.26" };
        int i = 0;
        for (Genome genome : gDir) {
            assertThat("Incorrect result for genome at position " + i + ".", genome.getId(), equalTo(expected[i]));
            assertThat("Incorrect file for genome at position " + i + ".", gDir.currFile().getName(), equalTo(expected[i] + ".gto"));
            i++;
            String gString = genome.toJsonString();
            assertThat(gString.contains("\n"), equalTo(false));
            Genome genome2 = Genome.fromJson(gString);
            assertThat(genome2.getId(), equalTo(genome.getId()));
            assertThat(genome2.getContigCount(), equalTo(genome.getContigCount()));
            for (Contig contig2 : genome2.getContigs()) {
                String contigID = contig2.getId();
                Contig contig = genome.getContig(contigID);
                assertThat(contig2.getDescription(), equalTo(contig.getDescription()));
                assertThat(contig2.getSequence(), equalTo(contig.getSequence()));
                assertThat(contig2.getAccession(), equalTo(contig.getAccession()));
            }
            assertThat(genome2.getFeatureCount(), equalTo(genome.getFeatureCount()));
            for (Feature feature2 : genome2.getFeatures()) {
                String fid = feature2.getId();
                Feature feature = genome.getFeature(fid);
                assertThat(feature2.getAliases(), equalTo(feature.getAliases()));
                assertThat(feature2.getAnnotations(), equalTo(feature.getAnnotations()));
                assertThat(feature2.getCouplings(), equalTo(feature.getCouplings()));
                assertThat(feature2.getFunction(), equalTo(feature.getFunction()));
                assertThat(feature2.getGoTerms(), equalTo(feature.getGoTerms()));
                assertThat(feature2.getLocation(), equalTo(feature.getLocation()));
                assertThat(feature2.getPegFunction(), equalTo(feature.getPegFunction()));
                assertThat(feature2.getPgfam(), equalTo(feature.getPgfam()));
                assertThat(feature2.getPlfam(), equalTo(feature.getPlfam()));
                assertThat(feature2.getProteinTranslation(), equalTo(feature.getProteinTranslation()));
            }
        }
    }

    /**
     * Test the feature-by-location sort
     * @throws IOException
     * @throws NumberFormatException
     */
    @Test
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
                assertThat("Feature is on wrong contig.", current.getContigId(), equalTo(contig.getId()));
                assertThat("Feature is out of order.", current.getBegin() >= previous.getBegin(), equalTo(true));
            }
            assertThat("Contig has wrong length.", contig.length(), equalTo(contig.getSequence().length()));
        }
        assertThat("Feature count incorrect.", featureCount, equalTo(myGto.getFeatureCount()));
        Genome gto2 = new Genome(new File("data", "testLocs.gto"));
        FeatureList contigFeatures = new FeatureList(gto2, "1313.7001.con.0029");
        Location region = Location.create("1313.7001.con.0029", "+", 160, 6860);
        Collection<Feature> inRegion = contigFeatures.inRegion(160, 6860);
        ArrayList<String> fids = new ArrayList<String>(inRegion.size());
        for (Feature feat : inRegion) {
            fids.add(feat.getId());
            assertThat("Feature " + feat + " not in region.", region.distance(feat.getLocation()), equalTo(-1));
        }
        assertThat(contigFeatures.isOccupied(region), equalTo(true));
        region = Location.create("1313.7001.con.0029", "-", 160, 6860);
        assertThat(contigFeatures.isOccupied(region), equalTo(true));
        assertThat("Not all expected features found.", fids,
                hasItems("fig|1313.7001.peg.1244", "fig|1313.7001.peg.1245", "fig|1313.7001.peg.1246",
                         "fig|1313.7001.peg.1249", "fig|1313.7001.peg.1250", "fig|1313.7001.peg.1251"));
        inRegion = contigFeatures.inRegion(0, 100);
        assertThat("Error at left extreme.", inRegion.size(), equalTo(1));
        inRegion = contigFeatures.inRegion(14000, 15000);
        assertThat("Error at right extreme.", inRegion.size(), equalTo(0));
        region = Location.create("1313.7001.con.0029", "+", 14000, 15000);
        assertThat(contigFeatures.isOccupied(region), equalTo(false));
        region = Location.create("1313.7001.con.0029", "-", 5100, 6500);
        assertThat(contigFeatures.isOccupied(region), equalTo(true));
        region = Location.create("1313.7001.con.0029", "+", 5100, 6500);
        assertThat(contigFeatures.isOccupied(region), equalTo(false));
        inRegion = contigFeatures.inRegion(12000, 15000);
        assertThat("Count error at right edge.", inRegion.size(), equalTo(2));
        fids.clear();
        for (Feature feat : inRegion) fids.add(feat.getId());
        assertThat("Incorrect feature found at right edge.", fids,
                containsInAnyOrder("fig|1313.7001.peg.1256", "fig|1313.7001.peg.1257"));
        contigFeatures = new FeatureList(gto2, "1313.7001.con.0003");
        inRegion = contigFeatures.inRegion(2000, 16000);
        assertThat("Features found in empty contig.", inRegion.size(), equalTo(0));
        contigFeatures = new FeatureList(gto2, "1313.7001.con.0025");
        inRegion = contigFeatures.inRegion(0, 2000);
        assertThat("Features found in empty left edge.", inRegion.size(), equalTo(0));

    }

    /**
     * Test the key pairs.
     */
    @Test
    public void testKeyPair() {
        Thing t1 = new Thing("T1", "first thing");
        Thing t2 = new Thing("T2", "second thing");
        Thing t3 = new Thing("T3", "third thing");
        KeyPair<Thing> p12 = new KeyPair<Thing>(t1, t2);
        KeyPair<Thing> p21 = new KeyPair<Thing>(t2, t1);
        KeyPair<Thing> p13 = new KeyPair<Thing>(t1, t3);
        assertThat("Equality is not commutative.", p21, equalTo(p12));
        assertThat("Hash codes are not commutative.", p21.hashCode(), equalTo(p12.hashCode()));
        assertThat("Different things compare equal.", p13.equals(p12), equalTo(false));
        assertThat("Wrong left value.", p12.getLeft(),sameInstance(t1));
        assertThat("Wrong right value.", p12.getRight(),sameInstance(t2));
    }


    /**
     * Create a genome and check it for couplings.
     */
    @Test
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
        assertThat("Incorrect ID in fake genome.", fakeGenome.getId(), equalTo("12345.6"));
        assertThat("Incorrect name in fake genome.", fakeGenome.getName(), equalTo("Bacillus praestrigiae Narnia"));
        assertThat("Incorrect domain in fake genome.", fakeGenome.getDomain(), equalTo("Bacteria"));
        assertThat("Incorrect genetic code in fake genome.", fakeGenome.getGeneticCode(), equalTo(11));
        Collection<Contig> contigs0 = fakeGenome.getContigs();
        assertThat("Wrong number of contigs in fake genome.", contigs0.size(), equalTo(1));
        Contig[] contigs = new Contig[1];
        contigs = contigs0.toArray(contigs);
        assertThat("Incorrect contig ID in fake contig.", contigs[0].getId(), equalTo("con1"));
        assertThat("Incorrect genetic code in fake contig.", contigs[0].getGeneticCode(), equalTo(11));
        assertThat("Incorrect sequence in fake contig.", contigs[0].getSequence(), equalTo("agct"));
        Collection<Feature> features = fakeGenome.getFeatures();
        assertThat("Incorrect number of features in fake genome.", features.size(), equalTo(10));
        int found = 0;
        for (Feature feat : features) {
            assertThat("Incorrect contig in " + feat + ".", feat.getLocation().isContig("con1"), equalTo(true));
            if (feat.getId().contentEquals("fig|12345.6.peg.1")) {
                assertThat("Wrong role in peg.1.", feat.getFunction(), equalTo("Role 1"));
                Location loc = feat.getLocation();
                assertThat("Wrong strand in peg 1.", loc.getDir(), equalTo('+'));
                assertThat("Wrong left in peg 1.", loc.getLeft(), equalTo(100));
                assertThat("Wrong right in peg 1.", loc.getRight(), equalTo(300));
                found++;
            }
        }
        assertThat("Test feature not found.", found, equalTo(1));
        // Create a feature list for the contig and do a coupling iteration.
        FeatureList contigFeatures = fakeGenome.getContigFeatures("con1");
        FeatureList.Position pos = contigFeatures.new Position();
        assertThat("End-of-list at beginning.", pos.hasNext(), equalTo(true));
        Feature current = pos.next();
        assertThat("Wrong feature first.", current.getId(), equalTo("fig|12345.6.peg.1"));
        Collection<Feature> neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg1.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                containsInAnyOrder("fig|12345.6.peg.2", "fig|12345.6.peg.3"));
        neighbors = pos.within(1000);
        assertThat("Wrong neighbors found at 1000 for peg1.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                containsInAnyOrder("fig|12345.6.peg.2", "fig|12345.6.peg.3",
                        "fig|12345.6.peg.4", "fig|12345.6.peg.5"));
        assertThat("End of list after peg 1", pos.hasNext(), equalTo(true));
        current = pos.next();
        assertThat("Wrong feature after peg 1.", current.getId(), equalTo("fig|12345.6.peg.2"));
        neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg2.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.3"));
        current = pos.next();
        assertThat("Wrong feature after peg 2.", current.getId(), equalTo("fig|12345.6.peg.3"));
        neighbors = pos.within(100);
        assertThat("Found neighbors at 100 for peg3.", neighbors.size(), equalTo(0));
        current = pos.next();
        assertThat("Wrong feature after peg 3.", current.getId(), equalTo("fig|12345.6.peg.4"));
        neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg4.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.5"));
        assertThat("End-of-list after peg 4.", pos.hasNext(), equalTo(true));
        current = pos.next();
        assertThat("Wrong feature after peg 4.", current.getId(), equalTo("fig|12345.6.peg.5"));
        neighbors = pos.within(100);
        assertThat("Found neighbors at 100 for peg5.", neighbors.size(), equalTo(0));
        current = pos.next();
        assertThat("Wrong feature after peg 5.", current.getId(), equalTo("fig|12345.6.peg.6"));
        neighbors = pos.within(100);
        assertThat("Found neighbors at 100 for peg6.", neighbors.size(), equalTo(0));
        current = pos.next();
        assertThat("Wrong feature after peg 6.", current.getId(), equalTo("fig|12345.6.peg.7"));
        neighbors = pos.within(48);
        assertThat("Found neighbors at 48 for peg7.", neighbors.size(), equalTo(0));
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
        assertThat("End-of-list after peg 7.", pos.hasNext(), equalTo(true));
        current = pos.next();
        assertThat("Wrong feature after peg 7.", current.getId(), equalTo("fig|12345.6.peg.8"));
        neighbors = pos.within(48);
        assertThat("Found neighbors at 48 for peg8.", neighbors.size(), equalTo(0));
        neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg8.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.9"));
        current = pos.next();
        assertThat("Wrong feature after peg 8.", current.getId(), equalTo("fig|12345.6.peg.9"));
        neighbors = pos.within(100);
        assertThat("Wrong neighbors found at 100 for peg9.",
                neighbors.stream().map(Feature::getId).collect(Collectors.toList()),
                contains("fig|12345.6.peg.10"));
        current = pos.next();
        neighbors = pos.within(1000);
        assertThat("Found neighbors for peg10.", neighbors.size(), equalTo(0));
        assertThat("No end-of-list after peg10.", pos.hasNext(), equalTo(false));
    }

    /**
     * test custom features
     */
    @Test
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
        assertThat(feat.getPgfam(), nullValue());
        feat.setPlfam("");
        assertThat(feat.getPlfam(), nullValue());
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
        assertThat(goTerms[0].getDescription(), nullValue());
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

    @Test
    public void testFunctions() {
        String fun1 = "fun serum/thing / slash role @ at-role; semi role   # pound comment ## pounding";
        String[] roles = Feature.rolesOfFunction(fun1);
        assertThat("Wrong role count.", roles.length, equalTo(4));
        assertThat("first role wrong", roles[0], equalTo("fun serum/thing"));
        assertThat("second role wrong", roles[1], equalTo("slash role"));
        assertThat("third role wrong", roles[2], equalTo("at-role"));
        assertThat("fourth role wrong", roles[3], equalTo("semi role"));
        fun1 = "unitary role with comment ! bang comment";
        roles = Feature.rolesOfFunction(fun1);
        assertThat("Wrong unit role count.", roles.length, equalTo(1));
        assertThat("Wrong unit role", roles[0], equalTo("unitary role with comment"));
        roles = Feature.rolesOfFunction("");
        assertThat("Failure on empty function", roles.length, equalTo(0));
        roles = Feature.rolesOfFunction(null);
        assertThat("Failure on null function", roles.length, equalTo(0));
        roles = Feature.rolesOfFunction("simple");
        assertThat("Failure on simple role", roles.length, equalTo(1));
        assertThat("Wrong simple role", roles[0], equalTo("simple"));
        Feature testFeat = myGto.getFeature("fig|1313.7001.peg.1189");
        roles = testFeat.getRoles();
        assertThat("Wrong number of roles in feature", roles.length, equalTo(2));
        assertThat("Wrong first role of feature", roles[0], equalTo("IMP cyclohydrolase (EC 3.5.4.10)"));
        assertThat("Wrong second role of feature", roles[1],
                equalTo("Phosphoribosylaminoimidazolecarboxamide formyltransferase (EC 2.1.2.3)"));
        // Test useful roles.
        RoleMap goodRoles = new RoleMap();
        goodRoles.register("Phosphoribosylaminoimidazolecarboxamide formyltransferase (EC 2.1.2.3)");
        List<Role> found = testFeat.getUsefulRoles(goodRoles);
        assertThat("Useful role count wrong.", found.size(), equalTo(1));
        assertThat("Wrong useful role returned", found.get(0).getId(), equalTo("PhosForm"));
        testFeat = myGto.getFeature("fig|1313.7001.peg.1190");
        found = testFeat.getUsefulRoles(goodRoles);
        assertThat("Useful role found in useless peg.", found.size(), equalTo(0));
        // Test hypotheticals
        assertThat(Feature.isHypothetical("hypothetical protein"), equalTo(true));
        assertThat(Feature.isHypothetical(null), equalTo(true));
        assertThat(Feature.isHypothetical("  # comment only"), equalTo(true));
        assertThat(Feature.isHypothetical("Hypothetical protein # with comment"), equalTo(true));
        assertThat(Feature.isHypothetical("Normal function # hypothetical protein"), equalTo(false));
        assertThat(Feature.isHypothetical("May some day be a putative function"), equalTo(false));
    }

    /**
     * FASTA file test
     * @throws IOException
     */
    @Test
    public void testFasta() throws IOException {
        File inFasta = new File("data", "empty.fa");
        FastaInputStream inStream = new FastaInputStream(inFasta);
        assertThat("Error in empty fasta.", inStream.hasNext(), equalTo(false));
        inStream.close();
        inFasta = new File("data", "test.fa");
        inStream = new FastaInputStream(inFasta);
        ArrayList<Sequence> testSeqs = new ArrayList<Sequence>(5);
        for (Sequence input : inStream) {
            testSeqs.add(input);
        }
        inStream.close();
        assertThat("Wrong number of sequences.", testSeqs.size(), equalTo(5));
        Sequence seq = testSeqs.get(0);
        assertThat("Wrong label for seq 1.", seq.getLabel(), equalTo("label1"));
        assertThat("Wrong comment for seq 1.", seq.getComment(), equalTo(""));
        assertThat("Wrong sequence for seq 1.", seq.getSequence(), equalTo("tgtgcagcgagccctacagccttggagggaacaacacggactacctgccgctcgtctacccaaagggggtccccctccccaacacaacggttaccagcgtgccgagcg"));
        seq = testSeqs.get(1);
        assertThat("Wrong label for seq 2.", seq.getLabel(), equalTo("label2"));
        assertThat("Wrong comment for seq 2.", seq.getComment(), equalTo("comment2 with spaces"));
        assertThat("Wrong sequence for seq 2.", seq.getSequence(), equalTo("ctcaatgggtccgtagtcggcatcggcagatgtgtataagcagcatgcccgccctctgcag"));
        seq = testSeqs.get(2);
        assertThat("Wrong label for seq 3.", seq.getLabel(), equalTo("label3"));
        assertThat("Wrong comment for seq 3.", seq.getComment(), equalTo("comment3"));
        assertThat("Wrong sequence for seq 3.", seq.getSequence(), equalTo("gtataaagtattggcctgttgag"));
        seq = testSeqs.get(3);
        assertThat("Wrong label for seq 4.", seq.getLabel(), equalTo("label4"));
        assertThat("Wrong comment for seq 4.", seq.getComment(), equalTo("comment4"));
        assertThat("Wrong sequence for seq 4.", seq.getSequence(), equalTo(""));
        seq = testSeqs.get(4);
        assertThat("Wrong label for seq 5.", seq.getLabel(), equalTo("label5"));
        assertThat("Wrong comment for seq 5.", seq.getComment(), equalTo("comment5"));
        assertThat("Wrong sequence for seq 5.", seq.getSequence(), equalTo("ggggccctgaggtcctgagcaagtgggtcggcgagagcgagaaggcgataaggt"));
        // Write the FASTA back out.
        File outFasta = new File("data", "fasta.ser");
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
        assertThat("Wrong number of records on read back.", readSeqs.size(), equalTo(5));
        for (int i = 0; i < 5; i++) {
            assertThat("Compare error at position " + i + " on readback.", readSeqs.get(i), equalTo(testSeqs.get(i)));
        }
    }

    /**
     * test frame comparison
     */
    @Test
    public void testFrames() {
        assertThat("M0 compare fail.", Frame.M0.compareTo(Frame.F0), lessThan(0));
        assertThat("M1 compare fail.", Frame.M1.compareTo(Frame.F0), lessThan(0));
        assertThat("M2 compare fail.", Frame.M2.compareTo(Frame.F0), lessThan(0));
        assertThat("F0 compare fail.", Frame.F0.compareTo(Frame.F0), equalTo(0));
        assertThat("P0 compare fail.", Frame.P0.compareTo(Frame.F0), greaterThan(0));
        assertThat("P1 compare fail.", Frame.P1.compareTo(Frame.F0), greaterThan(0));
        assertThat("P2 compare fail.", Frame.P2.compareTo(Frame.F0), greaterThan(0));
        assertThat("M0 negative fail.", Frame.M0.negative(), equalTo(true));
        assertThat("M1 negative fail.", Frame.M1.negative(), equalTo(true));
        assertThat("M2 negative fail.", Frame.M2.negative(), equalTo(true));
        assertThat("F0 negative fail.", Frame.F0.negative(), equalTo(false));
        assertThat("P0 negative fail.", Frame.P0.negative(), equalTo(false));
        assertThat("P1 negative fail.", Frame.P1.negative(), equalTo(false));
        assertThat("P2 negative fail.", Frame.P2.negative(), equalTo(false));
    }

    /**
     * test GTO updating
     *
     * @throws IOException
     * @throws NumberFormatException
     */
    @Test
    public void testGTO() throws NumberFormatException, IOException {
        Genome smallGenome = new Genome(new File("data", "small.gto"));
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
            assertThat(curr.compareTo(next) < 0, equalTo(true));
        }
        // Now we will add a feature and a contig.
        Feature rna = new Feature("fig|161.31.rna.1", "brand new RNA", "161.31.con.0001", "-", 89101, 90100);
        rna.setPgfam("PGF_RNA1");
        rna.addCoupling("fig|161.31.rna.2", 20, 20.5);
        rna.addCoupling("fig|161.31.rna.3", 30, 30.5);
        rna.addAlias("rna1");
        rna.addAlias("gi|thingness");
        smallGenome.addFeature(rna);
        assertThat(rna.getParent(), equalTo(smallGenome));
        Contig fakeContig = new Contig("161.31.con.0002", "aaaccctttggg", 11);
        fakeContig.setAccession("fakeAcc");
        fakeContig.setDescription("fake description");
        assertThat(fakeContig.getAccession(), equalTo("fakeAcc"));
        assertThat(fakeContig.getDescription(), equalTo("fake description"));
        smallGenome.addContig(fakeContig);
        File testFile = new File("data", "gto.ser");
        smallGenome.save(testFile);
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
        assertThat(feat.getPlfam(), nullValue());
        assertThat(feat.getAliases(), containsInAnyOrder("rna1", "gi|thingness"));
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
        Genome testOther = new Genome(new File("data", "bin3a.gto"));
        assertThat(testGenome.identical(testGenome), equalTo(true));
        assertThat(testGenome.identical(null), equalTo(false));
        assertThat(testGenome.identical(testOther), equalTo(false));
        assertThat(testGenome.identical(smallGenome), equalTo(false));
    }

    /**
     * test codon sets
     */
    @Test
    public void testCodonSet() {
        String myDna = "aaattgcaaggacaactgatggagcgctaatagtgactg";
        CodonSet newSet = new CodonSet("ttg", "ctg", "atg");
        assertThat(newSet.contains(myDna, 1), equalTo(false));
        assertThat(newSet.contains(myDna, 4), equalTo(true));
        assertThat(newSet.contains(myDna, 7), equalTo(false));
        assertThat(newSet.contains(myDna, 10), equalTo(false));
        assertThat(newSet.contains(myDna, 13), equalTo(false));
        assertThat(newSet.contains(myDna, 16), equalTo(true));
        assertThat(newSet.contains(myDna, 19), equalTo(true));
        assertThat(newSet.contains(myDna, 22), equalTo(false));
        assertThat(newSet.contains(myDna, 25), equalTo(false));
        assertThat(newSet.contains(myDna, 28), equalTo(false));
        assertThat(newSet.contains(myDna, 31), equalTo(false));
        assertThat(newSet.contains(myDna, 34), equalTo(false));
        assertThat(newSet.contains(myDna, 37), equalTo(true));
    }

    /**
     * test location extension
     * @throws IOException
     */
    @Test
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
        assertThat(Location.containsCodon(new CodonSet("caa", "cag", "cat"), seq, 16, 54), equalTo(false));
        assertThat(Location.containsCodon(new CodonSet("cag", "acg"), seq, 1, 22), equalTo(true));
        assertThat(Location.containsCodon(new CodonSet("cag", "acg"), seq, 1, 15), equalTo(false));
        assertThat(Location.containsCodon(new CodonSet("cag", "acg"), seq, 2, 44), equalTo(false));
        // Test edge before stop on - strand.
        Location loc = Location.create("c1", "-", 4, 15);
        assertThat(loc.extend(fakeGenome), nullValue());
        // Insure that a GC 11 stop is not found in GC 4.
        loc = Location.create("c1", "+", 37, 39);
        Location loc2 = loc.extend(fakeGenome);
        assertThat(loc2.getLeft(), equalTo(25));
        assertThat(loc2.getRight(), equalTo(45));
        // Test edge before start on - strand.
        loc = Location.create("c1",  "-", 52, 54);
        assertThat(loc.extend(fakeGenome), nullValue());
        // Test edge before stop on + strand.
        loc = Location.create("c2", "+", 52, 54);
        assertThat(loc.extend(fakeGenome), nullValue());
        // Test edge before start on + strand.
        loc = Location.create("c2", "+", 10, 12);
        // Test internal stops.
        loc = Location.create("c3",  "+",  34, 63);
        assertThat(loc.extend(fakeGenome), nullValue());
        loc = Location.create("c3", "-", 7, 27);
        assertThat(loc.extend(fakeGenome), nullValue());
        // Stress test a real genome.
        Genome gto = new Genome(new File("data", "testLocs.gto"));
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
    @Test
    public void testMD5() throws NoSuchAlgorithmException, IOException {
        MD5Hex mdComputer = new MD5Hex();
        String prot = "MLHIKPYLVNQKTLHEIEKAIKKAKPNVTINSKDSELICSIPEPTAEIREEKIKNSKQVVEETRIALRTKRQDLLKKFKS";
        assertThat(mdComputer.checksum(prot), equalTo("54ce0e56c20f6eee543f1995ae7d7dcc"));
        prot = "MEHTFPEILALAFVGGLVLNLMPCVFPILSLKVLSIVRKSSKSRWSTAVDGVYYTAGVMSSMLLLSLVLILLRSAGHFLGWGFQMQSPALVIGLLHVTFLVGMSFSGFLDLSIKVPFVDAMTAHNVGSFFAGVLSALIGTPCSAPFMVSAVSFALLQPGLRSVAIFQVMGLGMALPYLIISCSPGLTRLLPKPGRWMEYLKQFLAFPMYATSAWLLHVLVSQKGTHVLLPTVLSIVAVSLGVWFLRVMSNVKMQASKALAVLLPLFVVGTAIYIGRFRGHDHAYGELAVVEFSEARLARLLRKDKTVFLSVGAEWCLTCKVNEKVLESASVQSFLRTHGVIYMKADWTNMDSTIAEYLSEHGGGGVPFYELYVNGKSVGPMPQIFSEKTLLEILGKHLNANPSSKASPE";
        assertThat(mdComputer.checksum(prot), equalTo("a5b29797d99fceda08c5bb913473529f"));
        prot = "MITHNPVIATVTDRVIRIDGGKIVEDYRNPNPVSIDSLTNL";
        assertThat(mdComputer.checksum(prot), equalTo("9e36013f41acf2443643cdcf178bda67"));
        Genome coreGenome = new Genome(new File("data", "360106.5.gto"));
        String md5 = mdComputer.sequenceMD5(coreGenome);
        assertThat(md5, equalTo("9606255e9c598c259f96a74083d87a35"));
        File testFile = File.createTempFile("test", ".fasta", new File("data"));
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

    @Test
    public void testContigUpdate() throws IOException {
        Genome myGenome = new Genome(new File("data", "bin3.gto"));
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
    @Test
    public void testGoTerms() {
        GoTerm go = new GoTerm(33925);
        assertThat(go.toString(), equalTo("GO:0033925"));
        assertThat(go.getNumber(), equalTo(33925));
        assertThat(go.getDescription(), nullValue());
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
    @Test
    public void testSave() throws IOException {
        File gtoFile = new File("data", "gto2.ser");
        Genome gto = new Genome(new File("data/gto_test", "243277.26.gto"));
        gto.purify();
        gto.save(gtoFile);
        Genome diskGenome = new Genome(gtoFile);
        test(gto, diskGenome, true);
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
    @Test
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
        assertThat(subsystem.isActive(), equalTo(true));
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
    @Test
    public void testFastas() throws IOException {
        File fastaTemp = new File("data", "fasta.ser");
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
    @Test
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

    /**
     * Verify two genomes are identical.
     *
     * @param gto	first genome
     * @param gto2	second genome
     * @param full	if TRUE, non-core attributes will be tested
     */
    public static void test(Genome gto, Genome gto2, boolean full) {
        assertThat(gto2.getId(), equalTo(gto.getId()));
        assertThat(gto2.getName(), equalTo(gto.getName()));
        assertThat(gto2.getDomain(), equalTo(gto.getDomain()));
        assertThat(ArrayUtils.toObject(gto2.getLineage()), arrayContaining(ArrayUtils.toObject(gto.getLineage())));
        assertThat(gto2.getGeneticCode(), equalTo(gto.getGeneticCode()));
        assertThat(gto2.getTaxonomyId(), equalTo(gto.getTaxonomyId()));
        assertThat(gto2.getFeatureCount(), equalTo(gto.getFeatureCount()));
        assertThat(gto2.getContigCount(), equalTo(gto.getContigCount()));
        Collection<Feature> fids = gto.getFeatures();
        for (Feature fid : fids) {
            Feature diskFid = gto2.getFeature(fid.getId());
            assertThat(diskFid.getFunction(), equalTo(fid.getFunction()));
            assertThat(diskFid.getLocation(), equalTo(fid.getLocation()));
            assertThat(diskFid.getPlfam(), equalTo(fid.getPlfam()));
            assertThat(diskFid.getType(), equalTo(fid.getType()));
            assertThat(diskFid.getProteinTranslation(), equalTo(fid.getProteinTranslation()));
            if (full) {
                Collection<GoTerm> fidGoTerms = fid.getGoTerms();
                assertThat(diskFid.getGoTerms().size(), equalTo(fidGoTerms.size()));
                for (GoTerm diskGoTerm : diskFid.getGoTerms()) {
                    assertThat(fidGoTerms, hasItem(diskGoTerm));
                }
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
            Contig diskContig = gto2.getContig(contig.getId());
            assertThat(diskContig.length(), equalTo(contig.length()));
            assertThat(diskContig.getSequence(), equalTo(contig.getSequence()));
        }
    }

    public static class Thing extends MagicObject implements Comparable<Thing> {

        /**
         * serialization object type ID
         */
        private static final long serialVersionUID = -5855290474686618053L;

        /** Create a blank thing. */
        public Thing() { }

        /** Create a new thing with a given ID and description. */
        public Thing(String thingId, String thingDesc) {
            super(thingId, thingDesc);
        }

        @Override
        protected String normalize() {
            // Convert all sequences of non-word characters to a single space and lower-case it.
            String retVal = this.getName().replaceAll("\\W+", " ").toLowerCase();
            return retVal;
        }

        @Override
        public int compareTo(Thing o) {
            return super.compareTo(o);
        }

    }

    public static class ThingMap extends MagicMap<Thing> {

        public ThingMap() {
            super(new Thing());
        }

        /**
         * Find the named thing.  If it does not exist, a new thing will be created.
         *
         * @param thingDesc	the thing name
         *
         * @return	a Thing object for the thing
         */
        public Thing findOrInsert(String thingDesc) {
            Thing retVal = this.getByName(thingDesc);
            if (retVal == null) {
                // Create a thing without an ID.
                retVal = new Thing(null, thingDesc);
                // Store it in the map to create the ID.
                this.put(retVal);
            }
            return retVal;
        }

        public void save(File saveFile) {
            try {
                PrintWriter printer = new PrintWriter(saveFile);
                for (Thing thing : this.objectValues()) {
                    printer.format("%s\t%s%n", thing.getId(), thing.getName());
                }
                printer.close();
            } catch (IOException e) {
                throw new RuntimeException("Error saving thing map.", e);
            }
        }

        public static ThingMap load(File loadFile) {
            ThingMap retVal = new ThingMap();
            try {
                Scanner reader = new Scanner(loadFile);
                while (reader.hasNext()) {
                    String myLine = reader.nextLine();
                    String[] fields = StringUtils.splitByWholeSeparator(myLine, "\t", 2);
                    Thing newThing = new Thing(fields[0], fields[1]);
                    retVal.put(newThing);
                }
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException("Error loading thing map.", e);
            }
            return retVal;
        }

        /**
         * Add an alias for an existing ID.
         *
         * @param id	ID to get the alias
         * @param name	name string to be made an alias
         */
        public void addAlias(String id, String name) {
            Thing altThing = new Thing(id, name);
            this.register(altThing);
        }


    }

}
