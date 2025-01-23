/**
 *
 */
package org.theseed.subsystems.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Genome;
import org.theseed.subsystems.StrictRoleMap;

/**
 * @author Bruce Parrello
 *
 */
class TestRuleProjector {

    @Test
    void testSubsystemDescriptorProject() throws IOException, ParseFailureException {
        StrictRoleMap roleMap = StrictRoleMap.load(new File("data/ss_test/Subsystems", "core.roles.in.subsystems"));
        File inDir = new File("data/ss_test/Subsystems", "Histidine_Biosynthesis");
        CoreSubsystem sub = new CoreSubsystem(inDir, roleMap);
        SubsystemDescriptor desc = new SubsystemDescriptor(sub);
        Genome gto = new Genome(new File("data/ss_test/Gtos", "319225.3.gto"));
        gto.clearSubsystems();
        var roleSet = roleMap.getRolePresenceMap(gto);
        String vCode = desc.project(gto, roleSet, roleMap);
        assertThat(vCode, equalTo("likely"));
        vCode = desc.projectActive(gto, roleSet, roleMap);
        assertThat(vCode, nullValue());
        gto = new Genome(new File("data/ss_test/Gtos", "1045004.4.gto"));
        gto.clearSubsystems();
        vCode = desc.project(gto, roleSet, roleMap);
        assertThat(vCode, equalTo("active.1.1"));
        gto = new Genome(new File("data/ss_test/Gtos", "483218.5.gto"));
        gto.clearSubsystems();
        vCode = desc.project(gto, roleSet, roleMap);
        assertThat(vCode, equalTo("dirty"));
    }

}
