/**
 *
 */
package org.theseed.samples;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.theseed.test.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.theseed.io.TabbedLineReader;

/**
 * @author Bruce Parrello
 *
 */
public class SampleTest {

    @Test
    public void idTest() {
        SampleId samp1 = new SampleId("7_D_TasdA_P_asdD_zwf_DasdDdapA_I_6_M1");
        assertThat(samp1.toStrain(), equalTo("7_D_TasdA_P_asdD_zwf_DasdDdapA"));
        assertThat(samp1.toString(), equalTo("7_D_TasdA_P_asdD_zwf_DasdDdapA_I_6_M1"));
        assertThat(samp1.getTimePoint(), equalTo(6.0));
        SampleId samp2 = new SampleId("7_D_TasdA_P_asdD_zwf_DasdDdapA_I_4p5_M1");
        assertThat(samp2.toStrain(), equalTo("7_D_TasdA_P_asdD_zwf_DasdDdapA"));
        assertThat(samp2.toString(), equalTo("7_D_TasdA_P_asdD_zwf_DasdDdapA_I_4p5_M1"));
        assertThat(samp2.getTimePoint(), equalTo(4.5));
        assertThat(samp2, lessThan(samp1));
        assertThat(samp2.getDeletes(), contains("asd", "dapA"));
        assertThat(samp2.isIPTG(), isTrue());
        samp2 = new SampleId("7_0_0_A_asdO_000_D000_0_12_M1");
        assertThat(samp2.getDeletes().size(), equalTo(0));
        assertThat(samp2.isIPTG(), isFalse());
        SampleId samp3 = new SampleId("7_0_0_A_asdO_000_D000_0_12_M1_rep1");
        assertThat(samp2.repBaseId(), equalTo(samp3.repBaseId()));
    }

    @Test
    public void testSamplePattern() throws IOException {
        File testFile = new File("data", "idMap.tbl");
        try (TabbedLineReader testStream = new TabbedLineReader(testFile)) {
            for (TabbedLineReader.Line line : testStream) {
                String strain = line.get(1);
                boolean iptg = line.getFlag(3);
                double time = line.getDouble(4);
                String medium = line.get(5);
                SampleId sample = SampleId.translate(strain, time, iptg, medium);
                assertThat(strain, sample.toString(), equalTo(line.get(2)));
            }
        }
    }

    @Test
    public void testRnaFileNames() throws IOException {
        File rnaFile = new File("data", "rnaSamps.txt");
        try (TabbedLineReader reader = new TabbedLineReader(rnaFile, 2)) {
            // Each input line contains a file name followed by the sample ID string.
            for (TabbedLineReader.Line line : reader) {
                File file1 = new File("data", line.get(0));
                SampleId sample1 = SampleId.translate(file1);
                assertThat(sample1.toString(), equalTo(line.get(1)));
            }
        }
        rnaFile = new File("data", "rnaNums.txt");
        try (TabbedLineReader reader = new TabbedLineReader(rnaFile, 2)) {
            // Each input line contains a file name followed by the sample number string.
            for (TabbedLineReader.Line line : reader) {
                File file1 = new File("data", line.get(0));
                String num = SampleId.getSampleNumber(file1);
                assertThat(num, equalTo(line.get(1)));
            }
        }

    }

    @Test
    public void testIncrement() {
        SampleId test = new SampleId("7_0_0_A_asdO_000_D000_0_12_M1");
        test.increment();
        assertThat(test.toString(), equalTo("7_0_0_A_asdO_000_D000_0_12_M1_rep1"));
        test.increment();
        assertThat(test.toString(), equalTo("7_0_0_A_asdO_000_D000_0_12_M1_rep2"));
        test.increment();
        assertThat(test.toString(), equalTo("7_0_0_A_asdO_000_D000_0_12_M1_rep3"));
    }
}
