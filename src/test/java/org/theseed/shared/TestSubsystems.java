package org.theseed.shared;

import org.theseed.counters.Shuffler;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.genome.SubsystemRow;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.subsystems.SubsystemProjector;
import org.theseed.subsystems.SubsystemSpec;
import org.theseed.subsystems.VariantId;
import org.theseed.subsystems.VariantSpec;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class TestSubsystems {

    /**
     * Test the variant ID
     */
    @Test
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
        assertThat(varID.isActive(), equalTo(false));
        assertThat(varID2.isActive(), equalTo(true));
        varID2.setCode("-1");
        assertThat(varID2.getCode(), equalTo("-1"));
        assertThat(varID2, not(equalTo(varID4)));
        assertThat(varID2.isActive(), equalTo(false));
        varID2.setCode("*2");
        assertThat(varID2.isActive(), equalTo(true));
        varID2.setCode("*-1");
        assertThat(varID2.isActive(), equalTo(false));
    }

    /**
     * Test the subsystem specification
     */
    @Test
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
     * test role map creation
     *
     * @throws IOException
     */
    @Test
    public void testFamilyMap() throws IOException {
        // Create a projector with a subsystem.
        SubsystemProjector projector = new SubsystemProjector();
        SubsystemSpec sub1 = new SubsystemSpec("Two cell division clusters relating to chromosome partitioning");
        sub1.addRole("Chromosome (plasmid) partitioning protein ParB");
        sub1.addRole("Chromosome partition protein smc");
        sub1.addRole("Ribonuclease III (EC 3.1.26.3)");
        sub1.addRole("Ribosomal large subunit pseudouridine synthase B (EC 5.4.99.22)");
        sub1.addRole("Segregation and condensation protein A");
        sub1.addRole("Segregation and condensation protein B");
        sub1.addRole("Signal recognition particle associated protein");
        sub1.addRole("Signal recognition particle protein Ffh");
        sub1.addRole("Signal recognition particle receptor FtsY");
        sub1.addRole("16S rRNA (cytosine(1402)-N(4))-methyltransferase (EC 2.1.1.199)");
        sub1.addRole("Cell division initiation protein DivIVA");
        sub1.addRole("Cell division integral membrane protein, YggT and half-length relatives");
        sub1.addRole("Cell division protein FtsA");
        sub1.addRole("Cell division protein FtsI [Peptidoglycan synthetase] (EC 2.4.1.129)");
        sub1.addRole("Cell division protein FtsL");
        sub1.addRole("Cell division protein FtsQ");
        sub1.addRole("Cell division protein FtsZ");
        sub1.addRole("Phospho-N-acetylmuramoyl-pentapeptide-transferase (EC 2.7.8.13)");
        sub1.addRole("Pyridoxal phosphate-containing protein YggS");
        sub1.addRole("SepF, FtsZ-interacting protein related to cell division");
        sub1.addRole("UDP-N-acetylglucosamine--N-acetylmuramyl-(pentapeptide) pyrophosphoryl-undecaprenol N-acetylglucosamine transferase (EC 2.4.1.227)");
        sub1.addRole("UDP-N-acetylmuramate--L-alanine ligase (EC 6.3.2.8)");
        sub1.addRole("UDP-N-acetylmuramoyl-tripeptide--D-alanyl-D-alanine ligase (EC 6.3.2.10)");
        sub1.addRole("UDP-N-acetylmuramoylalanine--D-glutamate ligase (EC 6.3.2.9)");
        sub1.addRole("UDP-N-acetylmuramoylalanyl-D-glutamate--L-lysine ligase (EC 6.3.2.7)");
        projector.addSubsystem(sub1);
        assertThat(sub1.getRoleIndex("Cell division initiation protein DivIVA"), equalTo(10));
        assertThat(sub1.getRoleIndex("Signal recognition particle protein Ffh"), equalTo(7));
        assertThat(sub1.getRoleIndex("Signal recognition particle protein invalid"), equalTo(-1));
        Genome gto = new Genome(new File("data/gto_test", "1313.7001.gto"));
        Map<String, Set<String>> gRoleMap = projector.computeRoleMap(gto);
        // Verify that all the features with known roles are found.
        RoleMap usefulRoles = projector.usefulRoles();
        for (Feature feat : gto.getFeatures()) {
            for (Role role : feat.getUsefulRoles(usefulRoles)) {
                String fid = feat.getId();
                assertThat(fid, gRoleMap.get(role.getId()), hasItem(fid));
            }
        }
        // Verify that all roles have valid features.
        for (Map.Entry<String, Set<String>> roleEntry : gRoleMap.entrySet()) {
            String roleId = roleEntry.getKey();
            Set<String> fids = roleEntry.getValue();
            for (String fid : fids) {
                Feature feat = gto.getFeature(fid);
                assertThat(fid, feat, not(nullValue()));
                List<String> roles = feat.getUsefulRoles(usefulRoles).stream().map(r -> r.getId()).collect(Collectors.toList());
                assertThat(fid, roles, hasItem(roleId));
            }
        }
    }

    /**
     * test the variant specification
     *
     * @throws IOException
     */
    @Test
    public void testVariantSpec() throws IOException {
        SubsystemProjector projector = new SubsystemProjector();
        // Create a subsystem found in 1313.7001.
        SubsystemSpec subsystem = new SubsystemSpec("Cell division related cluster including coaD");
        subsystem.setClassifications("", "Clustering-based subsystems", "Cell Division");
        subsystem.addRole("16S rRNA (guanine(966)-N(2))-methyltransferase (EC 2.1.1.171)"); //16sRrnaNMeth
        subsystem.addRole("Cell-division-associated, ABC-transporter-like signaling protein FtsE"); // CellDiviAssoAbcTran
        subsystem.addRole("Cell-division-associated, ABC-transporter-like signaling protein FtsX"); // CellDiviAssoAbcTran2
        subsystem.addRole("Phosphopantetheine adenylyltransferase (EC 2.7.7.3)"); // PhosAden
        subsystem.addRole("Signal recognition particle receptor FtsY"); // SignRecoPartRece
        subsystem.addRole("Hydroxychloroquine-denial protein"); // HydrDeniProt
        projector.addSubsystem(subsystem);
        VariantSpec variant = new VariantSpec(subsystem, "likely");
        assertThat(variant.getName(), equalTo(subsystem.getName()));
        assertThat(variant.getCode(), equalTo("likely"));
        // This is a test for doing compares and stuff.  We will continue to build the variant-spec in "variant".
        VariantSpec variant2 = new VariantSpec(subsystem, "likely");
        variant.setCell(1, projector);
        assertThat(variant2, not(equalTo(variant)));
        assertThat(variant2, greaterThan(variant));
        assertThat(variant.getKeyRole(), equalTo("CellDiviAssoAbcTran"));
        variant2.setCell(1, projector);
        assertThat(variant2, equalTo(variant));
        assertThat(variant2.compareTo(variant), equalTo(0));
        assertThat(variant2.getCellCount(), equalTo(1));
        variant2.setCell(2, projector);
        assertThat(variant2, not(equalTo(variant)));
        assertThat(variant2.compareTo(variant), not(equalTo(0)));
        assertThat(variant2.getCellCount(), equalTo(2));
        variant2.setCell(4, projector);
        assertThat(variant2.getCellCount(), equalTo(3));
        variant.setCell(0, projector);
        assertThat(variant.getKeyRole(), equalTo("16sRrnaNMeth"));
        assertThat(variant, greaterThan(variant2));
        variant2.setCell(5, projector);
        variant.setCell(2, projector);
        assertThat(variant.getKeyRole(), equalTo("16sRrnaNMeth"));
        variant.setCell(3, projector);
        assertThat(variant.getKeyRole(), equalTo("16sRrnaNMeth"));
        variant.setCell(4, projector);
        assertThat(variant.getKeyRole(), equalTo("16sRrnaNMeth"));
        assertThat(variant.getCellCount(), equalTo(5));
        assertThat(variant.isRedundant(variant2), equalTo(true));
        VariantSpec variant3 = new VariantSpec(subsystem, "active");
        assertThat(variant.isRedundant(variant3), equalTo(true));
        assertThat(variant3.isRedundant(variant), equalTo(true));
        SubsystemSpec sub2 = new SubsystemSpec("Funny subsystem");
        variant3 = new VariantSpec(sub2, "likely");
        assertThat(variant.isRedundant(variant3), equalTo(false));
        sub2 = new SubsystemSpec(subsystem.getName());
        sub2.addRole("16S rRNA (guanine(966)-N(2))-methyltransferase (EC 2.1.1.171)");
        sub2.addRole("Cell-division-associated, ABC-transporter-like signaling protein FtsE");
        sub2.addRole("Cell-division-associated, ABC-transporter-like signaling protein FtsX");
        sub2.addRole("Phosphopantetheine adenylyltransferase (EC 2.7.7.3)");
        sub2.addRole("Signal recognition particle receptor FtsY");
        sub2.addRole("Hydroxychloroquine-denial protein");
        VariantSpec variant4 = new VariantSpec(sub2, "likely");
        variant4.setCell(1, projector);
        variant4.setCell(0, projector);
        variant4.setCell(2, projector);
        variant4.setCell(3, projector);
        variant4.setCell(4, projector);
        assertThat(variant4, equalTo(variant));
        assertThat(variant4.compareTo(variant), equalTo(0));
        assertThat(variant.compareTo(variant4), equalTo(0));
        assertThat(projector.addVariant(variant), equalTo(true));
        assertThat(projector.addVariant(variant4), equalTo(false));
        // Test matching and projecting using the genome family map.
        Genome gto = new Genome(new File("data/gto_test", "1313.7001.gto"));
        Map<String, Set<String>> familyMap = projector.computeRoleMap(gto);
        assertThat(variant.matches(familyMap), equalTo(true));
        assertThat(variant2.matches(familyMap), equalTo(false));
        // Get the current subsystem row for this subsystem.
        SubsystemRow current = gto.getSubsystem(variant.getName());
        // Clear the genome's subsystems.
        gto.clearSubsystems();
        SubsystemRow created = variant.instantiate(gto, familyMap);
        assertThat(created == gto.getSubsystem(variant.getName()), equalTo(true));
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
    @Test
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
        var1.setCell(0, projector);
        var1.setCell(1, projector);
        var1.setCell(2, projector);
        var1.setCell(3, projector);
        var1.setCell(4, projector);
        var1.setCell(5, projector);
        var1.setCell(6, projector);
        var1.setCell(7, projector);
        projector.addVariant(var1);
        // This is designed to stress the role map save and restore.
        SubsystemSpec sub2 = new SubsystemSpec("fake subsystem 1");
        sub2.addRole("SSU rRNAtest");
        sub2.addRole("V-type H+-transporting ATPase subunit A (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit B (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit C (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit D (EC 7.1.2.2)");
        projector.addSubsystem(sub2);
        VariantSpec varX = new VariantSpec(sub2, "XX");
        varX.setCell(0, projector);
        varX.setCell(2, projector);
        varX.setCell(3, projector);
        projector.addVariant(varX);
        sub2 = new SubsystemSpec("fake subsystem 2");
        sub2.addRole("SSU rRNAtest2");
        projector.addSubsystem(sub2);
        // This next adds a second subsystem to test mass projection.
        sub2 = new SubsystemSpec("V-type ATP synthase");
        sub2.setClassifications("Respiration", "ATP synthases", "");
        sub2.addRole("V-type H+-transporting ATPase subunit A (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit B (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit C (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit D (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit E (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit epsilon (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit F (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit G (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit I (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit K (EC 7.1.2.2)");
        sub2.addRole("V-type H+-transporting ATPase subunit H (EC 7.1.2.2)");
        projector.addSubsystem(sub2);
        // We put two variants of different sizes to make sure the correct one is projected.
        VariantSpec var2 = new VariantSpec(sub2, "active");
        var2.setCell(0, projector);
        var2.setCell(1, projector);
        var2.setCell(2, projector);
        var2.setCell(3, projector);
        var2.setCell(4, projector);
        var2.setCell(6, projector);
        var2.setCell(7, projector);
        var2.setCell(8, projector);
        var2.setCell(9, projector);
        projector.addVariant(var2);
        VariantSpec var3 = new VariantSpec(sub2, "likely");
        var3.setCell(1, projector);
        var3.setCell(2, projector);
        var3.setCell(3, projector);
        var3.setCell(8, projector);
        projector.addVariant(var3);
        VariantSpec var4 = new VariantSpec(sub2, "unlikely");
        var4.setCell(0, projector);
        var4.setCell(1, projector);
        var4.setCell(2, projector);
        var4.setCell(3, projector);
        var4.setCell(4, projector);
        var4.setCell(5, projector);
        var4.setCell(6, projector);
        var4.setCell(7, projector);
        var4.setCell(8, projector);
        var4.setCell(9, projector);
        projector.addVariant(var4);
        // And here is a variant that won't match.
        // Save and restore to check the role map.
        File outFile = new File("data", "projector.ser");
        projector.save(outFile);
        SubsystemProjector loaded = SubsystemProjector.load(outFile);
        assertThat(loaded.getRoleId("SSU rRNA"), equalTo(ssuRrna));
        assertThat(loaded.getRoleId("LSU rRNA"), equalTo(lsuRrna));
        // Map the test genome.
        Genome gto = new Genome(new File("data","123214.3.gto"));
        Map<String, Set<String>> familyMap = projector.computeRoleMap(gto);
        // Test matching.
        assertThat(var1.matches(familyMap), equalTo(true));
        assertThat(var2.matches(familyMap), equalTo(true));
        assertThat(var3.matches(familyMap), equalTo(true));
        assertThat(var4.matches(familyMap), equalTo(false));
        // Test projection.
        projector.project(gto);
        assertThat(gto.getSubsystems().size(), equalTo(2));
        SubsystemRow row = gto.getSubsystem("Antibiotic targets in protein synthesis");
        assertThat(row.getVariantCode(), equalTo("1"));
        List<SubsystemRow.Role> rowRoles = row.getRoles();
        for (SubsystemRow.Role rowRole : rowRoles)
            assertThat(rowRole.getFeatures().size(), greaterThan(0));
        row = gto.getSubsystem("V-type ATP synthase");
        assertThat(row.getVariantCode(), equalTo("active"));
        rowRoles = row.getRoles();
        assertThat(rowRoles.get(0).getFeatures().size(), greaterThan(0));
        assertThat(rowRoles.get(1).getFeatures().size(), greaterThan(0));
        assertThat(rowRoles.get(2).getFeatures().size(), greaterThan(0));
        assertThat(rowRoles.get(3).getFeatures().size(), greaterThan(0));
        assertThat(rowRoles.get(4).getFeatures().size(), greaterThan(0));
        assertThat(rowRoles.get(6).getFeatures().size(), greaterThan(0));
        assertThat(rowRoles.get(7).getFeatures().size(), greaterThan(0));
        assertThat(rowRoles.get(8).getFeatures().size(), greaterThan(0));
        assertThat(rowRoles.get(9).getFeatures().size(), greaterThan(0));
        row = gto.getSubsystem("fake subsystem 1");
        assertThat(row, nullValue());
    }

    /**
     * test creating, saving, and loading a projector
     *
     * @throws IOException
     */
    @Test
    public void testProjectorIO() throws IOException {
        SubsystemProjector projector = new SubsystemProjector();
        SubsystemSpec sub1 = new SubsystemSpec("subsystem 1");
        sub1.setClassifications("a1", "b1", "");
        sub1.addRole("s1 role 0");
        sub1.addRole("s1 role 1 / s1 role 4");
        sub1.addRole("s1 role 2");
        sub1.addRole("s1 role 3");
        projector.addSubsystem(sub1);
        VariantSpec var1_1 = new VariantSpec(sub1, "1");
        var1_1.setCell(1, projector);
        var1_1.setCell(3, projector);
        projector.addVariant(var1_1);
        VariantSpec var1_2 = new VariantSpec(sub1, "2");
        var1_2.setCell(0, projector);
        var1_2.setCell(1, projector);
        var1_2.setCell(2, projector);
        projector.addVariant(var1_2);
        VariantSpec var1_3 = new VariantSpec(sub1, "3");
        var1_3.setCell(1, projector);
        var1_3.setCell(2, projector);
        var1_3.setCell(3, projector);
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
        var2_1.setCell(0, projector);
        var2_1.setCell(1, projector);
        var2_1.setCell(2, projector);
        var2_1.setCell(3, projector);
        var2_1.setCell(4, projector);
        projector.addVariant(var2_1);
        VariantSpec var2_2 = new VariantSpec(sub2, "2");
        var2_2.setCell(0, projector);
        var2_2.setCell(2, projector);
        var2_2.setCell(4, projector);
        projector.addVariant(var2_2);
        VariantSpec var2_3 = new VariantSpec(sub2, "3");
        var2_3.setCell(2, projector);
        var2_3.setCell(3, projector);
        projector.addVariant(var2_3);
        VariantSpec var2_4 = new VariantSpec(sub2, "4");
        var2_4.setCell(1, projector);
        projector.addVariant(var2_4);
        File saveFile = new File("data", "projector.ser");
        projector.save(saveFile);
        SubsystemProjector loaded = SubsystemProjector.load(saveFile);
        SubsystemSpec sub = loaded.getSubsystem("subsystem 1");
        assertThat(sub, equalTo(sub1));
        assertThat(sub.getClassifications(), contains("a1", "b1", ""));
        assertThat(sub.getRoles(), contains("s1 role 0", "s1 role 1 / s1 role 4", "s1 role 2", "s1 role 3"));
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
