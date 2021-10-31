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

import org.junit.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestKeyedFileMap {

    @Test
    public void test() throws IOException {
        KeyedFileMap outMap = new KeyedFileMap("letter");
        outMap.addHeaders(Arrays.asList("plant", "animal"));
        outMap.addRecord("a", Arrays.asList("Apple", "Aardvark"));
        outMap.addRecord("p", Arrays.asList("Petunia", "Panther"));
        outMap.addRecord("d", Arrays.asList("Dog", "Daffodil"));
        outMap.addRecord("c", Arrays.asList("Cat", "Carrot"));
        outMap.addHeaders(Arrays.asList("machine"));
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
            assertThat(line, equalTo("letter\tplant\tanimal\tmachine"));
            line = testStream.next();
            assertThat(line, equalTo("a\tApple\tAardvark\tAbacus"));
            line = testStream.next();
            assertThat(line, equalTo("d\tDog\tDaffodil\t"));
            line = testStream.next();
            assertThat(line, equalTo("c\tCat\tCarrot\tClock"));
            assertThat(testStream.hasNext(), isFalse());
        }
    }

}
