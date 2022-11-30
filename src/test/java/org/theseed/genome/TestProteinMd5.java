package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.theseed.io.TabbedLineReader;

class TestProteinMd5 {

    @Test
    void testProtMD5() throws IOException {
        File gtoFile = new File("data", "511145.12.gto");
        Genome gto = new Genome(gtoFile);
        File testFile = new File("data", "prots.tbl");
        try (var inStream = new TabbedLineReader(testFile)) {
            for (var line : inStream) {
                String fid = line.get(1);
                String md5 = line.get(2);
                var feat = gto.getFeature(fid);
                String testMd5 = feat.getMD5();
                assertThat(fid, testMd5, equalTo(md5));
            }
        }
    }

}
