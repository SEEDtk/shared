/**
 *
 */
package org.theseed.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.theseed.counters.CountMap;
import org.theseed.stats.Shuffler;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
public class IoTests {

    /**
     * Test the balanced stream.
     *
     * @throws IOException
     */
    @Test
    public void testBalancedStream() throws IOException {
        File testFile = new File("data", "balanced.ser");
        BalancedOutputStream outStream = new BalancedOutputStream(1.5, testFile);
        outStream.writeImmediate("type", "text");
        outStream.write("a", "a1");
        outStream.write("a", "a2");
        outStream.write("a", "a3");
        outStream.write("a", "a4");
        outStream.write("a", "a5");
        outStream.write("a", "a6");
        outStream.write("a", "a7");
        outStream.write("b", "b1");
        outStream.write("b", "b2");
        outStream.write("b", "b3");
        outStream.write("b", "b4");
        outStream.write("b", "b5");
        outStream.write("b", "b6");
        outStream.write("b", "b7");
        outStream.write("b", "b8");
        outStream.write("b", "b9");
        outStream.write("b", "b10");
        outStream.write("b", "b11");
        outStream.write("b", "b12");
        outStream.write("b", "b13");
        outStream.write("b", "b14");
        outStream.write("b", "b15");
        outStream.write("b", "b16");
        outStream.write("b", "b17");
        outStream.write("b", "b18");
        outStream.write("b", "b19");
        outStream.write("b", "b20");
        outStream.write("c", "c1");
        outStream.write("c", "c2");
        outStream.write("c", "c3");
        outStream.write("c", "c4");
        outStream.write("c", "c5");
        outStream.write("c", "c6");
        outStream.write("c", "c7");
        outStream.write("c", "c8");
        outStream.close();
        // The file should contain 7 a, 10 b, and 8 c.
        TabbedLineReader reader = new TabbedLineReader(testFile);
        assertThat(reader.findField("type"), equalTo(0));
        assertThat(reader.findField("text"), equalTo(1));
        assertThat(reader.size(), equalTo(2));
        CountMap<String> counts = new CountMap<String>();
        for (TabbedLineReader.Line line : reader) {
            String label = line.get(0);
            String text = line.get(1);
            assertThat("Text \"" + text + "\" has wrong label.", StringUtils.startsWith(text, label));
            counts.count(label);
        }
        assertThat(counts.getCount("a"), equalTo(7));
        assertThat(counts.getCount("b"), equalTo(10));
        assertThat(counts.getCount("c"), equalTo(8));
        reader.close();
        // Verify the first half of the file has half the objects.
        reader = new TabbedLineReader(testFile);
        counts = new CountMap<String>();
        for (int i = 0; i < 13; i++) {
            TabbedLineReader.Line line = reader.next();
            String label = line.get(0);
            counts.count(label);
        }
        assertThat(counts.getCount("a"), equalTo(4));
        assertThat(counts.getCount("b"), equalTo(5));
        assertThat(counts.getCount("c"), equalTo(4));
        reader.close();
        outStream = new BalancedOutputStream(0, testFile);
        outStream.writeImmediate("type", "text");
        outStream.write("a", "a1");
        outStream.write("a", "a2");
        outStream.write("a", "a3");
        outStream.write("a", "a4");
        outStream.write("b", "b1");
        outStream.write("b", "b2");
        outStream.close();
        reader = new TabbedLineReader(testFile);
        TabbedLineReader.Line line = reader.next();
        assertThat(line.get(0), equalTo("a"));
        assertThat(line.get(1), equalTo("a1"));
        line = reader.next();
        assertThat(line.get(0), equalTo("a"));
        assertThat(line.get(1), equalTo("a2"));
        line = reader.next();
        assertThat(line.get(0), equalTo("a"));
        assertThat(line.get(1), equalTo("a3"));
        line = reader.next();
        assertThat(line.get(0), equalTo("a"));
        assertThat(line.get(1), equalTo("a4"));
        line = reader.next();
        assertThat(line.get(0), equalTo("b"));
        assertThat(line.get(1), equalTo("b1"));
        line = reader.next();
        assertThat(line.get(0), equalTo("b"));
        assertThat(line.get(1), equalTo("b2"));
        assertThat(reader.hasNext(), equalTo(false));
        reader.close();
        outStream = new BalancedOutputStream(1.2, testFile);
        BalancedOutputStream.setBufferMax(600);
        outStream.writeImmediate("type", "text");
        for (int i = 0; i < 200; i++) {
            outStream.write("a", "a1");
            outStream.write("b", "b1");
            outStream.write("b", "b1");
        }
        outStream.write("a", "a2");
        outStream.close();
        reader = new TabbedLineReader(testFile);
        int aCount = 0;
        int bCount = 0;
        for (int i = 0; i < 440; i++) {
            line = reader.next();
            String label = line.get(0);
            if (label.equals("a")) {
                aCount++;
                assertThat(line.get(1), equalTo("a1"));
            } else if (label.equals("b")) {
                bCount++;
                assertThat(line.get(1), equalTo("b1"));
            }
        }
        assertThat(aCount, equalTo(200));
        assertThat(bCount, equalTo(240));
        line = reader.next();
        assertThat(line.get(1), equalTo("a2"));
        reader.close();
    }

    /**
     * Test the column reader.
     *
     * @throws IOException
     */
    @Test
    public void testColumnRead() throws IOException {
        File inFile = new File("data", "tabset.tbl");
        List<String> roles = TabbedLineReader.readColumn(inFile, "role");
        assertThat(roles, contains("roleA", "roleB", "roleA", "roleC", "roleD", "roleE"));
    }

    /**
     * Test the shuffler
     */
    @Test
    public void testShuffler() {
        Shuffler<Integer> test1 = new Shuffler<Integer>(100);
        for (int i = 0; i < 100; i++)
            test1.add(i);
        for (int i = 0; i < 100; i++)
            assertThat(test1.get(i), equalTo(i));
        // Test the limited iterator.
        Iterator<Integer> limited = test1.limitedIter(5);
        assertThat(limited.hasNext(), equalTo(true));
        assertThat(limited.next(), equalTo(0));
        assertThat(limited.hasNext(), equalTo(true));
        assertThat(limited.next(), equalTo(1));
        assertThat(limited.hasNext(), equalTo(true));
        assertThat(limited.next(), equalTo(2));
        assertThat(limited.hasNext(), equalTo(true));
        assertThat(limited.next(), equalTo(3));
        assertThat(limited.hasNext(), equalTo(true));
        assertThat(limited.next(), equalTo(4));
        assertThat(limited.hasNext(), equalTo(false));
        // Test the shuffling.
        test1.shuffle(50);
        for (int i = 0; i < 100; i++) {
            assertThat(test1.get(i), lessThan(100));
            assertThat(test1.get(i), greaterThanOrEqualTo(0));
            for (int j = 0; j < 100; j++) {
                if (i != j) assertThat(test1.get(i), not(equalTo(test1.get(j))));
            }
        }
        // Insure the edge cases don't crash.
        test1.shuffle(200);
        test1.shuffle(101);
        test1.shuffle(99);
        // Test adding an iterable.
        ArrayList<Integer> array1 = new ArrayList<Integer>(5);
        array1.add(200);
        array1.add(201);
        array1.add(202);
        array1.add(203);
        array1.add(204);
        Shuffler<Integer> test2 = test1.addSequence(array1);
        assertThat(test1, sameInstance(test2));
        assertThat(test1.get(100), equalTo(200));
        assertThat(test1.get(101), equalTo(201));
        assertThat(test1.get(102), equalTo(202));
        assertThat(test1.get(103), equalTo(203));
        assertThat(test1.get(104), equalTo(204));
        assertThat(test1.size(), equalTo(105));
        test1 = new Shuffler<Integer>(array1);
        assertThat(test1.get(0), equalTo(200));
        assertThat(test1.get(1), equalTo(201));
        assertThat(test1.get(2), equalTo(202));
        assertThat(test1.get(3), equalTo(203));
        assertThat(test1.get(4), equalTo(204));
        assertThat(test1.size(), equalTo(5));
        Shuffler<Integer> test3 = test1.add1(300);
        assertThat(test3.size(), equalTo(6));
        assertThat(test3.get(5), equalTo(300));
        assertThat(test3 == test1, equalTo(true));
    }

    /**
     * Test the advanced set and get in the shuffler.
     */
    @Test
    public void testShufflerExtend() {
        Shuffler<String> test = new Shuffler<String>(10);
        assertThat(test.get(6), nullValue());
        test.set(3, "frog");
        assertThat(test.get(0), nullValue());
        assertThat(test.get(1), nullValue());
        assertThat(test.get(2), nullValue());
        assertThat(test.get(3), equalTo("frog"));
        test.set(1, "dog");
        assertThat(test.get(1), equalTo("dog"));
        assertThat(test.size(), equalTo(4));
        test.set(2, "hog");
        test.set(0, "woggle bird");
        assertThat(test, contains("woggle bird", "dog", "hog", "frog"));
        assertThat(test.size(), equalTo(4));
    }

    /**
     * Test list rotation.
     */
    @Test
    public void testShufflerRotate() {
        Shuffler<Integer> test = new Shuffler<Integer>(10);
        for (int i = 0; i < 10; i++)
            test.add(i);
        test.rotate(1, -5);
        assertThat(test.get(0), equalTo(0));
        assertThat(test.get(1), equalTo(6));
        assertThat(test.get(2), equalTo(7));
        assertThat(test.get(3), equalTo(8));
        assertThat(test.get(4), equalTo(9));
        assertThat(test.get(5), equalTo(1));
        assertThat(test.get(6), equalTo(2));
        assertThat(test.get(7), equalTo(3));
        assertThat(test.get(8), equalTo(4));
        assertThat(test.get(9), equalTo(5));
        test.rotate(1, 5);
        assertThat(test.get(0), equalTo(0));
        assertThat(test.get(1), equalTo(1));
        assertThat(test.get(2), equalTo(2));
        assertThat(test.get(3), equalTo(3));
        assertThat(test.get(4), equalTo(4));
        assertThat(test.get(5), equalTo(5));
        assertThat(test.get(6), equalTo(6));
        assertThat(test.get(7), equalTo(7));
        assertThat(test.get(8), equalTo(8));
        assertThat(test.get(9), equalTo(9));
        test.rotate(1, -4);
        assertThat(test.get(0), equalTo(0));
        assertThat(test.get(1), equalTo(5));
        assertThat(test.get(2), equalTo(6));
        assertThat(test.get(3), equalTo(7));
        assertThat(test.get(4), equalTo(8));
        assertThat(test.get(5), equalTo(9));
        assertThat(test.get(6), equalTo(1));
        assertThat(test.get(7), equalTo(2));
        assertThat(test.get(8), equalTo(3));
        assertThat(test.get(9), equalTo(4));
        test.rotate(1, 4);
        assertThat(test.get(0), equalTo(0));
        assertThat(test.get(1), equalTo(1));
        assertThat(test.get(2), equalTo(2));
        assertThat(test.get(3), equalTo(3));
        assertThat(test.get(4), equalTo(4));
        assertThat(test.get(5), equalTo(5));
        assertThat(test.get(6), equalTo(6));
        assertThat(test.get(7), equalTo(7));
        assertThat(test.get(8), equalTo(8));
        assertThat(test.get(9), equalTo(9));
        test.rotate(1,  -3);
        assertThat(test.get(0), equalTo(0));
        assertThat(test.get(1), equalTo(4));
        assertThat(test.get(2), equalTo(5));
        assertThat(test.get(3), equalTo(6));
        assertThat(test.get(4), equalTo(7));
        assertThat(test.get(5), equalTo(8));
        assertThat(test.get(6), equalTo(9));
        assertThat(test.get(7), equalTo(1));
        assertThat(test.get(8), equalTo(2));
        assertThat(test.get(9), equalTo(3));
        test.rotate(1, 3);
        assertThat(test.get(0), equalTo(0));
        assertThat(test.get(1), equalTo(1));
        assertThat(test.get(2), equalTo(2));
        assertThat(test.get(3), equalTo(3));
        assertThat(test.get(4), equalTo(4));
        assertThat(test.get(5), equalTo(5));
        assertThat(test.get(6), equalTo(6));
        assertThat(test.get(7), equalTo(7));
        assertThat(test.get(8), equalTo(8));
        assertThat(test.get(9), equalTo(9));
        test.rotate(1,  -2);
        assertThat(test.get(0), equalTo(0));
        assertThat(test.get(1), equalTo(3));
        assertThat(test.get(2), equalTo(4));
        assertThat(test.get(3), equalTo(5));
        assertThat(test.get(4), equalTo(6));
        assertThat(test.get(5), equalTo(7));
        assertThat(test.get(6), equalTo(8));
        assertThat(test.get(7), equalTo(9));
        assertThat(test.get(8), equalTo(1));
        assertThat(test.get(9), equalTo(2));
        test.rotate(1, 2);
        assertThat(test.get(0), equalTo(0));
        assertThat(test.get(1), equalTo(1));
        assertThat(test.get(2), equalTo(2));
        assertThat(test.get(3), equalTo(3));
        assertThat(test.get(4), equalTo(4));
        assertThat(test.get(5), equalTo(5));
        assertThat(test.get(6), equalTo(6));
        assertThat(test.get(7), equalTo(7));
        assertThat(test.get(8), equalTo(8));
        assertThat(test.get(9), equalTo(9));
        test = new Shuffler<Integer>(120);
        for (int i = 0; i < 120; i++)
            test.add(i);
        // Test rotating different sizes.
        for (int d = 1; d < 60; d++) {
            test.rotate(0, -d);
            for (int i = 0; i < 120 - d; i++)
                assertThat(String.format("Rotation -%d, position %d.", d, i), test.get(i), equalTo(i+d));
            for (int i = 0; i < d; i++) {
                int pos = 120 - d + i;
                assertThat(String.format("Rotation -%d, position %d.", d, pos), test.get(pos), equalTo(i));
            }
            test.rotate(0, d);
            for (int i = 0; i < 120; i++)
                assertThat(String.format("Rotation %d, position %d.", d, i), test.get(i), equalTo(i));
        }
    }

    /**
     * Test the tabbed input file.
     *
     * @throws IOException
     */
    @Test
    public void testTabbedFile() throws IOException {
        File inFile = new File("data", "tabbed.txt");
        TabbedLineReader tabReader = new TabbedLineReader(inFile);
        assertThat("Header line wrong.", tabReader.header(), equalTo("genome_id\tgenome.genome_name\tcounter.0\tfraction\tflag"));
        applyTabReaderTests(tabReader, "100.99\tname of 100.99\t10\t0.8");
        // Reopen to test iteration.
        tabReader = new TabbedLineReader(inFile);
        String[] testLabels = new String[] { "100.99", "200.20", "1000.6", "4" };
        int i = 0;
        for (TabbedLineReader.Line l : tabReader) {
            assertThat("Wrong record at index " + i, l.get(0), equalTo(testLabels[i]));
            i++;
        }
        assertThat("Wrong number of records", i, equalTo(4));
        tabReader.close();
        // Try with a string list.
        LineReader reader = new LineReader(inFile);
        Shuffler<String> tabStrings = new Shuffler<String>(100);
        tabStrings.addSequence(reader);
        reader.close();
        tabReader = new TabbedLineReader(tabStrings);
        applyTabReaderTests(tabReader, "100.99\tname of 100.99\t10\t0.8");
        // Try with comma delimiters.
        inFile = new File("data", "comma.txt");
        tabReader = new TabbedLineReader(inFile, ',');
        assertThat("Header line wrong.", tabReader.header(), equalTo("genome_id,genome.genome_name,counter.0,fraction,flag"));
        applyTabReaderTests(tabReader, "100.99,name of 100.99,10,0.8");
        // Reopen to test iteration.
        tabReader = new TabbedLineReader(inFile, ',');
        testLabels = new String[] { "100.99", "200.20", "1000.6", "4" };
        i = 0;
        for (TabbedLineReader.Line l : tabReader) {
            assertThat("Wrong record at index " + i, l.get(0), equalTo(testLabels[i]));
            i++;
        }
        assertThat("Wrong number of records", i, equalTo(4));
        tabReader.close();
        // Test headerless files.
        File fixFile = new File("data", "fixed.txt");
        tabReader = new TabbedLineReader(fixFile, 5);
        TabbedLineReader.Line line = tabReader.next();
        assertThat("Wrong value in column 0 of line 1", line.get(0), equalTo("100.99"));
        assertThat("Wrong value in column 2 of line 1", line.getInt(2), equalTo(10));
        assertThat("Wrong value in column 3 of line 1", line.getDouble(3), closeTo(0.8, 0.0001));
        assertThat("Boolean adjustment fail in line 1", line.getFlag(4), equalTo(false));
        assertThat("Line input not working", line.getAll(), equalTo("100.99\tname of 100.99\t10\t0.8"));
        line = tabReader.next();
        assertThat("Wrong value in column 2 of line 2", line.getInt(2), equalTo(-4));
        assertThat("Wrong value in column 3 of line 2", line.getDouble(3), equalTo(12.0));
        assertThat("Wrong value in column 4 of line 2", line.getFlag(4), equalTo(true));
        line = tabReader.next();
        assertThat("Wrong value in column 4 of line 3", line.getFlag(4), equalTo(false));
        assertThat("End of file not detected", tabReader.hasNext(), equalTo(false));
        assertThat("Error reading past end-of-file", tabReader.next(), nullValue());
        tabReader.close();
    }

    @Test
    public void testTabbedStream() throws IOException {
        File testFile = new File("data", "tabbed.txt");
        try (TabbedLineReader inStream = new TabbedLineReader(testFile)) {
            double[] nums = inStream.stream().filter(x -> ! x.get(0).isBlank())
                    .mapToDouble(x -> x.getInt(2) * x.getDouble(3)).toArray();
            assertThat(nums[0], closeTo(8.0, 0.0001));
            assertThat(nums[1], closeTo(-48.0, 0.0001));
            assertThat(nums[2], equalTo(0.0));
            assertThat(nums[3], closeTo(30.0, 0.0001));
            assertThat(nums.length, equalTo(4));
        }
    }

    /**
     * @param tabReader
     * @throws IOException
     */
    private void applyTabReaderTests(TabbedLineReader tabReader, String testLine) throws IOException {
        assertThat("Wrong number of columns.", tabReader.size(), equalTo(5));
        // Test the column finder.
        assertThat("Did not find genome name.", tabReader.findField("genome_name"), equalTo(1));
        assertThat("Did not find fraction.", tabReader.findField("fraction"), equalTo(3));
        assertThat("Could not find column 3.", tabReader.findField("3"), equalTo(2));
        assertThat("Cound not find last column.", tabReader.findField("0"), equalTo(4));
        int colIdx = 0;
        try {
            colIdx = tabReader.findField("genome");
            fail("Found genome column at " + colIdx);
        } catch (IOException e) {
            // this is good
        }
        try {
            colIdx = tabReader.findField("10");
            fail("Found out-of-bounds column at " + colIdx);
        } catch (IOException e) {
            // this is good
        }
        try {
            colIdx = tabReader.findField("-6");
            fail("Found out-of-bounds column at " + colIdx);
        } catch (IOException e) {
            // this is good
        }
        TabbedLineReader.Line line = tabReader.next();
        assertThat("Wrong value in column 0 of line 1", line.get(0), equalTo("100.99"));
        assertThat("Wrong value in column 2 of line 1", line.getInt(2), equalTo(10));
        assertThat("Wrong value in column 3 of line 1", line.getDouble(3), closeTo(0.8, 0.0001));
        assertThat("Boolean adjustment fail in line 1", line.getFlag(4), equalTo(false));
        assertThat("Line input not working", line.getAll(), equalTo(testLine));
        line = tabReader.next();
        assertThat("Wrong value in column 2 of line 2", line.getInt(2), equalTo(-4));
        assertThat("Wrong value in column 3 of line 2", line.getDouble(3), equalTo(12.0));
        assertThat("Wrong value in column 4 of line 2", line.getFlag(4), equalTo(true));
        line = tabReader.next();
        assertThat("Wrong value in column 4 of line 3", line.getFlag(4), equalTo(false));
        line = tabReader.next();
        assertThat("Blank column failure.", line.getInt(2), equalTo(5));
        assertThat("End of file not detected", tabReader.hasNext(), equalTo(false));
        assertThat("Error reading past end-of-file", tabReader.next(), nullValue());
        tabReader.close();
    }


    /**
     * test marker files
     */
    @Test
    public void testMarkers() {
        File marker = new File("data", "marker.ser");
        MarkerFile.write(marker, "abc");
        assertThat(MarkerFile.read(marker), equalTo("abc"));
        MarkerFile.write(marker, 1024);
        assertThat(MarkerFile.readInt(marker), equalTo(1024));
    }

    /**
     * test string set files
     *
     * @throws IOException
     */
    @Test
    public void testStringSet() throws IOException {
        File setFile = new File("data", "set.txt");
        Set<String> strings = LineReader.readSet(setFile);
        assertThat(strings.size(), equalTo(4));
        assertThat(strings, containsInAnyOrder("value 1", "value 2", "value 3", "value 4"));
        File tabFile = new File("data", "tabset.tbl");
        strings = TabbedLineReader.readSet(tabFile, "role");
        assertThat(strings.size(), equalTo(5));
        assertThat(strings, containsInAnyOrder("roleA", "roleB", "roleC", "roleD", "roleE"));
    }

    /**
     * test gto file filter
     */
    @Test
    public void testGtoFilter() {
        File gtoDir = new File("data", "gto_test");
        File[] files = GtoFilter.getAll(gtoDir);
        assertThat(files.length, equalTo(5));
        List<File> list = Arrays.asList(files);
        assertThat(list, containsInAnyOrder(new File("data/gto_test", "1005394.4.gto"),
                new File("data/gto_test", "1313.7001.gto"),
                new File("data/gto_test", "1313.7002.gto"),
                new File("data/gto_test", "1313.7016.gto"),
                new File("data/gto_test", "243277.26.gto")));
    }

    /**
     * Test shuffled stream.
     *
     * @throws IOException
     */
    @Test
    public void testShuffleStream() throws IOException {
        File testFile = new File("data", "balanced.ser");
        try (ShuffledOutputStream outStream = new ShuffledOutputStream(4.5, "yes", "no", testFile)) {
            outStream.writeImmediate("type", "value");
            outStream.write("no", "1");
            outStream.write("no", "2");
            outStream.write("yes", "3");
            outStream.write("yes", "4");
            outStream.write("yes", "5");
            outStream.write("yes", "6");
            outStream.write("yes", "7");
            outStream.write("yes", "8");
            outStream.write("no", "9");
            outStream.write("no", "10");
            outStream.write("no", "11");
            outStream.write("no", "12");
            outStream.write("no", "13");
            outStream.write("no", "14");
            outStream.write("no", "15");
            outStream.write("no", "16");
            outStream.write("no", "17");
            outStream.write("no", "18");
            outStream.write("no", "19");
            outStream.write("no", "20");
            outStream.write("no", "21");
            outStream.write("no", "22");
            outStream.write("no", "23");
            outStream.write("no", "24");
            outStream.write("no", "25");
            outStream.write("no", "26");
            outStream.write("no", "27");
            outStream.write("no", "28");
            outStream.write("no", "29");
            outStream.write("no", "30");
            outStream.write("no", "31");
            outStream.write("no", "32");
            outStream.write("no", "0");
        }
        // Re-open the file and test the read.
        try (TabbedLineReader inStream = new TabbedLineReader(testFile)) {
            boolean[] found = new boolean[33];
            for (int i = 0; i < 33; i++) found[i] = false;
            Iterator<TabbedLineReader.Line> inIter = inStream.iterator();
            assertThat("Start of file.", inIter.hasNext());
            TabbedLineReader.Line line = inIter.next();
            assertThat(line.get(0), equalTo("yes"));
            found[line.getInt(1)] = true;
            double smallCount = 1.0;
            double largeCount = 0.0;
            while (inIter.hasNext()) {
                line = inIter.next();
                int idx = line.getInt(1);
                assertThat(Integer.toString(idx), ! found[idx]);
                found[idx] = true;
                switch (line.get(0)) {
                case "yes" :
                    smallCount++;
                    break;
                case "no" :
                    largeCount++;
                    break;
                default :
                    assertThat("Invalid label: " + line.get(0), false);
                }
                assertThat(Integer.toString(idx), largeCount / smallCount, lessThanOrEqualTo(4.5));
            }
            // Verify we read back everything.
            for (int i = 0; i < 33; i++)
                assertThat(Integer.toString(i), found[i]);
        }
    }

    /**
     * test sections
     *
     * @throws IOException
     */
    @Test
    public void testLineReaderIter() throws IOException {
        File testFile = new File("data", "lrTest.tbl");
        LineReader testStream = new LineReader(testFile);
        Iterator<String[]> iter2 = testStream.new SectionIter("//", "\t");
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("a", "a1", "a2"));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("b", "b1", "b2"));
        assertThat(iter2.hasNext(), equalTo(false));
        assertThat(iter2.hasNext(), equalTo(false));
        iter2 = testStream.new SectionIter("//", "\t");
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("c", "", "c2", "", ""));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("d", "d2"));
        assertThat(iter2.hasNext(), equalTo(false));
        iter2 = testStream.new SectionIter("//", "\t");
        assertThat(iter2.hasNext(), equalTo(false));
        iter2 = testStream.new SectionIter("//", "\t");
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("e", "e1", "", "e2"));
        assertThat(iter2.hasNext(), equalTo(false));
        testStream.close();
        testStream = new LineReader(testFile);
        Iterator<String> iter = testStream.iterator();
        testStream.skipSection("//");
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo("c\t\tc2\t\t"));
        testStream.skipSection("//");
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo("//"));
        testStream.skipSection("//");
        assertThat(iter.hasNext(), equalTo(false));
        testStream.close();
        testStream = new LineReader(testFile);
        testStream.skipSection("//");
        iter2 = testStream.new SectionIter(null, "\t");
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("c", "", "c2", "", ""));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("d", "d2"));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("//"));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("//"));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("e", "e1", "", "e2"));
        assertThat(iter2.hasNext(), equalTo(false));
        testStream.close();
        testFile = new File("data", "lrtest.csv");
        testStream = new LineReader(testFile);
        iter2 = testStream.new SectionIter("//", ",");
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("a", "a1", "a2"));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("b", "b1", "b2"));
        assertThat(iter2.hasNext(), equalTo(false));
        assertThat(iter2.hasNext(), equalTo(false));
        iter2 = testStream.new SectionIter("//", ",");
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("c", "", "c2", "", ""));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("d", "d2"));
        assertThat(iter2.hasNext(), equalTo(false));
        iter2 = testStream.new SectionIter("//", ",");
        assertThat(iter2.hasNext(), equalTo(false));
        iter2 = testStream.new SectionIter("//", ",");
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("e", "e1", "", "e2"));
        assertThat(iter2.hasNext(), equalTo(false));
        testStream.close();
        testStream = new LineReader(testFile);
        iter = testStream.iterator();
        testStream.skipSection("//");
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo("c,,c2,,"));
        testStream.skipSection("//");
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo("//"));
        testStream.skipSection("//");
        assertThat(iter.hasNext(), equalTo(false));
        testStream.close();
        testStream = new LineReader(testFile);
        testStream.skipSection("//");
        iter2 = testStream.new SectionIter(null, ",");
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("c", "", "c2", "", ""));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("d", "d2"));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("//"));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("//"));
        assertThat(iter2.hasNext(), equalTo(true));
        assertThat(iter2.next(), arrayContaining("e", "e1", "", "e2"));
        assertThat(iter2.hasNext(), equalTo(false));
        testStream.close();
    }

    @Test
    public void testCsvReader() throws IOException {
        File csvFile = new File("data", "quotes.csv");
        try (CsvLineReader reader = new CsvLineReader(csvFile)) {
            assertThat(reader.size(), equalTo(4));
            assertThat(reader.getLabels(), arrayContaining("a", "b", "c", "d"));
            assertThat(reader.hasNext(), equalTo(true));
            var line = reader.next();
            assertThat(line.get(0), equalTo("1 2,3"));
            assertThat(line.getInt(1), equalTo(4));
            assertThat(line.get(2), equalTo("5\"6\"7"));
            assertThat(line.getInt(3), equalTo(888));
            assertThat(reader.hasNext(), equalTo(true));
            line = reader.next();
            assertThat(line.get(0), equalTo(""));
            assertThat(line.get(1), equalTo("b2"));
            assertThat(line.get(2), equalTo(""));
            assertThat(line.get(3), equalTo("d2"));
            assertThat(reader.hasNext(), equalTo(true));
            line = reader.next();
            assertThat(line.get(0), equalTo("123"));
            assertThat(line.get(1), equalTo("45\",6"));
            assertThat(line.get(2), equalTo("7"));
            assertThat(line.get(3), equalTo(""));
            assertThat(reader.hasNext(), equalTo(false));
        }
    }
}
