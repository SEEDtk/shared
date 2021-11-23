/**
 *
 */
package org.theseed.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.theseed.test.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestKeyedFileMap {

    @Test
    public void testCharFile() throws IOException {
        KeyedFileMap outMap = new KeyedFileMap("letter");
        outMap.addHeaders(Arrays.asList("main.plant", "animal"));
        outMap.addRecord("a", Arrays.asList("Apple", "Aardvark"));
        outMap.addRecord("p", Arrays.asList("Petunia", "Panther"));
        outMap.addRecord("d", Arrays.asList("Dog", "Daffodil"));
        outMap.addRecord("c", Arrays.asList("Cat", "Carrot"));
        outMap.addHeaders(Arrays.asList("machine"));
        assertThat(outMap.width(), equalTo(4));
        assertThat(outMap.findColumn("PLANT"), equalTo(1));
        assertThat(outMap.findColumn("mineral"), equalTo(-1));
        assertThat(outMap.findColumn("letter"), equalTo(0));
        Iterator<Map.Entry<String, List<String>>> iter = outMap.iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<String>> current = iter.next();
            switch (current.getKey()) {
            case "a" :
                current.getValue().add("Abacus");
                break;
            case "p" :
                iter.remove();
                break;
            case "d" :
                current.getValue().add("");
                break;
            case "c" :
                current.getValue().add("Clock");
                break;
            }
        }
        File testFile = new File("data", "outFile.ser");
        outMap.write(testFile);
        try (LineReader testStream = new LineReader(testFile)) {
            String line = testStream.next();
            assertThat(line, equalTo("letter\tmain.plant\tanimal\tmachine"));
            line = testStream.next();
            assertThat(line, equalTo("a\tApple\tAardvark\tAbacus"));
            line = testStream.next();
            assertThat(line, equalTo("d\tDog\tDaffodil\t"));
            line = testStream.next();
            assertThat(line, equalTo("c\tCat\tCarrot\tClock"));
            assertThat(testStream.hasNext(), isFalse());
        }
    }

    @Test
    public void testNumFile() {
        KeyedFileMap keyMap = new KeyedFileMap("key");
        keyMap.addHeaders(Arrays.asList("val1", "val2"));
        keyMap.addRecord("a", Arrays.asList("100.5", "100.6"));
        keyMap.addRecord("b", Arrays.asList("200.5", "b"));
        keyMap.addRecord("c", Arrays.asList("c", "300.6"));
        keyMap.addRecord("d", Arrays.asList("400.5", "400.6"));
        keyMap.addRecord("e", Arrays.asList("", "500.6"));
        List<double[]> numRecords = keyMap.getRecordNumbers();
        double[] rec = numRecords.get(0);
        assertThat(Double.isNaN(rec[0]), isTrue());
        assertThat(rec[1], closeTo(100.5, 0.001));
        assertThat(rec[2], closeTo(100.6, 0.001));
        rec = numRecords.get(1);
        assertThat(Double.isNaN(rec[0]), isTrue());
        assertThat(rec[1], closeTo(200.5, 0.001));
        assertThat(Double.isNaN(rec[2]), isTrue());
        rec = numRecords.get(2);
        assertThat(Double.isNaN(rec[0]), isTrue());
        assertThat(Double.isNaN(rec[1]), isTrue());
        assertThat(rec[2], closeTo(300.6, 0.001));
        rec = numRecords.get(3);
        assertThat(Double.isNaN(rec[0]), isTrue());
        assertThat(rec[1], closeTo(400.5, 0.001));
        assertThat(rec[2], closeTo(400.6, 0.001));
        rec = numRecords.get(4);
        assertThat(Double.isNaN(rec[0]), isTrue());
        assertThat(Double.isNaN(rec[1]), isTrue());
        assertThat(rec[2], closeTo(500.6, 0.001));
    }

    @Test
    public void testColumnMerge() {
        KeyedFileMap keyMap = new KeyedFileMap("key");
        keyMap.addHeaders(Arrays.asList("a", "b", "c", "d", "e", "f"));
        keyMap.addRecord("100", Arrays.asList("a100", "b100", "c100", "d100", "e100", "f100"));
        keyMap.addRecord("200", Arrays.asList("a200", "b200", "c200", "d200", "e200", "f200"));
        keyMap.addRecord("300", Arrays.asList("a300", "b300", "c300", "d300", "e300", "f300"));
        keyMap.addRecord("400", Arrays.asList("a400", "b400", "c400", "d400", "e400", "f400"));
        keyMap.addRecord("500", Arrays.asList("a500", "b500", "c500", "d500", "e500", "f500"));
        keyMap.addRecord("600", Arrays.asList("a600", "b600", "c600", "d600", "e600", "f600"));
        keyMap.addRecord("700", Arrays.asList("a700", "b700", "c700", "d700", "e700", "f700"));
        Set<String> newHeaders = Set.of("c", "a", "d");
        int count = keyMap.reduceCols(newHeaders);
        assertThat(count, equalTo(3));
        assertThat(keyMap.size(), equalTo(7));
        // These arrays are used to verify the data.
        String[] prefixes = new String[] { "a", "c", "d" };
        Iterator<String> keySequence = Arrays.asList("100", "200", "300", "400", "500", "600", "700")
                .iterator();
         for (List<String> record : keyMap.getRecords()) {
            String key = record.get(0);
            assertThat(key, record.size(), equalTo(4));
            String suffix = keySequence.next();
            assertThat(key, equalTo(suffix));
            for (int c = 0; c < 3; c++) {
                String datum = record.get(c + 1);
                assertThat(datum, equalTo(prefixes[c] + suffix));
            }
        }
    }

    @Test
    public void testKeyedFile() throws IOException {
        File inFile = new File("data", "keyed.txt");
        KeyedFileMap map = new KeyedFileMap(inFile, "genome_id");
        assertThat(map.getDupCount(), equalTo(1));
        assertThat(map.size(), equalTo(5));
        assertThat(map.findColumn("num"), equalTo(1));
        assertThat(map.getHeaders(),
                contains("genome_id", "num", "genome.genome_name", "counter.0", "fraction", "flag"));
        List<String> record = map.getRecord("4");
        assertThat(record, contains("4", "6", "5", "6", "7", "8"));
        record = map.getRecord("999");
        assertThat(record, nullValue());
        record = map.getRecord("100.99");
        assertThat(record, contains("100.99", "1", "name of 100.99", "10", "0.8", ""));
        record = map.getRecord("300.30");
        assertThat(record, contains("300.30", "3", "", "", "", ""));
        record = map.getRecord("200.20");
        assertThat(record, contains("200.20", "2", "name of 200.20", "-4", "12", "Y"));
    }

}
