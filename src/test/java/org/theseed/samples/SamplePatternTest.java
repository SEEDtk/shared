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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.LineReader;

/**
 * @author Bruce Parrello
 */
public class SamplePatternTest {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SamplePatternTest.class);

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

}
