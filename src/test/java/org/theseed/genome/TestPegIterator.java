/**
 *
 */
package org.theseed.genome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.theseed.proteins.RoleMap;

/**
 * @author Bruce Parrello
 *
 */
class TestPegIterator {

    @Test
    void testInterestingIter() throws IOException {
        File gFile = new File("data/kmer_test", "1074889.3.gto");
        Genome genome = new Genome(gFile);
        RoleMap roleMap = RoleMap.load(new File("data", "roles.for.hammers"));
        Iterator<Feature> iter = genome.new InterestingPegs(roleMap);
        Collection<Feature> found = new ArrayList<Feature>(20);
        while (iter.hasNext())
            found.add(iter.next());
        // Insure we found the correct number of pegs.
        assertThat(found.size(), equalTo(10));
        // Insure they all qualify.
        for (Feature feat : found)
            assertThat(feat.getId(), feat.isInteresting(roleMap));
    }

}
