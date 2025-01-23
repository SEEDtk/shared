/**
 *
 */
package org.theseed.subsystems.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.subsystems.StrictRoleMap;

/**
 * @author Bruce Parrello
 *
 */
class TestCoreSubsystem {

    protected static final String[] SUB_NAMES = new String[] { "Citrate_Metabolism", "Cluster_with_dapF",
            "Histidine_Biosynthesis", "ZZ_gjo_need_homes_3", "2-oxoisovalerate_to_2-isopropyl-3-oxosuccinate_module",
            "Biosynthesis_of_Arabinogalactan_in_Mycobacteria", "Oxygen-dependent_NiFe_hydrogenase"};
    protected static final String[] REAL_NAMES = new String[] { "Citrate Metabolism", "Cluster with dapF",
            "Histidine Biosynthesis", "ZZ gjo need homes 3", "2-oxoisovalerate to 2-isopropyl-3-oxosuccinate module",
            "Biosynthesis of Arabinogalactan in Mycobacteria", "Oxygen-dependent NiFe hydrogenase"};

    @Test
    void testCoreSubsystemLoad() throws IOException, ParseFailureException {
        StrictRoleMap roleMap = StrictRoleMap.load(new File("data/ss_test/Subsystems", "core.roles.in.subsystems"));
        CoreSubsystem[] subs = new CoreSubsystem[7];
        for (int subIdx = 0; subIdx < 7; subIdx++) {
            File inDir = new File("data/ss_test/Subsystems", SUB_NAMES[subIdx]);
            subs[subIdx] = new CoreSubsystem(inDir, roleMap);
            assertThat(subs[subIdx].getName(), equalTo(REAL_NAMES[subIdx]));
        }
        // First, we handle the good subsystem, histidine biosynthesis.
        CoreSubsystem sub = subs[2];
        // Test the meta-data-- aux roles, version, classes, goodness.
        assertThat(sub.isAuxRole("HistTrnaSynt"), equalTo(true));
        assertThat(sub.isAuxRole("HistTrnaSyntLike"), equalTo(true));
        assertThat(sub.isAuxRole("PutaHistDehy"), equalTo(true));
        assertThat(sub.isAuxRole("SimiImidGlycPhos"), equalTo(true));
        assertThat(sub.isAuxRole("SimiImidGlycPhos2"), equalTo(true));
        assertThat(sub.isAuxRole("SimiImidGlycPhos3"), equalTo(true));
        assertThat(sub.isAuxRole("DomaSimiAminTerm"), equalTo(false));
        assertThat(sub.getVersion(), equalTo(201));
        assertThat(sub.getSuperClass(), equalTo("Amino Acids and Derivatives"));
        assertThat(sub.getMiddleClass(), equalTo("Histidine Metabolism"));
        assertThat(sub.getSubClass(), equalTo(""));
        assertThat(sub.isGood(), equalTo(true));
        // Get the variant codes from the spreadsheet.
        assertThat(sub.variantOf("1215343.11"), equalTo("likely"));
        assertThat(sub.variantOf("306264.1"), equalTo("active.1.0"));
        assertThat(sub.variantOf("100226.99"), nullValue());
        // Return the feature IDs from the spreadsheet.
        assertThat(sub.fidSetOf("306264.1"), containsInAnyOrder("fig|306264.1.peg.1424", "fig|306264.1.peg.1766", "fig|306264.1.peg.626",
                "fig|306264.1.peg.616", "fig|306264.1.peg.620", "fig|306264.1.peg.618", "fig|306264.1.peg.621",
                "fig|306264.1.peg.624", "fig|306264.1.peg.1724", "fig|306264.1.peg.625"));
        assertThat(sub.fidSetOf("100226.99").size(), equalTo(0));
        assertThat(sub.fidSetOf("218496.1"), containsInAnyOrder("fig|218496.1.rna.767", "fig|218496.1.peg.760"));
        // Test the notes.
        assertThat(sub.getPubmed(), containsInAnyOrder(10382260, 19348578));
        var vNotes = sub.getVariantNotes();
        assertThat(vNotes.size(), equalTo(4));
        assertThat(vNotes.get("-1"), equalTo("organism incapable of de novo biosynthesis of tetrapyrroles"));
        assertThat(vNotes.get("1.AhbABCD"), equalTo("de novo bios of 5-ALA and siroheme asserted. HEME bios from sirocheme via AhbABCD path"));
        assertThat(sub.getNote(), equalTo(" Thiamin monophosphate is formed by coupling of two independently synthesized moieties.\n"
                + "\n"
                + "#36	THI10: Thiamin transporter in yeast. PMID: 19348578\n"
                + "\n"
                + "References\n"
                + "\n"
                + "1.	Begley TP, Downs DM, Ealick SE, McLafferty FW, Van Loon AP, Taylor S, Campobasso N, Chiu HJ, Kinsland C, Reddick JJ, Xi J. Thiamin biosynthesis in prokaryotes. Arch Microbiol. 1999 Apr;171(5):293-300. Review. PMID: 10382260\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + ""));
        assertThat(sub.getDescription(), equalTo(" Ubiquinone (Coenzyme Q) functions in the respiratory electron transport chain and serves as a lipophilic antioxidant. Ubiquinone is an acceptor of electrons from many cellular dehydrogenases involved in the oxidative metabolism of dihydroorotate, choline, fatty acyl-CoA, glycerolphosphate, sarcosine, and dimethylglycine .\n"
                + " The UQ biosynthetic enzymes may constitute a complex that is tightly bound to the membrane.\n"
                + "   In the biosythetic pathway the nucleus is derived from the shikimate pathway via chorismate in bacteria or tyrosin in higher eukaryotes. The prenyl side chain is derived from prenyl diphosphate (prenyl PPi) and the methyl groups are derived from S-adenosylmethionine.\n"
                + ""));
        // Test the role helpers.
        assertThat(sub.getRoleId("Histidinol-phosphatase [alternative form] (EC 3.1.3.15)"), equalTo("HistPhosAlteForm"));
        assertThat(sub.isExactRole("HistPhosAlteForm", "Histidinol-phosphatase [alternative form] (EC 3.1.3.15)"), equalTo(true));
        assertThat(sub.isExactRole("HistPhosAlteForm", "Histidinol-phosphatase [alternative form]"), equalTo(false));
        assertThat(sub.getRoleId("Histidine-phosphatase [alternative form] (EC 3.1.3.15)"), equalTo(null));
        assertThat(sub.isExactRole("HistPhosAlteForm", "Histidine-phosphatase [alternative form] (EC 3.1.3.15)"), equalTo(false));
        assertThat(sub.getRoleId("Phenylalanyl-tRNA synthetase alpha chain"), equalTo(null));
        assertThat(sub.isExactRole("PhenTrnaSyntAlph", "Phenylalanyl-tRNA synthetase alpha chain"), equalTo(false));
        assertThat(sub.getExpectedRole("HistPhosAlteForm"), equalTo("Histidinol-phosphatase [alternative form] (EC 3.1.3.15)"));
        Genome genome = new Genome(new File("data/ss_test_gto", "1215343.11.gto"));
        Set<String> gRoleSet = CoreSubsystem.getRoleSet(genome, roleMap);
        assertThat(gRoleSet, hasItem("PhosSynt4"));
        assertThat(gRoleSet, hasItem("UdpGluc4Epim"));
        assertThat(gRoleSet, not(hasItem("MethCoaHydr")));
        assertThat(sub.applyRules(gRoleSet), equalTo("likely"));
        genome = new Genome(new File("data/ss_test_gto", "306264.1.gto"));
        gRoleSet = CoreSubsystem.getRoleSet(genome, roleMap);
        assertThat(sub.applyRules(gRoleSet), equalTo("active.1.0"));
        // Test the string representation of some rules.
        SubsystemRule rule = sub.getRule("active.1.0");
        String ruleString = rule.toString();
        SubsystemRule recursive = RuleCompiler.parseRule(ruleString, sub.getNameSpace());
        assertThat(recursive, equalTo(rule));
        // Now test the bad ones.
        for (int i = 0; i < 4; i++) {
            if (i != 2) {
                sub = subs[i];
                assertThat(sub.getName(), sub.isGood(), equalTo(false));
            }
        }
        // Now we check Cluster with dapF, that has no rules, to verify that.
        sub = subs[1];
        assertThat(sub.getName(), equalTo("Cluster with dapF"));
        assertThat(sub.hasRules(), equalTo(false));
        // We will use Biosynthesis of Arbainogalactan to test rulebits.
        sub = subs[5];
        CoreSubsystem.Row row = sub.getRowOf("83332.1");
        assertThat(row, not(nullValue()));
        RuleBits row83332_1 = new RuleBits(row);
        assertThat(row83332_1.size(), equalTo(14));
        RuleBits row362242_7 = new RuleBits(sub.getRowOf("362242.7"));
        assertThat(row362242_7, equalTo(row83332_1));
        assertThat(row83332_1.subsumeCompare(row362242_7), equalTo(-1));
        assertThat(row83332_1.compareTo(row362242_7), equalTo(0));
        assertThat(row362242_7.subsumeCompare(row83332_1), equalTo(-1));
        assertThat(row362242_7.compareTo(row83332_1), equalTo(0));
        RuleBits row350058_5 = new RuleBits(sub.getRowOf("350058.5"));
        assertThat(row350058_5.size(), equalTo(13));
        assertThat(row350058_5.subsumeCompare(row83332_1), equalTo(-1));
        assertThat(row83332_1.subsumeCompare(row350058_5), equalTo(1));
        assertThat(row350058_5.compareTo(row83332_1), greaterThan(0));
        assertThat(row83332_1.compareTo(row350058_5), lessThan(0));
        assertThat(row350058_5.compareTo(row362242_7), greaterThan(0));
        assertThat(row362242_7.compareTo(row350058_5), lessThan(0));
        RuleBits row257309_1 = new RuleBits(sub.getRowOf("257309.1"));
        assertThat(row257309_1.size(), equalTo(10));
        assertThat(row257309_1.subsumeCompare(row83332_1), equalTo(0));
        assertThat(row83332_1.subsumeCompare(row257309_1), equalTo(0));
    }

    @Test
    void testSubsystemList() throws IOException {
        // Verify that we find four subsystems directories.
        List<File> subs = CoreSubsystem.getSubsystemDirectories(new File("data", "ss_test"));
        assertThat(subs.size(), equalTo(7));
        List<String> subNames = subs.stream().map(x -> x.getName()).collect(Collectors.toList());
        assertThat(subNames, containsInAnyOrder(SUB_NAMES));
        // Verify that all found directories have spreadsheets.
        for (File sub : subs) {
            File ssFile = new File(sub, "spreadsheet");
            assertThat(sub.getName(), ssFile.exists(), equalTo(true));
        }
    }

    @Test
    void testSubsystemDescriptor() throws IOException, ParseFailureException, ClassNotFoundException {
        StrictRoleMap roleMap = StrictRoleMap.load(new File("data/ss_test/Subsystems", "core.roles.in.subsystems"));
        File inDir = new File("data/ss_test/Subsystems", "Histidine_Biosynthesis");
        CoreSubsystem sub = new CoreSubsystem(inDir, roleMap);
        SubsystemDescriptor desc = new SubsystemDescriptor(sub);
        final File objTestFile = new File("data", "subsystem.ser");
        try (FileOutputStream outStream = new FileOutputStream(objTestFile)) {
            ObjectOutputStream objStream = new ObjectOutputStream(outStream);
            objStream.writeObject(desc);
            objStream.close();
        }
        // Now we read the object back in and verify it worked.
        SubsystemDescriptor desc2 = null;
        try (FileInputStream inStream = new FileInputStream(objTestFile)) {
            ObjectInputStream objStream = new ObjectInputStream(inStream);
            desc2 = (SubsystemDescriptor) objStream.readObject();
            objStream.close();
        }
        assertThat(desc2, equalTo(desc));
    }

    @Test
    void testSubsystemCompiler() throws IOException, ParseFailureException, ClassNotFoundException {
        StrictRoleMap roleMap = StrictRoleMap.load(new File("data/ss_test/Subsystems", "core.roles.in.subsystems"));
        File inDir = new File("data/ss_test/Subsystems", "Oxygen-dependent_NiFe_hydrogenase");
        CoreSubsystem sub = new CoreSubsystem(inDir, roleMap);
        assertThat(sub.getVariantRuleCount(), equalTo(2));
    }

    @Test
    void testStrictRoleMap() throws IOException, ParseFailureException, ClassNotFoundException {
        StrictRoleMap roleMap = StrictRoleMap.load(new File("data/ss_test/Subsystems", "core.roles.in.subsystems"));
        Genome gto = new Genome(new File("data/ss_test/Gtos", "319225.3.gto"));
        var roleSet = roleMap.getRolePresenceMap(gto);
        // Insure every role in the set is in the genome.
        for (var roleSetEntry : roleSet.entrySet()) {
            String roleKey = roleSetEntry.getKey();
            Set<String> fids = roleSetEntry.getValue();
            for (String fid : fids) {
                String function = gto.getFeature(fid).getFunction();
                Set<String> roles = roleMap.usefulRoles(function).stream().map(x -> x.getId()).collect(Collectors.toSet());
                assertThat(roleKey + " " + fid, roles, hasItem(roleKey));
            }
        }
        // Insure every useful role in the genome is in the set.
        Set<String> roleSetKeys = roleSet.keySet();
        for (Feature feat : gto.getFeatures()) {
            String function = feat.getFunction();
            Set<String> roles = roleMap.usefulRoles(function).stream().map(x -> x.getId()).collect(Collectors.toSet());
            for (String role : roles)
                assertThat(feat.toString(), role, in(roleSetKeys));
        }


    }

}
