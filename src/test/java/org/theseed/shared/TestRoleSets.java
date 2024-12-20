/**
 *
 */
package org.theseed.shared;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.theseed.genome.Genome;
import org.theseed.genome.SubsystemRow;
import org.theseed.proteins.RoleSet;
import org.theseed.subsystems.SubsystemProjector;
import org.theseed.subsystems.SubsystemSpec;
import org.theseed.subsystems.VariantSpec;

/**
 * @author Bruce Parrello
 *
 */
public class TestRoleSets {

    @Test
    public void testEmptySets() {
        RoleSet emptySet = RoleSet.fromString("");
        assertThat(emptySet.size(), equalTo(0));
        assertThat(emptySet, sameInstance(RoleSet.NO_ROLES));
        RoleSet single = RoleSet.fromString("abc");
        assertThat(emptySet, not(equalTo(single)));
        assertThat(emptySet.toString(), equalTo(""));
        Iterator<String> emptyIter = emptySet.iterator();
        assertThat(emptyIter.hasNext(), equalTo(false));
        assertThat(emptySet.isEmpty(), equalTo(true));
    }

    @Test
    public void testMultiRoleProjection() throws IOException {
        Genome gto = new Genome(new File("data/gto_test", "1313.7001.gto"));
        SubsystemProjector projector = new SubsystemProjector();
        SubsystemSpec subsystem = new SubsystemSpec("Test subsystem");
        subsystem.addRole("Peptide-methionine (S)-S-oxide reductase MsrA (EC 1.8.4.11) / Peptide-methionine (R)-S-oxide reductase MsrB (EC 1.8.4.12)");
        subsystem.addRole("Lipoate carrier protein");
        subsystem.addRole("Arginine peptide dehydrogenase");
        subsystem.addRole("Phenylalanyl-tRNA synthetase alpha chain");
        subsystem.addRole("Alanine racemase / Glycosyltransferase");
        projector.addSubsystem(subsystem);
        Map<String, Set<String>> roleMap = projector.computeRoleMap(gto);
        VariantSpec variant = new VariantSpec(subsystem, "1");
        variant.setCell(0, projector);
        variant.setCell(1, projector);
        variant.setCell(3, projector);
        projector.addVariant(variant);
        assertThat(variant.matches(roleMap), equalTo(true));
        variant = new VariantSpec(subsystem, "2");
        variant.setCell(1,  projector);
        variant.setCell(3, projector);
        variant.setCell(4, projector);
        projector.addVariant(variant);
        assertThat(variant.matches(roleMap), equalTo(false));
        projector.project(gto);
        SubsystemRow projected = gto.getSubsystem("Test subsystem");
        assertThat(projected.getVariantCode(), equalTo("1"));
        List<SubsystemRow.Role> roles = projected.getRoles();
        assertThat(roles.get(0).getFeatures().stream().map(f -> f.getId()).collect(Collectors.toSet()),
                containsInAnyOrder("fig|1313.7001.peg.2040", "fig|1313.7001.peg.970"));
        assertThat(roles.get(1).getFeatures().stream().map(f -> f.getId()).collect(Collectors.toSet()),
                containsInAnyOrder("fig|1313.7001.peg.1889"));
        assertThat(roles.get(2).getFeatures().size(), equalTo(0));
        assertThat(roles.get(3).getFeatures().stream().map(f -> f.getId()).collect(Collectors.toSet()),
                containsInAnyOrder("fig|1313.7001.peg.897"));
        assertThat(roles.get(4).getFeatures().size(), equalTo(0));
    }

    @Test
    public void testRoleSetMethods() {
        RoleSet set1 = RoleSet.fromString("Bbbb,Aaaa");
        RoleSet set2 = RoleSet.fromString("Cccc");
        RoleSet set3 = RoleSet.fromString("");
        RoleSet set4 = RoleSet.fromString("Bbbb,Cccc");
        RoleSet set5 = RoleSet.fromString("Bbbb,Cccc");
        RoleSet set6 = RoleSet.fromString("Cccc,Bbbb");
        assertThat(set1, not(equalTo(set4)));
        assertThat(set4, equalTo(set5));
        assertThat(set2, not(equalTo(set1)));
        assertThat(set1.toString(), equalTo("Bbbb,Aaaa"));
        assertThat(set2.toString(), equalTo("Cccc"));
        assertThat(set1.compareTo(set2), lessThan(0));
        assertThat(set2.compareTo(set3), lessThan(0));
        assertThat(set1.compareTo(set4), lessThan(0));
        assertThat(set2.compareTo(set1), greaterThan(0));
        assertThat(set3.isEmpty(), equalTo(true));
        assertThat(set1.isEmpty(), equalTo(false));
        assertThat(set2.isEmpty(), equalTo(false));
        Iterator<String> iter = set4.iterator();
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo("Bbbb"));
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo("Cccc"));
        assertThat(iter.hasNext(), equalTo(false));
        assertThat(set1.size(), equalTo(2));
        assertThat(set2.size(), equalTo(1));
        RoleSet[] roleArray = new RoleSet[] { set5, set4, set3, set1, set2 };
        assertThat(RoleSet.min(roleArray), equalTo("Aaaa"));
        assertThat(set1.contains(set2), equalTo(false));
        assertThat(set4.contains(set2), equalTo(true));
        assertThat(set4.contains(set1), equalTo(false));
        assertThat(set1.contains(set3), equalTo(true));
        assertThat(set3.contains(set1), equalTo(false));
        assertThat(set5.contains(set6), equalTo(true));
    }
}
