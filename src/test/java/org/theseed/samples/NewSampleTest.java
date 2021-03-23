/**
 *
 */
package org.theseed.samples;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.theseed.io.LineReader;
import org.theseed.io.TabbedLineReader;

/**
 * @author Bruce Parrello
 *
 */
public class NewSampleTest {

    @Test
    public void test() throws IOException {
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

}
