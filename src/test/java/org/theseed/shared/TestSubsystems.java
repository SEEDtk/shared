package org.theseed.shared;

import org.theseed.stats.Shuffler;
import org.theseed.subsystems.SubsystemSpec;
import org.theseed.subsystems.VariantId;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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

}
