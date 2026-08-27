package org.theseed.sequence;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Test;
import org.theseed.io.TabbedInputStream;

public class TestContigMd5 {

    @Test
    public void testMd5() throws Exception {
        File testFile = new File("data", "md5Test.tbl");
        try (TabbedInputStream inStream = new TabbedInputStream(testFile)) {
            MD5Hex md5 = new MD5Hex();
            int md5ColIdx = inStream.findField("sequence_md5");
            int seqColIdx = inStream.findField("sequence");
            for (var line : inStream) {
                String sequence = line.get(seqColIdx);
                String expected = line.get(md5ColIdx);
                String actual = md5.contigMD5(sequence);
                assertThat(actual, equalTo(expected));
            }
        }
    }

}
