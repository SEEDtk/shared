/**
 *
 */
package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.theseed.genome.core.CoreUtilities;
import org.theseed.genome.core.OrganismDirectories;
import org.theseed.genome.core.PegList;
import org.theseed.sequence.Sequence;

import junit.framework.TestCase;

/**
 * @author Bruce Parrello
 *
 */
public class CoreTest extends TestCase {

    /**
     * Test the peg list
     * @throws IOException
     */
    public void testPegList() throws IOException {
        PegList testList = new PegList(new File("src/test/resources", "testP.fa"));
        Sequence found = testList.get("fig|1538.8.peg.30");
        assertNull(found);
        found = testList.get("fig|1538.8.peg.12");
        assertThat(found.getLabel(), equalTo("fig|1538.8.peg.12"));
        Sequence found7 = testList.get("fig|1538.8.peg.7");
        Sequence found2 = testList.get("fig|1538.8.peg.2");
        Sequence found3 = testList.get("fig|1538.8.peg.3");
        Sequence found10 = testList.get("fig|1538.8.peg.10");
        ArrayList<Sequence> buffer = new ArrayList<Sequence>();
        testList.findClose(found, 1, buffer);
        assertThat(buffer, contains(found2));
        testList.suppress(found2);
        testList.suppress(found3);
        testList.findClose(found, 2, buffer);
        assertThat(buffer, contains(found2, found7, found10));
        Sequence found1 = testList.get("fig|1538.8.peg.1");
        Sequence found4 = testList.get("fig|1538.8.peg.4");
        Sequence found5 = testList.get("fig|1538.8.peg.5");
        Sequence found6 = testList.get("fig|1538.8.peg.6");
        Sequence found8 = testList.get("fig|1538.8.peg.8");
        Sequence found9 = testList.get("fig|1538.8.peg.9");
        Sequence found11 = testList.get("fig|1538.8.peg.11");
        Sequence found13 = testList.get("fig|1538.8.peg.13");
        Sequence found14 = testList.get("fig|1538.8.peg.14");
        Sequence found15 = testList.get("fig|1538.8.peg.15");
        testList.suppress(found1);
        testList.suppress(found4);
        testList.suppress(found5);
        testList.suppress(found6);
        testList.suppress(found7);
        testList.suppress(found8);
        testList.suppress(found9);
        testList.suppress(found10);
        testList.suppress(found11);
        testList.suppress(found14);
        testList.suppress(found15);
        testList.findClose(found, 4, buffer);
        assertThat(buffer, contains(found2, found7, found10, found13));
    }

    /**
     * test organism directories
     */
    public void testOrgDir() {
        OrganismDirectories orgDir = new OrganismDirectories(new File("src/test/resources", "core"));
        assertThat(orgDir.size(), equalTo(3));
        Iterator<String> orgIter = orgDir.iterator();
        assertTrue(orgIter.hasNext());
        assertThat(orgIter.next(), equalTo("100.1"));
        assertTrue(orgIter.hasNext());
        assertThat(orgIter.next(), equalTo("200.2"));
        assertTrue(orgIter.hasNext());
        assertThat(orgIter.next(), equalTo("300.3"));
        assertFalse(orgIter.hasNext());
    }

    /**
     * test genome ID extraction
     */
    public void testGenomeIds() {
        assertThat(CoreUtilities.genomeOf("fig|12345.6.peg.10"), equalTo("12345.6"));
        assertThat(CoreUtilities.genomeOf("fig|23456.789.202.10"), equalTo("23456.789"));
        assertThat(CoreUtilities.genomeOf("patric|123.45.peg.10"), equalTo(null));
    }

}
