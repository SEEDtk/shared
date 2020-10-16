/**
 *
 */
package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.theseed.test.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.rna.RnaData;

/**
 * @author Bruce Parrello
 *
 */
public class TestRnaData {

    @Test
    public void testSaveLoad() throws IOException, ClassNotFoundException {
        Genome gto = new Genome(new File("data", "MG1655-wild.gto"));
        RnaData testRna = new RnaData();
        testRna.addJob("job1", 1.0, 0.1);
        testRna.addJob("job2", 2.0, 0.2);
        testRna.addJob("job0", Double.NaN, Double.NaN);
        assertThat(testRna.size(), equalTo(3));
        Feature f1 = gto.getFeature("fig|511145.183.peg.4025");
        Feature f2 = gto.getFeature("fig|511145.183.peg.494");
        Feature f3 = gto.getFeature("fig|511145.183.peg.4026");
        RnaData.Row row = testRna.getRow(f1, f2);
        row.store("job2", true, 102.0);
        row.store("job1", false, 101.0);
        RnaData.Row rowX = testRna.getRow(f1, f3);
        assertThat(rowX, sameInstance(row));
        row = testRna.getRow(f3, null);
        row.store("job2", true, 202.0);
        row.store("job0", false, 203.0);
        Iterator<RnaData.Row> iter = testRna.iterator();
        rowX = iter.next();
        RnaData.FeatureData fX = rowX.getFeat();
        assertThat(fX.getId(), equalTo(f1.getId()));
        assertThat(fX.getLocation(), equalTo(f1.getLocation()));
        assertThat(fX.getFunction(), equalTo(f1.getPegFunction()));
        RnaData.Weight weightX = rowX.getWeight(0);
        assertThat(weightX.getWeight(), equalTo(101.0));
        assertThat(weightX.isExactHit(), isFalse());
        fX = rowX.getNeighbor();
        assertThat(fX.getId(), equalTo(f2.getId()));
        rowX = iter.next();
        fX = rowX.getFeat();
        assertThat(fX.getId(), equalTo(f3.getId()));
        assertThat(rowX.getNeighbor(), nullValue());
        File saveFile = new File("data", "rna.ser");
        testRna.save(saveFile);
        RnaData fileRna = RnaData.load(saveFile);
        assertThat(fileRna.size(), equalTo(testRna.size()));
        List<RnaData.JobData> jobs = testRna.getSamples();
        List<RnaData.JobData> fileJobs = fileRna.getSamples();
        assertThat(fileJobs, contains(jobs.toArray()));
        iter = testRna.iterator();
        for (RnaData.Row rowF : fileRna) {
            row = iter.next();
            assertThat(rowF.getFeat(), equalTo(row.getFeat()));
            assertThat(rowF.getNeighbor(), equalTo(row.getNeighbor()));
            for (int i = 0; i < 3; i++)
                assertThat(rowF.getWeight(i), equalTo(row.getWeight(i)));
        }
    }

}
