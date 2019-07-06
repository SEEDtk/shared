/**
 *
 */
package org.theseed.shared;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.SortedSet;

import org.theseed.utils.CountMap;
import org.theseed.utils.MagicMap;

import junit.framework.TestCase;

/**
 * @author Bruce Parrello
 *
 */
public class TestLibrary extends TestCase {

    /**
     * @param name
     */
    public TestLibrary(String name) {
        super(name);
    }

    /**
     * Test magic IDs.
     * @throws IOException
     */
    public void testMagic() throws IOException {
        File inFile = new File("src/test", "words.txt");
        Scanner thingScanner = new Scanner(inFile);
        thingScanner.useDelimiter("\t|\r\n|\n");
        while (thingScanner.hasNext()) {
            String condensed = thingScanner.next();
            String full = thingScanner.next();
            assertEquals("String did not condense.", condensed, MagicMap.condense(full));
        }
        thingScanner.close();
        // Test registration
        ThingMap magicTable = new ThingMap();
        inFile = new File("src/test", "things.txt");
        thingScanner = new Scanner(inFile);
        thingScanner.useDelimiter("\t|\r\n|\n");
        while (thingScanner.hasNext()) {
            String thingId = thingScanner.next();
            thingScanner.next();
            assertNull("Wrong ID found", magicTable.get(thingId));
            String thingDesc = thingScanner.next();
            Thing newThing = new Thing(thingId, thingDesc);
            magicTable.register(newThing);
            assertEquals("Registered ID did not read back.", thingDesc, magicTable.getName(thingId));
        }
        thingScanner.close();
        assertTrue("PheS not found.", magicTable.containsKey("PhenTrnaSyntAlph"));
        assertFalse("Known bad key found.", magicTable.containsKey("PhenTrnaSyntGamm"));
        String modifiedThing = "3-keto-L-gulonate-6-phosphate decarboxylase UlaK putative (L-ascorbate utilization protein D) (EC 4.1.1.85)";
        Thing newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for modified thing.", "3KetoLGulo6PhosDeca6", newThing.getId());
        assertSame("Modified thing did not read back.", newThing, magicTable.get("3KetoLGulo6PhosDeca6"));
        modifiedThing = "Unique (new) thing string without numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for unique thing.", "UniqThinStriWith", newThing.getId());
        assertSame("Unique thing did not read back.", newThing, magicTable.get("UniqThinStriWith"));
        Thing findThing = magicTable.findOrInsert(modifiedThing);
        assertSame("Unique thing was re-inserted.", newThing, findThing);
        modifiedThing = "Unique (old) thing string without numbers";
        findThing = magicTable.findOrInsert(modifiedThing);
        assertTrue("Parenthetical did not change thing ID.", findThing != newThing);
        assertEquals("Wrong ID assigned for parenthetical thing.", "UniqThinStriWith2", findThing.getId());
        assertSame("Parenthetical thing did not read back.", findThing, magicTable.get("UniqThinStriWith2"));
        modifiedThing = "Unique (newer) thing string without numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for newer thing.", "UniqThinStriWith3", newThing.getId());
        assertSame("Parenthetical thing did not read back.", newThing, magicTable.get("UniqThinStriWith3"));
        modifiedThing = "Unique thing string 12345 with numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Name not stored in thing.", modifiedThing, newThing.getName());
        assertEquals("Wrong ID assigned for numbered thing.", "UniqThinStri1234n1", newThing.getId());
        modifiedThing = "Unique thing string 12345 with more numbers";
        newThing = magicTable.findOrInsert(modifiedThing);
        assertEquals("Wrong ID assigned for second numbered thing.", "UniqThinStri1234n2", newThing.getId());
        // Test save and load.
        File saveFile = new File("src/test", "things.ser");
        magicTable.save(saveFile);
        ThingMap newTable = ThingMap.load(saveFile);
        for (Thing oldThing : magicTable.values()) {
            newThing = newTable.get(oldThing.getId());
            assertNotNull("Could not find thing in loaded table.", newThing);
            assertEquals("Loaded table has wrong thing name.", newThing.getName(), oldThing.getName());
            assertEquals("Loaded thing has wrong checksum.", newThing, oldThing);
        }
    }

    public void testCounts() {
        CountMap<String> testMap = new CountMap<String>();
        testMap.setGood("AAA");
        testMap.setGood("AAA");
        testMap.setGood("AAA");
        testMap.setGood("AAA");
        testMap.setBad("AAA");
        assertEquals("Incorrect fraction good for AAA.", 0.8, testMap.fractionGood("AAA"));
        testMap.setBad("BBB");
        assertEquals("Incorrect bad count for BBB.", 1, testMap.bad("BBB"));
        testMap.setGood("CCC");
        assertEquals("Incorrect good count for BBB.", 1, testMap.good("CCC"));
        testMap.setGood("DDD");
        testMap.setBad("DDD");
        SortedSet<String> keys = testMap.keys();
        String key1 = keys.first();
        assertEquals("Incorrect smallest key.", "AAA", key1);
        assertEquals("Incorrect number of keys.", 4, key1.length());
    }


}
