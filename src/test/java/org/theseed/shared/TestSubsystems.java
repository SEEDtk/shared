package org.theseed.shared;

import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.genome.SubsystemRow;
import org.theseed.io.Shuffler;
import org.theseed.subsystems.SubsystemProjector;
import org.theseed.subsystems.SubsystemSpec;
import org.theseed.subsystems.VariantId;
import org.theseed.subsystems.VariantSpec;

import junit.framework.TestCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class TestSubsystems extends TestCase {

    /**
     * Test the variant ID
     */
    public void testVariantID() {
        // Create some simple variants.
        VariantId varID = new VariantId("subsystem1", "0");
        VariantId varID2 = new VariantId("subsystem2", "2");
        VariantId varID3 = new VariantId("subsystem1", "1");
        VariantId varID4 = new VariantId("subsystem2", "2");
        assertThat(varID.getName(), equalTo("subsystem1"));
        assertThat(varID.getCode(), equalTo("0"));
        assertThat(varID2, equalTo(varID4));
        assertThat(varID2.compareTo(varID4), equalTo(0));
        assertThat(varID4, equalTo(varID2));
        assertThat(varID, not(equalTo(varID2)));
        assertThat(varID2, not(equalTo(varID3)));
        Shuffler<VariantId> variants = new Shuffler<VariantId>(4).add1(varID).add1(varID2).add1(varID3);
        variants.sort(null);
        assertThat(variants, contains(varID, varID3, varID2));
        assertFalse(varID.isActive());
        assertTrue(varID2.isActive());
        varID2.setCode("-1");
        assertThat(varID2.getCode(), equalTo("-1"));
        assertThat(varID2, not(equalTo(varID4)));
        assertFalse(varID2.isActive());
        varID2.setCode("*2");
        assertTrue(varID2.isActive());
        varID2.setCode("*-1");
        assertFalse(varID2.isActive());
    }

    /**
     * Test the subsystem specification
     */
    public void testSubsystemSpec() {
        SubsystemSpec subsystem = new SubsystemSpec("funny subsystem");
        subsystem.setClassifications("c1", "c2", "c3");
        assertThat(subsystem.getName(), equalTo("funny subsystem"));
        assertThat(subsystem.getClassifications(), contains("c1", "c2", "c3"));
        SubsystemSpec subsystem2 = new SubsystemSpec("funny subsystem");
        assertThat(subsystem, equalTo(subsystem2));
        assertThat(subsystem.compareTo(subsystem2), equalTo(0));
        subsystem.addRole("role 0");
        subsystem.addRole("role 1");
        subsystem.addRole("role 2");
        assertThat(subsystem.getRoleCount(), equalTo(3));
        assertThat(subsystem, equalTo(subsystem2));
        assertThat(subsystem.getRoles(), contains("role 0", "role 1", "role 2"));
        assertThat(subsystem.getRole(0), equalTo("role 0"));
        assertThat(subsystem.getRole(1), equalTo("role 1"));
        assertThat(subsystem.getRole(2), equalTo("role 2"));
    }

    /**
     *
     * test family map creation (PGFAM only)
     *
     * @throws IOException
     */
    public void testFamilyMap() throws IOException {
        // Test the genome family map.
        SubsystemProjector projector = new SubsystemProjector();
        Genome gto = new Genome(new File("src/test/gto_test", "1313.7001.gto"));
        Map<String, Set<String>> familyMap = projector.computeFamilyMap(gto);
        assertThat(familyMap.get("PGF_03504890"), hasItem("fig|1313.7001.peg.304"));
        assertThat(familyMap.get("PGF_91035886"), nullValue());
        // Verify that all the features with families are found.
        for (Feature feat : gto.getFeatures()) {
            String pgFam = feat.getPgfam();
            if (pgFam != null && ! pgFam.isEmpty()) {
                String fid = feat.getId();
                assertThat(fid, familyMap.get(pgFam), hasItem(fid));
            }
        }
        // Verify that all families have valid features.
        for (Map.Entry<String, Set<String>> famEntry : familyMap.entrySet()) {
            String pgFam = famEntry.getKey();
            Set<String> fids = famEntry.getValue();
            for (String fid : fids) {
                Feature feat = gto.getFeature(fid);
                assertThat(fid, feat, not(nullValue()));
                String fidFam = feat.getPgfam();
                assertThat(fid, pgFam, equalTo(fidFam));
            }
        }
    }

    /**
     * test the variant specification
     *
     * @throws IOException
     */
    public void testVariantSpec() throws IOException {
        SubsystemProjector projector = new SubsystemProjector();
        // Create a subsystem found in 1313.7001.
        SubsystemSpec subsystem = new SubsystemSpec("Cell division related cluster including coaD");
        subsystem.setClassifications("", "Clustering-based subsystems", "Cell Division");
        subsystem.addRole("16S rRNA (guanine(966)-N(2))-methyltransferase (EC 2.1.1.171)");
        subsystem.addRole("Cell-division-associated, ABC-transporter-like signaling protein FtsE");
        subsystem.addRole("Cell-division-associated, ABC-transporter-like signaling protein FtsX");
        subsystem.addRole("Phosphopantetheine adenylyltransferase (EC 2.7.7.3)");
        subsystem.addRole("Signal recognition particle receptor FtsY");
        projector.addSubsystem(subsystem);
        VariantSpec variant = new VariantSpec(subsystem, "likely");
        assertThat(variant.getName(), equalTo(subsystem.getName()));
        assertThat(variant.getCode(), equalTo("likely"));
        // This is a test for doing compares and stuff.  We will continue to built the variant-spec in variant.
        VariantSpec variant2 = new VariantSpec(subsystem, "likely");
        variant.setCell(1, "PGF_06857975");
        assertThat(variant2, not(equalTo(variant)));
        assertThat(variant2, greaterThan(variant));
        assertThat(variant.getKeyFamily(), equalTo("PGF_06857975"));
        variant2.setCell(1, "PGF_06857975");
        assertThat(variant2, equalTo(variant));
        assertThat(variant2.compareTo(variant), equalTo(0));
        assertThat(variant2.getCellCount(), equalTo(1));
        variant2.setCell(1, "PGF_07133621");
        assertThat(variant2, not(equalTo(variant)));
        assertThat(variant2.compareTo(variant), not(equalTo(0)));
        assertThat(variant2.getCellCount(), equalTo(1));
        variant2.setCell(4, "PGF_91035886");
        assertThat(variant2.getCellCount(), equalTo(2));
        variant.setCell(0, "PGF_07133621");
        assertThat(variant.getKeyFamily(), equalTo("PGF_06857975"));
        variant.setCell(2, "PGF_03701810");
        assertThat(variant.getKeyFamily(), equalTo("PGF_03701810"));
        assertThat(variant, lessThan(variant2));
        variant.setCell(3, "PGF_04762552");
        assertThat(variant.getKeyFamily(), equalTo("PGF_03701810"));
        variant.setCell(4, "PGF_08905885");
        assertThat(variant.getKeyFamily(), equalTo("PGF_03701810"));
        assertThat(variant.getCellCount(), equalTo(5));
        assertTrue(variant.isRedundant(variant2));
        VariantSpec variant3 = new VariantSpec(subsystem, "active");
        assertTrue(variant.isRedundant(variant3));
        assertTrue(variant3.isRedundant(variant));
        SubsystemSpec sub2 = new SubsystemSpec("Funny subsystem");
        variant3 = new VariantSpec(sub2, "likely");
        assertFalse(variant.isRedundant(variant3));
        sub2 = new SubsystemSpec(subsystem.getName());
        sub2.addRole("16S rRNA (guanine(966)-N(2))-methyltransferase (EC 2.1.1.171)");
        sub2.addRole("Cell-division-associated, ABC-transporter-like signaling protein FtsE");
        sub2.addRole("Cell-division-associated, ABC-transporter-like signaling protein FtsX");
        sub2.addRole("Phosphopantetheine adenylyltransferase (EC 2.7.7.3)");
        sub2.addRole("Signal recognition particle receptor FtsY");
        VariantSpec variant4 = new VariantSpec(sub2, "likely");
        variant4.setCell(1, "PGF_06857975");
        variant4.setCell(0, "PGF_07133621");
        variant4.setCell(2, "PGF_03701810");
        variant4.setCell(3, "PGF_04762552");
        variant4.setCell(4, "PGF_08905885");
        assertThat(variant4, equalTo(variant));
        assertThat(variant4.compareTo(variant), equalTo(0));
        assertThat(variant.compareTo(variant4), equalTo(0));
        assertTrue(projector.addVariant(variant));
        assertFalse(projector.addVariant(variant4));
        // Test matching and projecting using the genome family map.
        Genome gto = new Genome(new File("src/test/gto_test", "1313.7001.gto"));
        Map<String, Set<String>> familyMap = projector.computeFamilyMap(gto);
        assertTrue(variant.matches(familyMap));
        assertFalse(variant2.matches(familyMap));
        // Get the current subsystem row for this subsystem.
        SubsystemRow current = gto.getSubsystem(variant.getName());
        // Clear the genome's subsystems.
        gto.clearSubsystems();
        SubsystemRow created = variant.instantiate(gto, familyMap);
        assertTrue(created == gto.getSubsystem(variant.getName()));
        assertThat(created.getName(), equalTo(variant.getName()));
        assertThat(created.getVariantCode(), equalTo(variant.getCode()));
        assertThat(created.getClassifications(), contains("", "Clustering-based subsystems", "Cell Division"));
        List<SubsystemRow.Role> oldRoles = current.getRoles();
        List<SubsystemRow.Role> newRoles = current.getRoles();
        assertThat(newRoles.size(), equalTo(oldRoles.size()));
        for (int i = 0; i < newRoles.size(); i++) {
            SubsystemRow.Role oldRole = oldRoles.get(i);
            SubsystemRow.Role newRole = newRoles.get(i);
            assertThat(oldRole, equalTo(newRole));
            Set<Feature> feats = newRole.getFeatures();
            Set<Feature> oldFeats = oldRole.getFeatures();
            assertThat(feats.size(), equalTo(oldRole.getFeatures().size()));
            for (Feature feat : feats)
                assertThat(feat.getId(), oldFeats.contains(feat));
        }
    }

    /**
     * test projections with RNA components
     *
     * @throws IOException
     */
    public void testRnaProjection() throws IOException {
        SubsystemProjector projector = new SubsystemProjector();
        SubsystemSpec sub1 = new SubsystemSpec("Antibiotic targets in protein synthesis");
        sub1.setClassifications("Stress Response, Defense and Virulence",
                "Resistance to antibiotics and toxic compounds",
                "");
        sub1.addRole("SSU rRNA");
        sub1.addRole("LSU rRNA");
        sub1.addRole("SSU ribosomal protein S12p (S23e)");
        sub1.addRole("SSU ribosomal protein S10p (S20e)");
        sub1.addRole("Translation elongation factor G");
        sub1.addRole("LSU ribosomal protein L6p (L9e)");
        sub1.addRole("Translation elongation factor Tu");
        sub1.addRole("Isoleucyl-tRNA synthetase (EC 6.1.1.5)");
        projector.addSubsystem(sub1);
        String ssuRrna = projector.getRoleId("SSU rRNA");
        String lsuRrna = projector.getRoleId("LSU rRNA");
        VariantSpec var1 = new VariantSpec(sub1, "1");
        var1.setCell(0, ssuRrna);
        var1.setCell(1, lsuRrna);
        var1.setCell(2, "PGF_06941403");
        var1.setCell(3, "PGF_00049828");
        var1.setCell(4, "PGF_00060409");
        var1.setCell(5, "PGF_00016444");
        var1.setCell(6, "PGF_00060428");
        var1.setCell(7, "PGF_05171623");
        projector.addVariant(var1);
        // This is designed to stress the role map save and restore.
        SubsystemSpec sub2 = new SubsystemSpec("fake subsystem 1");
        sub2.addRole("SSU rRNAtest");
        projector.addSubsystem(sub2);
        sub2 = new SubsystemSpec("fake subsystem 2");
        sub2.addRole("SSU rRNAtest2");
        projector.addSubsystem(sub2);
        // Save and restore to check the role map.
        File outFile = new File("src/test", "projector.ser");
        projector.save(outFile);
        SubsystemProjector loaded = SubsystemProjector.Load(outFile);
        assertThat(loaded.getRoleId("SSU rRNA"), equalTo(ssuRrna));
        assertThat(loaded.getRoleId("LSU rRNA"), equalTo(lsuRrna));
        // Map the test genome.
        Genome gto = new Genome(new File("src/test","123214.3.gto"));
        Map<String, Set<String>> familyMap = projector.computeFamilyMap(gto);
        assertTrue(var1.matches(familyMap));
    }

    /**
     * test creating, saving, and loading a projector
     *
     * @throws IOException
     */
    public void testProjectorIO() throws IOException {
        SubsystemProjector projector = new SubsystemProjector();
        SubsystemSpec sub1 = new SubsystemSpec("subsystem 1");
        sub1.setClassifications("a1", "b1", "");
        sub1.addRole("s1 role 0");
        sub1.addRole("s1 role 1");
        sub1.addRole("s1 role 2");
        sub1.addRole("s1 role 3");
        projector.addSubsystem(sub1);
        VariantSpec var1_1 = new VariantSpec(sub1, "1");
        var1_1.setCell(1, "s1fam1");
        var1_1.setCell(3, "s1fam3");
        projector.addVariant(var1_1);
        VariantSpec var1_2 = new VariantSpec(sub1, "2");
        var1_2.setCell(0, "s1fam0");
        var1_2.setCell(1, "s1fam1a");
        var1_2.setCell(2, "s1fam2");
        projector.addVariant(var1_2);
        VariantSpec var1_3 = new VariantSpec(sub1, "3");
        var1_3.setCell(1,  "s1fam1");
        var1_3.setCell(2,  "s1fam2");
        var1_3.setCell(3,  "s1fam3");
        projector.addVariant(var1_3);
        SubsystemSpec sub2 = new SubsystemSpec("subsystem 2");
        sub2.setClassifications("", "b2", "c2");
        sub2.addRole("s2 role 0");
        sub2.addRole("s2 role 1");
        sub2.addRole("s2 role 2");
        sub2.addRole("s2 role 3");
        sub2.addRole("s2 role 4");
        projector.addSubsystem(sub2);
        VariantSpec var2_1 = new VariantSpec(sub2, "1");
        var2_1.setCell(0, "s2fam0");
        var2_1.setCell(1, "s2fam1");
        var2_1.setCell(2, "s2fam2");
        var2_1.setCell(3, "s2fam3");
        var2_1.setCell(4, "s2fam4");
        projector.addVariant(var2_1);
        VariantSpec var2_2 = new VariantSpec(sub2, "2");
        var2_2.setCell(0, "s2fam0");
        var2_2.setCell(2, "s2fam2");
        var2_2.setCell(4, "s2fam4");
        projector.addVariant(var2_2);
        VariantSpec var2_3 = new VariantSpec(sub2, "3");
        var2_3.setCell(2, "s2fam2");
        var2_3.setCell(3, "s2fam3");
        projector.addVariant(var2_3);
        VariantSpec var2_4 = new VariantSpec(sub2, "4");
        var2_4.setCell(1, "s2fam1");
        projector.addVariant(var2_4);
        File saveFile = new File("src/test", "projector.ser");
        projector.save(saveFile);
        SubsystemProjector loaded = SubsystemProjector.Load(saveFile);
        SubsystemSpec sub = loaded.getSubsystem("subsystem 1");
        assertThat(sub, equalTo(sub1));
        assertThat(sub.getClassifications(), contains("a1", "b1", ""));
        assertThat(sub.getRoles(), contains("s1 role 0", "s1 role 1", "s1 role 2", "s1 role 3"));
        sub = loaded.getSubsystem("subsystem 2");
        assertThat(sub, equalTo(sub2));
        assertThat(sub.getClassifications(), contains("", "b2", "c2"));
        assertThat(sub.getRoles(), contains("s2 role 0", "s2 role 1", "s2 role 2", "s2 role 3", "s2 role 4"));
        SortedSet<VariantSpec> variants = loaded.getVariants();
        assertThat(variants.size(), equalTo(7));
        VariantSpec old = null;
        for (VariantSpec curr : variants) {
            if (old != null)
                assertThat(old, lessThanOrEqualTo(curr));
            if (curr.getName().contentEquals("subsystem 1")) {
                switch (curr.getCode()) {
                case "1" :
                    assertThat(curr, equalTo(var1_1));
                    break;
                case "2" :
                    assertThat(curr, equalTo(var1_2));
                    break;
                case "3" :
                    assertThat(curr, equalTo(var1_3));
                    break;
                default :
                    fail("Invalid variant spec " + curr);
                }
            } else {
                switch (curr.getCode()) {
                case "1" :
                    assertThat(curr, equalTo(var2_1));
                    break;
                case "2" :
                    assertThat(curr, equalTo(var2_2));
                    break;
                case "3" :
                    assertThat(curr, equalTo(var2_3));
                    break;
                case "4" :
                    assertThat(curr, equalTo(var2_4));
                    break;
                default :
                    fail("Invalid variant spec " + curr);
                }
            }
        }
    }

}
