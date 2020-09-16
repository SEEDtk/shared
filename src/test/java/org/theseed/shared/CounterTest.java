/**
 *
 */
package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.theseed.counters.CountMap;
import org.theseed.counters.EnumCounter;
import org.theseed.counters.PairCounter;
import org.theseed.counters.QualityCountMap;
import org.theseed.utils.FloatList;
import org.theseed.utils.IntegerList;
import org.theseed.utils.SizeList;

import junit.framework.TestCase;

/**
 * @author Bruce Parrello
 *
 */
public class CounterTest extends TestCase {

    public void testCounts() {
        QualityCountMap<String> testMap = new QualityCountMap<String>();
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
        assertEquals("Incorrect number of keys.", 4, keys.size());
        List<String> sorted = testMap.bestKeys();
        assertThat("Incorrect sort key results", sorted, contains("AAA", "CCC", "DDD", "BBB"));
        assertThat("Allkeys returned wrong set.", testMap.allKeys(),
                containsInAnyOrder("AAA", "BBB", "CCC", "DDD"));
        testMap.setGood("BBB", 4);
        assertEquals("Incremental set-good failed.", 4, testMap.good("BBB"));
        testMap.setBad("DDD", 6);
        assertEquals("Incremental set-bad failed.", 7, testMap.bad("DDD"));
    }

    /**
     * Test counting maps.
     */
    public void testCounters() {
        Thing t1 = new Thing("T1", "first thing");
        Thing t2 = new Thing("T2", "second thing");
        Thing t3 = new Thing("T3", "third thing");
        Thing t4 = new Thing("T4", "fourth thing");
        Thing t5 = new Thing("T5", "fifth thing");
        CountMap<Thing> thingCounter = new CountMap<Thing>();
        assertEquals("New thingcounter not empty (items).", 0, thingCounter.size());
        assertEquals("Thing 5 count not zero.", 0, thingCounter.getCount(t5));
        assertEquals("Asking about thing 5 added to map.", 0, thingCounter.size());
        thingCounter.count(t1);
        thingCounter.count(t4);
        thingCounter.count(t2);
        thingCounter.count(t3);
        thingCounter.count(t2);
        thingCounter.count(t3);
        thingCounter.count(t4);
        thingCounter.count(t3);
        thingCounter.count(t4, 2);
        assertEquals("Wrong count for thing 1.", 1, thingCounter.getCount(t1));
        assertEquals("Wrong count for thing 2.", 2, thingCounter.getCount(t2));
        assertEquals("Wrong count for thing 3.", 3, thingCounter.getCount(t3));
        assertEquals("Wrong count for thing 4.", 4, thingCounter.getCount(t4));
        assertEquals("Wrong count for thing 5.", 0, thingCounter.getCount(t5));
        assertThat(thingCounter.getTotal(), equalTo(10));
        Collection<Thing> keysFound = thingCounter.keys();
        assertThat("Wrong keys returned.", keysFound, contains(t1, t4, t2, t3));
        assertEquals("Too many keys returned.", 4, keysFound.size());
        assertEquals("Wrong number of entries in counter.", 4, thingCounter.size());
        List<CountMap<Thing>.Count> countsFound = thingCounter.sortedCounts();
        List<Thing> keysCounted = new ArrayList<Thing>(4);
        int prev = Integer.MAX_VALUE;
        for (CountMap<Thing>.Count result : countsFound) {
            assertThat("Counts out of order for " + result.getKey() + ".", result.getCount(), lessThan(prev));
            prev = result.getCount();
            keysCounted.add(result.getKey());
        }
        Collection<CountMap<Thing>.Count> allCounts = thingCounter.counts();
        assertThat("Unsorted counts came back wrong.", allCounts, containsInAnyOrder(countsFound.toArray()));
        assertThat("Wrong keys returned in result list.", keysCounted, contains(t4, t3, t2, t1));
        thingCounter.setCount(t4, 10);
        assertThat("Wrong count after set.", thingCounter.getCount(t4), equalTo(10));
        thingCounter.clear();
        assertThat("Wrong count for thing 2 after clear.", thingCounter.getCount(t2), equalTo(0));
        thingCounter.count(t1);
        assertThat("Wrong count for thing 1 after recount.", thingCounter.getCount(t1), equalTo(1));
        thingCounter.count(t4);
        Set<Thing> singletons = thingCounter.getSingletons();
        assertThat("Wrong set of singletons", singletons, containsInAnyOrder(t1, t4));
        thingCounter.deleteAll();
        assertThat("Keys left after deleteAll", thingCounter.keys().size(), equalTo(0));
        PairCounter<Thing> pairCounter = new PairCounter<Thing>();
        assertEquals("Pair counter not empty after creation (pairs).", 0, pairCounter.size());
        assertEquals("Pair counter not empty after creation (items).", 0, pairCounter.itemSize());
        assertEquals("Pair counter nonzero at creation.", 0, pairCounter.getCount(t1, t2));
        pairCounter.recordOccurrence(t1);
        assertEquals("Pair count for t1 wrong.", 1, pairCounter.getCount(t1));
        assertEquals("Pair counter not empty after item record.", 0, pairCounter.size());
        assertEquals("Pair item counter not 1 after first count.", 1, pairCounter.itemSize());
        List<Thing> myList = Arrays.asList(t2, t3);
        pairCounter.recordOccurrence(t4, myList);
        pairCounter.recordPairing(t2, t4);
        // We have recorded t1 with no pairs and t4 with t2 and t3. We also have a pairing of t2 and t4.
        // The counts we expect are t1 = 1, t4 = 1, t2/t4 = 2, t3/t4 = 1, t4/t3 = 1, t4/t2 = 1.
        assertEquals("Pair counter pairs wrong.", 2, pairCounter.size());
        assertEquals("t1 counter wrong.", 1, pairCounter.getCount(t1));
        assertEquals("t2 counter wrong.", 0, pairCounter.getCount(t2));
        assertEquals("t4 counter wrong.", 1, pairCounter.getCount(t4));
        assertEquals("t2/t4 counter wrong.", 2, pairCounter.getCount(t2, t4));
        assertEquals("t3/t4 counter wrong.", 1, pairCounter.getCount(t3, t4));
        assertEquals("t4/t3 counter wrong.", 1, pairCounter.getCount(t4, t3));
        assertEquals("t4/t2 counter wrong.", 2, pairCounter.getCount(t4, t2));
        myList = Arrays.asList(t4, t1, t3, t3);
        pairCounter.recordOccurrence(t2, myList);
        pairCounter.recordOccurrence(t4, myList);
        assertEquals("Pair counter pairs wrong after second pass.", 6, pairCounter.size());
        assertEquals("Pair counter items wrong after second pass.", 3, pairCounter.itemSize());
        assertEquals("t1 counter wrong after second pass.", 1, pairCounter.getCount(t1));
        assertEquals("t2 counter wrong after second pass.", 1, pairCounter.getCount(t2));
        assertEquals("t4 counter wrong after second pass.", 2, pairCounter.getCount(t4));
        assertEquals("t2/t1 counter wrong after second pass.", 1, pairCounter.getCount(t2, t1));
        assertEquals("t1/t2 counter wrong after second pass.", 1, pairCounter.getCount(t1, t2));
        assertEquals("t2/t3 counter wrong after second pass.", 2, pairCounter.getCount(t2, t3));
        assertEquals("t3/t2 counter wrong after second pass.", 2, pairCounter.getCount(t3, t2));
        assertEquals("t1/t4 counter wrong after second pass.", 1, pairCounter.getCount(t1, t4));
        assertEquals("t2/t4 counter wrong after second pass.", 3, pairCounter.getCount(t2, t4));
        assertEquals("t3/t4 counter wrong after second pass.", 3, pairCounter.getCount(t3, t4));
        assertEquals("t4/t4 counter wrong after second pass.", 1, pairCounter.getCount(t4, t4));
        assertEquals("t4/t3 counter wrong after second pass.", 3, pairCounter.getCount(t4, t3));
        assertEquals("t4/t2 counter wrong after second pass.", 3, pairCounter.getCount(t4, t2));
        assertEquals("t4/t1 counter wrong after second pass.", 1, pairCounter.getCount(t4, t1));
        PairCounter<Thing>.Count testCounter = pairCounter.getPairCount(t4, t2);
        assertThat("Wrong counter returned (k1).", testCounter.getKey1(), anyOf(equalTo(t2), equalTo(t4)));
        assertThat("Wrong counter returned (k2).", testCounter.getKey2(), anyOf(equalTo(t2), equalTo(t4)));
        assertEquals("Wrong counter value.", 3, testCounter.getCount());
        assertEquals("Wrong togetherness.", 1.0, testCounter.togetherness(), 0.001);
        // Get the sorted list of counts.
        Collection<PairCounter<Thing>.Count> sortedPairs = pairCounter.sortedCounts(0.0, 0);
        prev = Integer.MAX_VALUE;
        for (PairCounter<Thing>.Count counter : sortedPairs) {
            assertThat("Counts out of order.", prev, greaterThanOrEqualTo(counter.getCount()));
            prev = counter.getCount();
        }
        // Get the sorted list of items counted.
        Collection<CountMap<Thing>.Count> sortedItems = pairCounter.sortedItemCounts();
        assertThat("Wrong list of items counted.",
                sortedItems.stream().map(CountMap<Thing>.Count::getKey).collect(Collectors.toList()),
                containsInAnyOrder(t1, t2, t4));
        prev = Integer.MAX_VALUE;
        for (CountMap<Thing>.Count counter : sortedItems) {
            assertThat("Counts out of order.", prev, greaterThanOrEqualTo(counter.getCount()));
            prev = counter.getCount();
        }
        // Get a better togetherness test.
        pairCounter.recordOccurrences(t2, 3);
        pairCounter.recordOccurrence(t4);
        testCounter = pairCounter.getPairCount(t4, t2);
        assertThat("Wrong counter returned (k1).", testCounter.getKey1(), anyOf(equalTo(t2), equalTo(t4)));
        assertThat("Wrong counter returned (k2).", testCounter.getKey2(), anyOf(equalTo(t2), equalTo(t4)));
        assertEquals("Wrong togetherness value.", 0.75, testCounter.togetherness(), 0.001);
        // Add some pairings.
        pairCounter.recordPairings(t1, t5, 4);
        assertEquals("Wrong counter returned after multi-count", 4, pairCounter.getCount(t5, t1));
        // Bump the item counts.
        pairCounter.recordOccurrences(t1, 6);
        pairCounter.recordOccurrences(t3, 10);
        pairCounter.recordOccurrences(t5, 8);
        // Try pair retrieval again with threshholds.
        sortedPairs = pairCounter.sortedCounts(0.30, 2);
        assertEquals("Threshold returned wrong number of pairs.", 3, sortedPairs.size());
        prev = Integer.MAX_VALUE;
        for (PairCounter<Thing>.Count counter : sortedPairs) {
            assertThat("Counts out of order in threshold list.", prev, greaterThanOrEqualTo(counter.getCount()));
            prev = counter.getCount();
            assertThat("Togetherness threshold failed.", counter.togetherness(), greaterThanOrEqualTo(0.30));
            assertThat("Mincount threshold failed.", counter.getCount(), greaterThanOrEqualTo(2));
        }
    }

    /**
     * test integer list
     */
    public void testIntList() {
        // Start with an empty list.
        IntegerList newList = new IntegerList();
        assertThat(newList.size(), equalTo(0));
        assertTrue(newList.isEmpty());
        assertFalse(newList.hasNext());
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        assertThat(newList.last(), equalTo(0));
        // Repeat with an empty string.
        newList = new IntegerList("");
        assertThat(newList.size(), equalTo(0));
        assertTrue(newList.isEmpty());
        assertFalse(newList.hasNext());
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        // Use the default feature.
        newList.setDefault(12);
        assertThat(newList.size(), equalTo(1));
        assertThat(newList.get(0), equalTo(12));
        assertThat(newList.toString(), equalTo("12"));
        assertThat(newList.original(), equalTo("12"));
        assertThat(newList.last(), equalTo(12));
        // Verify out-of-bounds works.
        assertThat(newList.get(-1), equalTo(0));
        assertThat(newList.get(1), equalTo(0));
        // Use a more sophisticated list.
        newList = new IntegerList("4,6,8,10");
        assertThat(newList.size(), equalTo(4));
        assertThat(newList.get(0), equalTo(4));
        assertThat(newList.get(1), equalTo(6));
        assertThat(newList.get(2), equalTo(8));
        assertThat(newList.get(3), equalTo(10));
        assertThat(newList.last(), equalTo(10));
        // Try traversal.
        assertThat(newList.next(), equalTo(4));
        assertThat(newList.next(), equalTo(6));
        assertThat(newList.next(), equalTo(8));
        assertThat(newList.next(), equalTo(10));
        assertThat(newList.next(), equalTo(0));
        // Reset to the first and traverse again, using hasnext checks.
        newList.reset();
        assertTrue(newList.hasNext());
        assertThat(newList.next(), equalTo(4));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), equalTo(6));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), equalTo(8));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), equalTo(10));
        assertFalse(newList.hasNext());
        assertThat(newList.next(), equalTo(0));
        assertThat(newList.toString(), equalTo("4, 6, 8, 10"));
        assertThat(newList.original(), equalTo("4,6,8,10"));
        // Iterate through the list.
        Iterator<Integer> iter = newList.new Iter();
        assertTrue(iter.hasNext());
        assertThat(iter.next(), equalTo(4));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), equalTo(6));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), equalTo(8));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), equalTo(10));
        assertFalse(iter.hasNext());
        // Test softnext.
        newList.reset();
        assertThat(newList.softNext(), equalTo(4));
        assertThat(newList.softNext(), equalTo(6));
        assertThat(newList.softNext(), equalTo(8));
        assertThat(newList.softNext(), equalTo(10));
        assertThat(newList.softNext(), equalTo(10));
        assertThat(newList.softNext(), equalTo(10));
        // Use an array initializer.
        int[] test = new int[] { 3, 5, 7 };
        newList = new IntegerList(test);
        assertThat(newList.size(), equalTo(3));
        assertThat(newList.get(0), equalTo(3));
        assertThat(newList.get(1), equalTo(5));
        assertThat(newList.get(2), equalTo(7));
        assertThat(newList.last(), equalTo(7));
    }

    /**
     * test float list
     */
    public void testFloatList() {
        // Start with an empty list.
        FloatList newList = new FloatList();
        assertThat(newList.size(), equalTo(0));
        assertTrue(newList.isEmpty());
        assertFalse(newList.hasNext());
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        assertThat(newList.last(), equalTo(0.0));
        // Repeat with an empty string.
        newList = new FloatList("");
        assertThat(newList.size(), equalTo(0));
        assertTrue(newList.isEmpty());
        assertFalse(newList.hasNext());
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        // Use the default feature.
        newList.setDefault(1.5);
        assertThat(newList.size(), equalTo(1));
        assertThat(newList.get(0), equalTo(1.5));
        assertThat(newList.toString(), equalTo("1.5"));
        assertThat(newList.original(), equalTo("1.5"));
        assertThat(newList.last(), equalTo(1.5));
        // Verify out-of-bounds works.
        assertThat(newList.get(-1), equalTo(0.0));
        assertThat(newList.get(1), equalTo(0.0));
        // Use a more sophisticated list.
        newList = new FloatList("0.4,0.6,0.8,0.1");
        assertThat(newList.size(), equalTo(4));
        assertThat(newList.get(0), closeTo(0.4, 1e-6));
        assertThat(newList.get(1), closeTo(0.6, 1e-6));
        assertThat(newList.get(2), closeTo(0.8, 1e-6));
        assertThat(newList.get(3), closeTo(0.1, 1e-6));
        assertThat(newList.last(), closeTo(0.1, 1e-6));
        // Try traversal.
        assertThat(newList.next(), closeTo(0.4, 1e-6));
        assertThat(newList.next(), closeTo(0.6, 1e-6));
        assertThat(newList.next(), closeTo(0.8, 1e-6));
        assertThat(newList.next(), closeTo(0.1, 1e-6));
        assertThat(newList.next(), equalTo(0.0));
        // Reset to the first and traverse again, using hasnext checks.
        newList.reset();
        assertTrue(newList.hasNext());
        assertThat(newList.next(), closeTo(0.4, 1e-6));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), closeTo(0.6, 1e-6));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), closeTo(0.8, 1e-6));
        assertTrue(newList.hasNext());
        assertThat(newList.next(), closeTo(0.1, 1e-6));
        assertFalse(newList.hasNext());
        assertThat(newList.next(), equalTo(0.0));
        assertThat(newList.toString(), equalTo("0.4, 0.6, 0.8, 0.1"));
        assertThat(newList.original(), equalTo("0.4,0.6,0.8,0.1"));
        // Iterate through the list.
        Iterator<Double> iter = newList.new Iter();
        assertTrue(iter.hasNext());
        assertThat(iter.next(), closeTo(0.4, 1e-6));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), closeTo(0.6, 1e-6));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), closeTo(0.8, 1e-6));
        assertTrue(iter.hasNext());
        assertThat(iter.next(), closeTo(0.1, 1e-6));
        assertFalse(iter.hasNext());
        // Test softnext.
        newList.reset();
        assertThat(newList.softNext(), closeTo(0.4, 1e-6));
        assertThat(newList.softNext(), closeTo(0.6, 1e-6));
        assertThat(newList.softNext(), closeTo(0.8, 1e-6));
        assertThat(newList.softNext(), closeTo(0.1, 1e-6));
        assertThat(newList.softNext(), closeTo(0.1, 1e-6));
        assertThat(newList.softNext(), closeTo(0.1, 1e-6));
        // Test getValues.
        double[] test = newList.getValues(6);
        assertThat(test.length, equalTo(6));
        assertThat(test[0], closeTo(0.4, 1e-6));
        assertThat(test[1], closeTo(0.6, 1e-6));
        assertThat(test[2], closeTo(0.8, 1e-6));
        assertThat(test[3], closeTo(0.1, 1e-6));
        assertThat(test[4], closeTo(0.1, 1e-6));
        assertThat(test[5], closeTo(0.1, 1e-6));
        test = newList.getValues(2);
        assertThat(test.length, equalTo(2));
        assertThat(test[0], closeTo(0.4, 1e-6));
        assertThat(test[1], closeTo(0.6, 1e-6));
        // Use an array initializer.
        test = new double[] { 3, 5.5, 7 };
        newList = new FloatList(test);
        assertThat(newList.size(), equalTo(3));
        assertThat(newList.get(0), equalTo(3.0));
        assertThat(newList.get(1), equalTo(5.5));
        assertThat(newList.get(2), equalTo(7.0));
        assertThat(newList.last(), equalTo(7.0));
        double[] test2 = newList.getValues();
        assertThat(test2.length, equalTo(test.length));
        for (int i = 0; i < test.length; i++) {
            assertThat(test[i], equalTo(test2[i]));
        }
        // Use a fill initializer.
        newList = new FloatList(1.5, 4);
        assertThat(newList.size(), equalTo(4));
        for (int i = 0; i < 4; i++)
            assertThat(newList.get(i), equalTo(1.5));
    }


    /**
     * test size lists
     */
    public void testSizes() {
        int[] sizes = SizeList.getSizes(10, 99, 10);
        assertThat(ArrayUtils.toObject(sizes), arrayContaining(10, 20, 30, 40, 50, 60, 70, 80, 90, 99));
        sizes = SizeList.getSizes(10, 100, 10);
        assertThat(ArrayUtils.toObject(sizes), arrayContaining(10, 20, 30, 40, 50, 60, 70, 80, 90, 100));
    }

    /**
     * Test enum counters
     */
    private enum Cats { A, B, C; }

    public void testEnumCounts() {
        EnumCounter<Cats> counters = new EnumCounter<Cats>(Cats.class);
        for (Cats cat : Cats.values())
            assertThat(cat.toString(), counters.getCount(cat), equalTo(0));
        counters.count(Cats.A);
        assertThat(counters.getCount(Cats.A), equalTo(1));
        assertThat(counters.getCount(Cats.B), equalTo(0));
        assertThat(counters.getCount(Cats.C), equalTo(0));
        counters.count(Cats.A);
        assertThat(counters.getCount(Cats.A), equalTo(2));
        assertThat(counters.getCount(Cats.B), equalTo(0));
        assertThat(counters.getCount(Cats.C), equalTo(0));
        counters.count(Cats.B);
        assertThat(counters.getCount(Cats.A), equalTo(2));
        assertThat(counters.getCount(Cats.B), equalTo(1));
        assertThat(counters.getCount(Cats.C), equalTo(0));
        counters.clear();
        for (Cats cat : Cats.values())
            assertThat(cat.toString(), counters.getCount(cat), equalTo(0));
        counters.count(Cats.C);
        assertThat(counters.getCount(Cats.A), equalTo(0));
        assertThat(counters.getCount(Cats.B), equalTo(0));
        assertThat(counters.getCount(Cats.C), equalTo(1));
        EnumCounter<Cats> count2 = new EnumCounter<Cats>(Cats.class);
        count2.count(Cats.A);
        counters.count(Cats.B);
        counters.count(Cats.B);
        count2.count(Cats.C);
        count2.count(Cats.C);
        count2.count(Cats.C);
        count2.count(Cats.C);
        counters.sum(count2);
        assertThat(counters.getCount(Cats.A), equalTo(1));
        assertThat(counters.getCount(Cats.B), equalTo(2));
        assertThat(counters.getCount(Cats.C), equalTo(5));
    }
}
