/**
 *
 */
package org.theseed.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestFieldStream {

    /**
     * Test a tabbed input file using the field stream reader.
     *
     * @throws IOException
     */
    @Test
    public void testFieldFile() throws IOException {
        File inFile = new File("data", "tabbed.tbl");
        FieldInputStream tabReader = FieldInputStream.create(inFile);
        applyTabReaderTests(tabReader, "100.99\tname of 100.99\t10\t0.8");
        // Reopen to test iteration.
        tabReader = FieldInputStream.create(inFile);
        String[] testLabels = new String[] { "100.99", "200.20", "1000.6", "4",  "14"};
        int i = 0;
        for (var l : tabReader) {
            assertThat("Wrong record at index " + i, l.get(0), equalTo(testLabels[i]));
            i++;
        }
        assertThat("Wrong number of records", i, equalTo(5));
        tabReader.close();
    }

    private void applyTabReaderTests(FieldInputStream tabReader, String testLine) throws IOException {
        // Test the column finder.
        assertThat("Did not find genome name.", tabReader.findField("genome_name"), equalTo(1));
        assertThat("Did not find fraction.", tabReader.findField("fraction"), equalTo(3));
        int colIdx = 0;
        try {
            colIdx = tabReader.findField("genome");
            fail("Found genome column at " + colIdx);
        } catch (IOException e) {
            // this is good
        }
        var line = tabReader.next();
        assertThat("Wrong value in column 0 of line 1", line.get(0), equalTo("100.99"));
        assertThat("Wrong value in column 2 of line 1", line.getInt(2), equalTo(10));
        assertThat("Wrong value in column 3 of line 1", line.getDouble(3), closeTo(0.8, 0.0001));
        assertThat("Boolean adjustment fail in line 1", line.getFlag(4), equalTo(false));
        line = tabReader.next();
        assertThat("Wrong value in column 2 of line 2", line.getInt(2), equalTo(-4));
        assertThat("Wrong value in column 3 of line 2", line.getDouble(3), equalTo(12.0));
        assertThat("Wrong value in column 4 of line 2", line.getFlag(4), equalTo(true));
        line = tabReader.next();
        assertThat("Wrong value in column 4 of line 3", line.getFlag(4), equalTo(false));
        line = tabReader.next();
        assertThat("Empty column failure.", line.getList(1).size(), equalTo(0));
        assertThat("Blank column failure.", line.getInt(2), equalTo(5));
        assertThat(tabReader.hasNext(), equalTo(true));
        line = tabReader.next();
        assertThat("singleton list failure", line.getList(0), contains("14"));
        assertThat("parsed list failure", line.getList(1), contains("a", "b", "c"));
        assertThat(tabReader.hasNext(), equalTo(false));
        tabReader.close();
    }

    @Test
    void TestJsonTokenizer() throws IOException {
        File jsonFile = new File("data", "crazy.json");
        try (LineReader inStream = new LineReader(jsonFile)) {
            Iterator<String> lineIter = inStream.iterator();
            JsonTokenizer tokens = new JsonTokenizer(lineIter.next(), 1);
            Iterator<String> tokenIter = tokens.iterator();
            assertThat(tokenIter.next(), equalTo("["));
            assertThat(tokenIter.hasNext(), equalTo(false));
            tokens = new JsonTokenizer(lineIter.next(), 2);
            tokenIter = tokens.iterator();
            assertThat(tokenIter.next(), equalTo("{"));
            assertThat(tokenIter.hasNext(), equalTo(false));
            tokens = new JsonTokenizer(lineIter.next(), 3);
            tokenIter = tokens.iterator();
            assertThat(tokenIter.next(), equalTo("accession"));
            assertThat(tokenIter.next(), equalTo(":"));
            assertThat(tokenIter.next(), equalTo("NC_000962"));
            assertThat(tokenIter.next(), equalTo(","));
            assertThat(tokenIter.next(), equalTo("annotation"));
            assertThat(tokenIter.next(), equalTo(":"));
            assertThat(tokenIter.next(), equalTo("This is \b a stress test for \\ escaped \"strings\": \u03B2"));
            assertThat(tokenIter.next(), equalTo(","));
            assertThat(tokenIter.hasNext(), equalTo(false));
            tokens = new JsonTokenizer(lineIter.next(), 4);
            tokenIter = tokens.iterator();
            assertThat(tokenIter.next(), equalTo("end"));
            assertThat(tokenIter.next(), equalTo(":"));
            assertThat(tokenIter.next(), equalTo("1612620"));
            assertThat(tokenIter.next(), equalTo(","));
            assertThat(tokenIter.next(), equalTo("public"));
            assertThat(tokenIter.next(), equalTo(":"));
            assertThat(tokenIter.next(), equalTo("true"));
            assertThat(tokenIter.next(), equalTo(","));
            assertThat(tokenIter.next(), equalTo("segments"));
            assertThat(tokenIter.next(), equalTo(":"));
            assertThat(tokenIter.next(), equalTo("["));
            assertThat(tokenIter.next(), equalTo("1612600..1612620"));
            assertThat(tokenIter.next(), equalTo(","));
            assertThat(tokenIter.next(), equalTo("12345"));
            assertThat(tokenIter.next(), equalTo("]"));
            assertThat(tokenIter.next(), equalTo(","));
            assertThat(tokenIter.hasNext(), equalTo(false));
        }
    }

    @Test
    void TestJsonFile() throws IOException {
        File inFile = new File("data", "genome_feature.json");
        try (FieldInputStream inStream = FieldInputStream.create(inFile)) {
            int accessionIdx = inStream.findField("accession");
            int publicIdx = inStream.findField("public");
            int featureTypeIdx = inStream.findField("feature_type");
            int taxonIdx = inStream.findField("taxon_id");
            int naLengthIdx = inStream.findField("na_length");
            int segmentIdx = inStream.findField("segments");
            Iterator<FieldInputStream.Record> iter = inStream.iterator();
            assertThat(iter.hasNext(), equalTo(true));
            var record = iter.next();
            assertThat(record.get(accessionIdx), equalTo("NC_000962"));
            assertThat(record.getFlag(publicIdx), equalTo(true));
            assertThat(record.get(featureTypeIdx), equalTo("repeat_region"));
            assertThat(record.getInt(taxonIdx), equalTo(83332));
            assertThat(record.getInt(naLengthIdx), equalTo(21));
            assertThat(record.getList(segmentIdx), contains("1612600..1612620", "300..400"));
            assertThat(iter.hasNext(), equalTo(true));
            record = iter.next();
            assertThat(record.get(accessionIdx), equalTo("NC_000963"));
            assertThat(record.getFlag(publicIdx), equalTo(false));
            assertThat(record.get(featureTypeIdx), equalTo("CDS"));
            assertThat(record.getInt(taxonIdx), equalTo(0));
            assertThat(record.getInt(naLengthIdx), equalTo(1554));
            assertThat(record.getList(segmentIdx), contains("4138202..4139755"));
            assertThat(iter.hasNext(), equalTo(true));
            record = iter.next();
            assertThat(record.get(accessionIdx), equalTo("NC_000962"));
            assertThat(record.getFlag(publicIdx), equalTo(true));
            assertThat(record.get(featureTypeIdx), equalTo("misc_feature"));
            assertThat(record.getInt(taxonIdx), equalTo(83332));
            assertThat(record.getInt(naLengthIdx), equalTo(24));
            assertThat(record.getList(segmentIdx).isEmpty(), equalTo(true));
            assertThat(iter.hasNext(), equalTo(false));
        }
    }


}
