/**
 *
 */
package org.theseed.shared;

import junit.framework.TestCase;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.theseed.io.Shuffler;
import org.theseed.reports.NaturalSort;
import org.theseed.utils.MultiParms;
import org.theseed.utils.Parms;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Bruce Parrello
 *
 */
public class ParmsTest extends TestCase {

    /**
     * test parm reader
     *
     * @throws IOException
     */
    public void testParms() throws IOException {
        File parmFile = new File("data", "parms.tbl");
        List<String> parms = Parms.fromFile(parmFile);
        assertThat("Invalid parms result.", parms, contains("-z", "-t", "10", "--bins", "this is a long string",
                "-tab", "used here"));
    }

    /**
     * test parm iterator
     *
     * @throws IOException
     */
    public void testMultiParms() throws IOException {
        File parmFile = new File("data", "parms2.tbl");
        MultiParms parmIterator = new MultiParms(parmFile);
        List<String> parms = parmIterator.next();
        assertThat(parms, contains("--digits", "1", "--constant", "X", "--letters", "a", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "1", "--constant", "X", "--letters", "b", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "1", "--constant", "X", "--letters", "c", "--batch"));
        assertTrue(parmIterator.hasNext());
        assertThat(parmIterator.toString(), equalTo("--digits 1; --letters c"));
        HashMap<String, String> varMap = parmIterator.getVariables();
        assertThat(varMap.get("--digits"), equalTo("1"));
        assertThat(varMap.get("--letters"), equalTo("c"));
        assertThat(varMap.size(), equalTo(2));
        assertThat(parmIterator.getOptions(), arrayContaining("--digits", "--letters"));
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "2", "--constant", "X", "--letters", "a", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "2", "--constant", "X", "--letters", "b", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "2", "--constant", "X", "--letters", "c", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "3", "--constant", "X", "--letters", "a", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "3", "--constant", "X", "--letters", "b", "--batch"));
        assertTrue(parmIterator.hasNext());
        parms = parmIterator.next();
        assertThat(parms, contains("--digits", "3", "--constant", "X", "--letters", "c", "--batch"));
        assertFalse(parmIterator.hasNext());
    }

    /**
     * test parm object
     */
    public void testParms2() {
        Parms newParms = new Parms();
        newParms.set("--digits", 12);
        newParms.set("--batch");
        List<String> parmList = newParms.get();
        assertThat(parmList, contains("--batch", "--digits", "12"));
        parmList = newParms.get("Command");
        assertThat(parmList, contains("Command", "--batch", "--digits", "12"));
        newParms.set("--maxE", 1e-10);
        parmList = newParms.get();
        assertThat(parmList, contains("--batch", "--digits", "12", "--maxE", "1.0E-10"));
        newParms.set("-v");
        parmList = newParms.get();
        assertThat(parmList, contains("-v", "--batch", "--digits", "12", "--maxE", "1.0E-10"));
        newParms.set("--long", "this has \"spaces\"");
        newParms.set("--long2", "this has <less than");
        newParms.set("--long3", "this has >greater than");
        newParms.set("--fid", "fig|83333.1.peg.4");
        String parmString = newParms.toString();
        assertThat(parmString,
                equalTo("-v --batch --digits 12 --fid \"fig|83333.1.peg.4\" " +
                        "--long \"this has \\\"spaces\\\"\" " +
                        "--long2 \"this has <less than\" " +
                        "--long3 \"this has >greater than\" " +
                        "--maxE 1.0E-10"));
        Parms parms2 = newParms.clone();
        parmString = parms2.toString();
        assertThat(parmString,
                equalTo("-v --batch --digits 12 --fid \"fig|83333.1.peg.4\" " +
                        "--long \"this has \\\"spaces\\\"\" " +
                        "--long2 \"this has <less than\" " +
                        "--long3 \"this has >greater than\" " +
                        "--maxE 1.0E-10"));
        parms2.set("--input", "frog");
        parmList = newParms.get();
        assertThat(parmList, not(hasItem("--input")));
        parmList = parms2.get();
        assertThat(parmList, hasItem("--input"));
    }

    /**
     * test the natural sort
     */
    public void testSort() {
        NaturalSort comparator = new NaturalSort();
        assertThat(comparator.compare("abs1_10", "abs1_2"), greaterThan(0));
        assertThat(comparator.compare("abc1_10", "abs1_2"), lessThan(0));
        assertThat(comparator.compare("fig|8333.2.peg,12", "fig|8333.10.peg.10"), lessThan(0));
        assertThat(comparator.compare("NODE_10_covg_32", "NODE_2_covg_12"), greaterThan(0));
        assertThat(comparator.compare("abcde", "abcee"), lessThan(0));
        assertThat(comparator.compare("abcdf", "abcd"), greaterThan(0));
        assertThat(comparator.compare("abc10", "abc10"), equalTo(0));
        assertThat(comparator.compare("abc10", "abc100"), lessThan(0));
        String[] sortArray = new String[] { "abcdefg1", "absdefg2", "abcdefg2", "abcdefg10", "abcdefg20", "abs1_10", "abs1_2",
                "1004", "", "abcdef", "200" };
        Shuffler<String> sortTest = new Shuffler<String>(Arrays.asList(sortArray));
        sortTest.shuffle(10);
        sortTest.sort(comparator);
        assertThat(sortTest, contains("", "200", "1004", "abcdef", "abcdefg1", "abcdefg2", "abcdefg10",
                "abcdefg20", "abs1_2", "abs1_10", "absdefg2"));
    }

}
