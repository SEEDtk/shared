/**
 *
 */
package org.theseed.rna;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;

/**
 * @author Bruce Parrello
 *
 */
public class TestRnaData {

    @Test
    public void testSaveLoad() throws IOException, ClassNotFoundException {
        Genome gto = new Genome(new File("data", "MG1655-wild.gto"));
        RnaData testRna = new RnaData();
        RnaData.JobData job0 = testRna.addJob("job1", 1.0, 0.1, "old1", true);
        job0.setBaseCount(1000000);
        job0.setReadCount(1000);
        job0.setQuality(11.1);
        job0.setProcessingDate(LocalDate.of(2021, 1, 11));
        testRna.addJob("job2", 2.0, 0.2, "old2", true);
        testRna.addJob("job0", Double.NaN, Double.NaN, "old0", false);
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
        testRna.storeRegulonData(f1.getId(), 1, "mod1,mod2", "secMA-mutT");
        testRna.storeRegulonData(f3.getId(), 2, "", "");
        Iterator<RnaData.Row> iter = testRna.iterator();
        rowX = iter.next();
        RnaFeatureData fX = rowX.getFeat();
        assertThat(fX.getId(), equalTo(f1.getId()));
        assertThat(fX.getLocation(), equalTo(f1.getLocation()));
        assertThat(fX.getFunction(), equalTo(f1.getPegFunction()));
        assertThat(fX.getGene(), equalTo("hslU"));
        assertThat(fX.getBNumber(), equalTo("b3931"));
        assertThat(fX.getAtomicRegulon(), equalTo(1));
        assertThat(fX.getiModulons(), arrayContaining("mod1", "mod2"));
        assertThat(fX.getOperon(), equalTo("secMA-mutT"));
        RnaData.Weight weightX = rowX.getWeight(0);
        assertThat(weightX.getWeight(), equalTo(101.0));
        assertThat(weightX.isExactHit(), equalTo(false));
        fX = rowX.getNeighbor();
        assertThat(fX.getId(), equalTo(f2.getId()));
        rowX = iter.next();
        fX = rowX.getFeat();
        assertThat(fX.getId(), equalTo(f3.getId()));
        assertThat(rowX.getNeighbor(), nullValue());
        assertThat(fX.getAtomicRegulon(), equalTo(2));
        assertThat(fX.getiModulons().length, equalTo(0));
        File saveFile = new File("data", "rna.ser");
        testRna.save(saveFile);
        RnaData fileRna = RnaData.load(saveFile);
        assertThat(fileRna.size(), equalTo(testRna.size()));
        List<RnaData.JobData> jobs = testRna.getSamples();
        List<RnaData.JobData> fileJobs = fileRna.getSamples();
        assertThat(fileJobs, contains(jobs.toArray()));
        Optional<RnaData.JobData> job1Check = jobs.stream().filter(x -> x.getName().contentEquals("job1")).findFirst();
        assertThat(job1Check.isPresent(), equalTo(true));
        RnaData.JobData job1 = job1Check.get();
        assertThat(job1.getName(), equalTo("job1"));
        assertThat(job1.getProduction(), equalTo(1.0));
        assertThat(job1.isSuspicious(), equalTo(true));
        assertThat(job1.getBaseCount(), equalTo(1000000L));
        assertThat(job1.getReadCount(), equalTo(1000));
        assertThat(job1.getProcessingDate(), equalTo(LocalDate.of(2021, 1, 11)));
        assertThat(job1.getQuality(), closeTo(11.1, 0.001));
        Optional<RnaData.JobData> jobFCheck = fileJobs.stream().filter(x -> x.getName().contentEquals("job1")).findFirst();
        assertThat(jobFCheck.isPresent(), equalTo(true));
        RnaData.JobData jobF = jobFCheck.get();
        assertThat(job1.getOldName(), equalTo("old1"));
        assertThat(jobF.getOldName(), equalTo("old1"));
        iter = testRna.iterator();
        for (RnaData.Row rowF : fileRna) {
            row = iter.next();
            RnaFeatureData feat = row.getFeat();
            RnaFeatureData featF = rowF.getFeat();
            assertThat(featF, equalTo(feat));
            assertThat(featF.getLocation(), equalTo(feat.getLocation()));
            assertThat(featF.getFunction(), equalTo(feat.getFunction()));
            assertThat(featF.getGene(), equalTo(feat.getGene()));
            assertThat(featF.getBNumber(), equalTo(feat.getBNumber()));
            assertThat(featF.getOperon(), equalTo(feat.getOperon()));
            assertThat(featF.getAtomicRegulon(), equalTo(feat.getAtomicRegulon()));
            String[] iMods = feat.getiModulons();
            String[] iModsF = featF.getiModulons();
            assertThat(iModsF.length, equalTo(iMods.length));
            for (int i = 0; i < iModsF.length; i++)
                assertThat(Integer.toString(i), iModsF[i], equalTo(iMods[i]));
            assertThat(rowF.getNeighbor(), equalTo(row.getNeighbor()));
            for (int i = 0; i < 3; i++)
                assertThat(rowF.getWeight(i), equalTo(row.getWeight(i)));
        }
        // Now test the subset copy.
        List<String> subset = Arrays.asList("job2", "job1");
        RnaData subRna = testRna.getSubset(subset);
        assertThat(subRna.rows(), equalTo(testRna.rows()));
        assertThat(subRna.size(), equalTo(2));
        // Verify the jobs.
        for (RnaData.JobData subJob : subRna.jobs) {
            RnaData.JobData testJob = testRna.getJob(subJob.getName());
            String sample = subJob.getName();
            assertThat(sample, testJob, not(nullValue()));
            assertThat(sample, subJob.getBaseCount(), equalTo(testJob.getBaseCount()));
            assertThat(sample, subJob.getCoverage(100), equalTo(testJob.getCoverage(100)));
            assertThat(sample, subJob.getExpressedPercent(), equalTo(testJob.getExpressedPercent()));
            assertThat(sample, subJob.getMeanReadLen(), equalTo(testJob.getMeanReadLen()));
            assertThat(sample, subJob.getName(), equalTo(testJob.getName()));
            assertThat(sample, subJob.getOldName(), equalTo(testJob.getOldName()));
            assertThat(sample, subJob.getOpticalDensity(), equalTo(testJob.getOpticalDensity()));
            assertThat(sample, subJob.getProcessingDate(), equalTo(testJob.getProcessingDate()));
            assertThat(sample, subJob.getProduction(), equalTo(testJob.getProduction()));
            assertThat(sample, subJob.getQuality(), equalTo(testJob.getQuality()));
            assertThat(sample, subJob.getReadCount(), equalTo(testJob.getReadCount()));
            assertThat(sample, subJob.isGood(), equalTo(testJob.isGood()));
            assertThat(sample, subJob.isSuspicious(), equalTo(testJob.isSuspicious()));
        }
        // Verify the features.
        for (RnaData.Row subRow : subRna.getRows()) {
            String fid = subRow.getFeat().getId();
            RnaData.Row testRow = testRna.getRow(fid);
            assertThat(fid, testRow, not(nullValue()));
            RnaFeatureData subFeat = subRow.getFeat();
            RnaFeatureData testFeat = testRow.getFeat();
            assertThat(fid, subFeat.getAtomicRegulon(), equalTo(testFeat.getAtomicRegulon()));
            assertThat(fid, subFeat.getBaseLine(), equalTo(testFeat.getBaseLine()));
            assertThat(fid, subFeat.getBNumber(), equalTo(testFeat.getBNumber()));
            assertThat(fid, subFeat.getFunction(), equalTo(testFeat.getFunction()));
            assertThat(fid, subFeat.getGene(), equalTo(testFeat.getGene()));
            assertThat(fid, subFeat.getId(), equalTo(testFeat.getId()));
            assertThat(fid, subFeat.getiModulons(), arrayContainingInAnyOrder(testFeat.getiModulons()));
            assertThat(fid, subFeat.getLocation(), equalTo(testFeat.getLocation()));
            assertThat(fid, subFeat.getOperon(), equalTo(testFeat.getOperon()));
            // Verify the weights.
            for (String jobName : subset) {
                int subIdx = subRna.getColIdx(jobName);
                int testIdx = testRna.getColIdx(jobName);
                assertThat(fid + "/" + jobName, subRow.getWeight(subIdx), equalTo(testRow.getWeight(testIdx)));
            }
        }
    }

    @Test
    public void testNormalize() throws IOException {
        Genome gto = new Genome(new File("data", "MG1655-wild.gto"));
        RnaData data = new RnaData();
        data.addJob("job1", 0.1, 1.0, "old1", true);
        data.addJob("job2", 0.2, 2.0, "old2", true);
        data.addJob("job3", 0.3, 3.0, "old3", false);
        data.addJob("job4", 0.4, 4.0, "old4", true);
        Feature f1 = gto.getFeature("fig|511145.183.peg.3580"); // Universal stress protein
        Feature f2 = gto.getFeature("fig|511145.183.peg.2251"); // SSU rRNA
        Feature f3 = gto.getFeature("fig|511145.183.peg.4072"); // LSU ribosomal
        Feature f4 = gto.getFeature("fig|511145.183.peg.4078"); // Thiazole synthase
        Feature f5 = gto.getFeature("fig|511145.183.peg.4076"); // Heat shock protein C
        Feature f6 = gto.getFeature("fig|511145.183.peg.4074"); // DNA-directed RNA polymerase
        Feature r1 = gto.getFeature("fig|511145.183.rna.2"); // pure RNA
        RnaData.Row row = data.getRow(f1, null);
        row.store("job1", true, 10000.0);
        row.store("job2", true, 20000.0);
        row.store("job3", true, 30000.0);
        row.store("job4", true, 40000.0);
        row = data.getRow(f2, null);
        row.store("job1", true, 1000.0);
        row.store("job2", true, 2000.0);
        row.store("job3", true, 3000.0);
        row.store("job4", true, 4000.0);
        row = data.getRow(f3, null);
        row.store("job1", true, 100.0);
        row.store("job2", true, 200.0);
        row.store("job3", true, 300.0);
        row.store("job4", true, 400.0);
        row = data.getRow(f4, null);
        row.store("job1", true, 20000.0);
        row.store("job2", true, 30000.0);
        row.store("job3", true, 40000.0);
        row.store("job4", true, 10000.0);
        row = data.getRow(f5, null);
        row.store("job1", true, 30000.0);
        row.store("job2", true, 40000.0);
        row.store("job3", true, 10000.0);
        row.store("job4", true, 20000.0);
        row = data.getRow(f6, null);
        row.store("job1", true, 40000.0);
        row.store("job2", true, 10000.0);
        row.store("job3", true, 20000.0);
        row.store("job4", true, 30000.0);
        row = data.getRow(r1, null);
        row.store("job1", true, 1.0);
        row.store("job2", true, 2.0);
        row.store("job3", true, 3.0);
        row.store("job4", true, 4.0);
        assertThat(data.rows(), equalTo(7));
        data.normalize();
        assertThat(data.rows(), equalTo(6));
        RnaFeatureData feat = new RnaFeatureData(f3);
        row = data.getRow(feat.getId());
        assertThat(row.getWeight(0).getWeight(), closeTo(100000000 / 101100.0, 0.1));
        assertThat(row.getWeight(1).getWeight(), closeTo(200000000 / 102200.0, 0.1));
        assertThat(row.getWeight(2).getWeight(), closeTo(300000000 / 103300.0, 0.1));
        assertThat(row.getWeight(3).getWeight(), closeTo(400000000 / 104400.0, 0.1));
        feat = new RnaFeatureData(r1);
        row = data.getRow(feat.getId());
        assertThat(row, nullValue());
        Map<String, Double> baseMap = data.getBaselines();
        assertThat(baseMap, not(nullValue()));
    }

}
