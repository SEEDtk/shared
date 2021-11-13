/**
 *
 */
package org.theseed.samples;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.theseed.test.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.LineReader;
import org.theseed.io.TabbedLineReader;

/**
 * @author Bruce Parrello
 *
 */
public class SampleTest {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SampleTest.class);

    @Test
    public void testNewSamples() throws IOException {
        File fileFile = new File("data", "files.txt");
        try (LineReader reader = new LineReader(fileFile)) {
            for (String line : reader) {
                File rnaFile = new File("data", line);
                SampleId sample = SampleId.translate(rnaFile);
                assertThat(line, sample, not(nullValue()));
                String num = SampleId.getSampleNumber(rnaFile);
                assertThat(line, containsString(num));
                assertThat(num, startsWith("S"));
                if (line.contains("pta-asd"))
                    assertThat(sample.getFragment(4), equalTo("asdT"));
                if (line.contains("lysC"))
                    assertThat(sample.getDeletes(), contains("lysC"));
                if (line.contains("pta-thrABC"))
                    assertThat(sample.getFragment(2), equalTo("TA1"));
                if (line.contains("plus"))
                    assertThat(sample.isIPTG(), isTrue());
                log.info("Translation of {} = {}.", line, sample.toString());
            }
        }
    }

    @Test
    public void doubleStest() throws IOException {
        File dFile = new File("data", "rnaseq.list.txt");
        try (LineReader reader = new LineReader(dFile)) {
            for (String[] parts : reader.new Section(null)) {
                File lineFile = new File("data", parts[0]);
                SampleId sample = SampleId.translate(lineFile);
                String sampleString = sample.toString();
                if (parts.length == 3) {
                    assertThat(sampleString, equalTo(parts[1]));
                    assertThat(SampleId.getSampleNumber(lineFile), equalTo(parts[2]));
                } else
                    log.info("Translation of {} = {} with sample {}.", parts[0], sampleString, SampleId.getSampleNumber(lineFile));
            }
        }
    }

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
        samp1 = new SampleId("M_0_TA1_C_asdT_pntAB-aspC-ppc_DtdhDmetLDdapA_I_24_M1");
        samp2 = new SampleId("M_0_TA1_C_asdT_pntAB-aspC-ppc_DtdhDmetLDdapA_0_24_M1");
        samp3 = new SampleId("M_0_TA1_C_asdT_pntAB-aspC-ppc_DdapADmetLDtdh_I_24_M1");
        assertThat(samp1, not(equalTo(samp2)));
        assertThat(samp1, equalTo(samp3));
        assertThat(samp1.compareTo(samp3), equalTo(0));
        samp3 = new SampleId("M_0_TA1_C_asdT_pntAB-ppc-aspC_DtdhDmetLDdapA_I_24_M1");
        assertThat(samp1, equalTo(samp3));
        assertThat(samp1.compareTo(samp3), equalTo(0));
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
    public void testFileSamples() throws IOException {
        File strainsIn = new File("data", "RNAseq_3_prod.txt");
        List<String> strainList = TabbedLineReader.readColumn(strainsIn, "sample");
        assertThat(strainList.size(), equalTo(88));
        File targetFile = new File("data", "newSampleIds.txt");
        Set<String> targets = LineReader.readSet(targetFile);
        for (String strain : strainList) {
            SampleId sample = SampleId.translate(strain, "9");
            assertThat(strain, sample.toString(), in(targets));
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

    @Test
    public void testObscureCompare() {
        SampleId samp1 = new SampleId("M_0_TA1_C_asdT_000_DtdhDmetL_I_24_M1");
        SampleId samp1a = new SampleId("M_0_TA1_C_asdT_000_DmetLDtdh_I_24_M1");
        SampleId samp2 = new SampleId("M_0_TA1_C_asdT_000_DmetLDtdhDlysC_I_24_M1");
        SampleId samp3 = new SampleId("M_0_TA1_C_asdT_000_Dtdh_I_24_M1");
        SampleId samp4 = new SampleId("M_0_TA1_C_asdT_000_DmetLDtdh_0_24_M1");
        assertThat(samp1, equalTo(samp1a));
        assertThat(samp1, not(equalTo(samp2)));
        assertThat(samp1, not(equalTo(samp3)));
        assertThat(samp1, not(equalTo(samp4)));
        assertThat(samp1.hashCode(), equalTo(samp1a.hashCode()));
        assertThat(samp1.hashCode(), not(equalTo(samp2.hashCode())));
        assertThat(samp1.hashCode(), not(equalTo(samp3.hashCode())));
        assertThat(samp1.hashCode(), not(equalTo(samp4.hashCode())));
    }

    @Test
    public void testMultiInsert() {
        SampleId samp1 = new SampleId("M_0_TA1_C_asdT_metL-rhtA_Dtdh_I_24_M1");
        SampleId samp1a = new SampleId("M_0_TA1_C_asdT_rhtA-metL_Dtdh_I_24_M1");
        SampleId samp2 = new SampleId("M_0_TA1_C_asdT_rhtA_Dtdh_I_24_M1");
        SampleId samp3 = new SampleId("M_0_TA1_C_asdT_metL_Dtdh_I_24_M1");
        SampleId samp4 = new SampleId("M_0_TA1_C_asdT_metL-rhtA-lysC_Dtdh_0_24_M1");
        assertThat(samp1, equalTo(samp1a));
        assertThat(samp1, not(equalTo(samp2)));
        assertThat(samp1, not(equalTo(samp3)));
        assertThat(samp1, not(equalTo(samp4)));
        assertThat(samp1.hashCode(), equalTo(samp1a.hashCode()));
        assertThat(samp1.hashCode(), not(equalTo(samp2.hashCode())));
        assertThat(samp1.hashCode(), not(equalTo(samp3.hashCode())));
        assertThat(samp1.hashCode(), not(equalTo(samp4.hashCode())));
        assertThat(samp1.getInserts(), containsInAnyOrder("metL", "rhtA"));
        assertThat(samp1.unInsert("tdh"), equalTo("M_0_TA1_C_asdT_metL-rhtA_Dtdh_I_24_M1"));
        assertThat(samp1.unInsert("metL"), equalTo("M_0_TA1_C_asdT_rhtA_Dtdh_I_24_M1"));
        assertThat(samp3.unInsert("metL"), equalTo("M_0_TA1_C_asdT_000_Dtdh_I_24_M1"));
    }

    @Test
    public void testGeneric() {
        SampleId samp1 = new SampleId("M_0_TA1_C_asdT_metL-rhtA_Dtdh_I_24_M1");
        SampleId samp2 = new SampleId("M_D_TAasd_P_asdT_rhtA_Dtdh_I_24_M1");
        assertThat(samp1.genericOperon(), equalTo("M_X_X_X_asdT_metL-rhtA_Dtdh_I_24_M1"));
        assertThat(samp2.genericOperon(), equalTo("M_X_X_X_asdT_rhtA_Dtdh_I_24_M1"));
        SampleId samp1X = new SampleId("M_0_TA1_C_X_rhtA-metL_Dtdh_I_24_M1");
        SampleId samp2X = new SampleId("X_D_TAasd_P_asdT_rhtA_Dtdh_I_24_M1");
        assertThat(samp1.matches(samp1X), isTrue());
        assertThat(samp1.matches(samp2X), isFalse());
        assertThat(samp2.matches(samp2X), isTrue());
        assertThat(samp2.matches(samp1X), isFalse());
        assertThat(samp1X.matches(samp2X), isFalse());
    }

    @Test
    public void testModifications() {
        SampleId samp1 = new SampleId("M_0_TA1_C_asdT_metL-rhtA_Dtdh_I_24_M1");
        SampleId samp3 = new SampleId(samp1);
        assertThat(samp3, equalTo(samp1));
        SampleId samp4 = samp1.addInsert("metL");
        assertThat(samp4, equalTo(samp1));
        samp4 = samp4.addInsert("metL");
        assertThat(samp4, equalTo(samp1));
        SampleId samp2 = new SampleId("M_0_TA1_C_asdT_000_Dtdh_I_24_M1");
        samp4 = samp2.addInsert("metL");
        assertThat(samp4.toString(), equalTo("M_0_TA1_C_asdT_metL_Dtdh_I_24_M1"));
        samp4 = samp4.addInsert("ppc");
        assertThat(samp4.toString(), equalTo("M_0_TA1_C_asdT_metL-ppc_Dtdh_I_24_M1"));
        samp2 = new SampleId(("M_0_TA1_C_asdT_metL_D000_I_24_M1"));
        samp4 = samp2.addDelete("metL");
        assertThat(samp4.toString(), equalTo("M_0_TA1_C_asdT_metL_DmetL_I_24_M1"));
        samp4 = samp4.addDelete("ppc");
        assertThat(samp4.toString(), equalTo("M_0_TA1_C_asdT_metL_DmetLDppc_I_24_M1"));
        samp4 = samp4.addDelete("metL");
        assertThat(samp4.toString(), equalTo("M_0_TA1_C_asdT_metL_DmetLDppc_I_24_M1"));
    }
}
