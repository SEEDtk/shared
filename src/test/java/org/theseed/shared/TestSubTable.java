package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.genome.SubsystemRow;
import org.theseed.subsystems.GenomeSubsystemTable;
import org.theseed.subsystems.SubsystemRowDescriptor;

public class TestSubTable {

    @Test
    public void testBuildTable() throws IOException {
        Genome myGto = new Genome(new File("data", "gto_test/1313.7001.gto"));
        File subFile = new File("data", "subsystems.ser");
        SubsystemRowDescriptor.createFile(myGto, subFile);
        GenomeSubsystemTable subTable = new GenomeSubsystemTable(subFile);
        for (Feature feat : myGto.getPegs()) {
            Set<String> subs = feat.getSubsystems();
            Collection<GenomeSubsystemTable.SubData> subItems = subTable.getSubsystems(feat.getId());
            if (subs.size() == 0) {
                assertThat(subItems, nullValue());
            } else {
                int count = 0;
                for (GenomeSubsystemTable.SubData subData : subItems) {
                    String subName = subData.getName();
                    assertThat(feat.getId(), subs, hasItem(subName));
                    SubsystemRow subRow = myGto.getSubsystem(subName);
                    assertThat(feat.getId(), subRow.getClassifications(), contains(subData.getClasses()));
                    count++;
                }
                assertThat(count, equalTo(subs.size()));
            }
        }
    }

}
