/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Parrello
 *
 */
class TestAliasMap {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TestAliasMap.class);


    @Test
    void testAliasMap() throws IOException {
        Genome genome = new Genome(new File("data", "MG1655-wild.gto"));
        var aliasMap = genome.getAliasMap();
        assertThat(aliasMap.get("hslU"), hasItems("fig|511145.183.peg.4025"));
        assertThat(aliasMap.get("b3931"), hasItems("fig|511145.183.peg.4025"));
        assertThat(aliasMap.get("b3932"), hasItems("fig|511145.183.peg.4026"));
        int dupCount = 0;
        for (Map.Entry<String, Set<String>> aliasEntry : aliasMap.entrySet()) {
            String alias = aliasEntry.getKey();
            if (aliasEntry.getValue().size() > 1)
                dupCount++;
            for (String fid : aliasEntry.getValue()) {
                Feature feat = genome.getFeature(fid);
                assertThat(fid, feat.getAliases(), hasItems(alias));
            }
        }
        log.info("{} aliases had multiple features.", dupCount);
    }

}
