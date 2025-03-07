/**
 *
 */
package org.theseed.counters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.theseed.magic.MagicMap;
import org.theseed.magic.MagicObject;
import org.theseed.stats.BestColumn;
import org.theseed.stats.EnumCounter;
import org.theseed.stats.PairCounter;
import org.theseed.stats.QualityCountMap;
import org.theseed.stats.WeightMap;
import org.theseed.utils.FloatList;
import org.theseed.utils.IntegerList;
import org.theseed.utils.SizeList;

import org.junit.jupiter.api.Test;


/**
 * @author Bruce Parrello
 *
 */
public class CounterTest  {

    @Test
    public void testCounts() {
        QualityCountMap<String> testMap = new QualityCountMap<String>();
        testMap.setGood("AAA");
        testMap.setGood("AAA");
        testMap.setGood("AAA");
        testMap.setGood("AAA");
        testMap.setBad("AAA");
        assertThat("Incorrect fraction good for AAA.", testMap.fractionGood("AAA"), equalTo(0.8));
        testMap.setBad("BBB");
        assertThat("Incorrect bad count for BBB.", testMap.bad("BBB"), equalTo(1));
        testMap.setGood("CCC");
        assertThat("Incorrect good count for BBB.", testMap.good("CCC"), equalTo(1));
        testMap.setGood("DDD");
        testMap.setBad("DDD");
        SortedSet<String> keys = testMap.keys();
        String key1 = keys.first();
        assertThat("Incorrect smallest key.", key1, equalTo("AAA"));
        assertThat("Incorrect number of keys.", keys.size(), equalTo(4));
        List<String> sorted = testMap.bestKeys();
        assertThat("Incorrect sort key results", sorted, contains("AAA", "CCC", "DDD", "BBB"));
        assertThat("Allkeys returned wrong set.", testMap.allKeys(),
                containsInAnyOrder("AAA", "BBB", "CCC", "DDD"));
        testMap.setGood("BBB", 4);
        assertThat("Incremental set-good failed.", testMap.good("BBB"), equalTo(4));
        testMap.setBad("DDD", 6);
        assertThat("Incremental set-bad failed.", testMap.bad("DDD"), equalTo(7));
        testMap.clear();
        assertThat(testMap.good("AAA"), equalTo(0));
        assertThat(testMap.bad("DDD"), equalTo(0));
        assertThat(testMap.size(), equalTo(0));
    }

    /**
     * Test counting maps.
     */
    @Test
    public void testCounters() {
        Thing t1 = new Thing("T1", "first thing");
        Thing t2 = new Thing("T2", "second thing");
        Thing t3 = new Thing("T3", "third thing");
        Thing t4 = new Thing("T4", "fourth thing");
        Thing t5 = new Thing("T5", "fifth thing");
        CountMap<Thing> thingCounter = new CountMap<Thing>();
        assertThat("New thingcounter not empty (items).", thingCounter.size(), equalTo(0));
        assertThat("Thing 5 count not zero.", thingCounter.getCount(t5), equalTo(0));
        assertThat("Asking about thing 5 added to map.", thingCounter.size(), equalTo(0));
        thingCounter.count(t1);
        thingCounter.count(t4);
        thingCounter.count(t2);
        thingCounter.count(t3);
        thingCounter.count(t2);
        thingCounter.count(t3);
        thingCounter.count(t4);
        thingCounter.count(t3);
        thingCounter.count(t4, 2);
        assertThat("Wrong count for thing 1.", thingCounter.getCount(t1), equalTo(1));
        assertThat("Wrong count for thing 2.", thingCounter.getCount(t2), equalTo(2));
        assertThat("Wrong count for thing 3.", thingCounter.getCount(t3), equalTo(3));
        assertThat("Wrong count for thing 4.", thingCounter.getCount(t4), equalTo(4));
        assertThat("Wrong count for thing 5.", thingCounter.getCount(t5), equalTo(0));
        assertThat(thingCounter.getTotal(), equalTo(10));
        var best = thingCounter.getBestEntry();
        assertThat(best.getCount(), equalTo(4));
        assertThat(best.getKey(), equalTo(t4));
        Collection<Thing> keysFound = thingCounter.keys();
        assertThat("Wrong keys returned.", keysFound, contains(t1, t4, t2, t3));
        assertThat("Too many keys returned.", keysFound.size(), equalTo(4));
        assertThat("Wrong number of entries in counter.", thingCounter.size(), equalTo(4));
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
        assertThat("Pair counter not empty after creation (pairs).", pairCounter.size(), equalTo(0));
        assertThat("Pair counter not empty after creation (items).", pairCounter.itemSize(), equalTo(0));
        assertThat("Pair counter nonzero at creation.", pairCounter.getCount(t1, t2), equalTo(0));
        pairCounter.recordOccurrence(t1);
        assertThat("Pair count for t1 wrong.", pairCounter.getCount(t1), equalTo(1));
        assertThat("Pair counter not empty after item record.", pairCounter.size(), equalTo(0));
        assertThat("Pair item counter not 1 after first count.", pairCounter.itemSize(), equalTo(1));
        List<Thing> myList = Arrays.asList(t2, t3);
        pairCounter.recordOccurrence(t4, myList);
        pairCounter.recordPairing(t2, t4);
        // We have recorded t1 with no pairs and t4 with t2 and t3. We also have a pairing of t2 and t4.
        // The counts we expect are t1 = 1, t4 = 1, t2/t4 = 2, t3/t4 = 1, t4/t3 = 1, t4/t2 = 1.
        assertThat("Pair counter pairs wrong.", pairCounter.size(), equalTo(2));
        assertThat("t1 counter wrong.", pairCounter.getCount(t1), equalTo(1));
        assertThat("t2 counter wrong.", pairCounter.getCount(t2), equalTo(0));
        assertThat("t4 counter wrong.", pairCounter.getCount(t4), equalTo(1));
        assertThat("t2/t4 counter wrong.", pairCounter.getCount(t2, t4), equalTo(2));
        assertThat("t3/t4 counter wrong.", pairCounter.getCount(t3, t4), equalTo(1));
        assertThat("t4/t3 counter wrong.", pairCounter.getCount(t4, t3), equalTo(1));
        assertThat("t4/t2 counter wrong.", pairCounter.getCount(t4, t2), equalTo(2));
        myList = Arrays.asList(t4, t1, t3, t3);
        pairCounter.recordOccurrence(t2, myList);
        pairCounter.recordOccurrence(t4, myList);
        assertThat("Pair counter pairs wrong after second pass.", pairCounter.size(), equalTo(6));
        assertThat("Pair counter items wrong after second pass.", pairCounter.itemSize(), equalTo(3));
        assertThat("t1 counter wrong after second pass.", pairCounter.getCount(t1), equalTo(1));
        assertThat("t2 counter wrong after second pass.", pairCounter.getCount(t2), equalTo(1));
        assertThat("t4 counter wrong after second pass.", pairCounter.getCount(t4), equalTo(2));
        assertThat("t2/t1 counter wrong after second pass.", pairCounter.getCount(t2, t1), equalTo(1));
        assertThat("t1/t2 counter wrong after second pass.", pairCounter.getCount(t1, t2), equalTo(1));
        assertThat("t2/t3 counter wrong after second pass.", pairCounter.getCount(t2, t3), equalTo(2));
        assertThat("t3/t2 counter wrong after second pass.", pairCounter.getCount(t3, t2), equalTo(2));
        assertThat("t1/t4 counter wrong after second pass.", pairCounter.getCount(t1, t4), equalTo(1));
        assertThat("t2/t4 counter wrong after second pass.", pairCounter.getCount(t2, t4), equalTo(3));
        assertThat("t3/t4 counter wrong after second pass.", pairCounter.getCount(t3, t4), equalTo(3));
        assertThat("t4/t4 counter wrong after second pass.", pairCounter.getCount(t4, t4), equalTo(1));
        assertThat("t4/t3 counter wrong after second pass.", pairCounter.getCount(t4, t3), equalTo(3));
        assertThat("t4/t2 counter wrong after second pass.", pairCounter.getCount(t4, t2), equalTo(3));
        assertThat("t4/t1 counter wrong after second pass.", pairCounter.getCount(t4, t1), equalTo(1));
        PairCounter<Thing>.Count testCounter = pairCounter.getPairCount(t4, t2);
        assertThat("Wrong counter returned (k1).", testCounter.getKey1(), anyOf(equalTo(t2), equalTo(t4)));
        assertThat("Wrong counter returned (k2).", testCounter.getKey2(), anyOf(equalTo(t2), equalTo(t4)));
        assertThat("Wrong counter value.", testCounter.getCount(), equalTo(3));
        assertThat("Wrong togetherness.", testCounter.togetherness(), closeTo(1.0, 0.001));
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
        assertThat("Wrong togetherness value.", testCounter.togetherness(), closeTo(0.75, 0.0001));
        // Add some pairings.
        pairCounter.recordPairings(t1, t5, 4);
        assertThat("Wrong counter returned after multi-count", pairCounter.getCount(t5, t1), equalTo(4));
        // Bump the item counts.
        pairCounter.recordOccurrences(t1, 6);
        pairCounter.recordOccurrences(t3, 10);
        pairCounter.recordOccurrences(t5, 8);
        // Try pair retrieval again with threshholds.
        sortedPairs = pairCounter.sortedCounts(0.30, 2);
        assertThat("Threshold returned wrong number of pairs.", sortedPairs.size(), equalTo(3));
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
    @Test
    public void testIntList() {
        // Start with an empty list.
        IntegerList newList = new IntegerList();
        assertThat(newList.size(), equalTo(0));
        assertThat(newList.isEmpty(), equalTo(true));
        assertThat(newList.hasNext(), equalTo(false));
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        assertThat(newList.last(), equalTo(0));
        // Repeat with an empty string.
        newList = new IntegerList("");
        assertThat(newList.size(), equalTo(0));
        assertThat(newList.isEmpty(), equalTo(true));
        assertThat(newList.hasNext(), equalTo(false));
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
        assertThat(newList.hasNext(), equalTo(true));
        assertThat(newList.next(), equalTo(4));
        assertThat(newList.hasNext(), equalTo(true));
        assertThat(newList.next(), equalTo(6));
        assertThat(newList.hasNext(), equalTo(true));
        assertThat(newList.next(), equalTo(8));
        assertThat(newList.hasNext(), equalTo(true));
        assertThat(newList.next(), equalTo(10));
        assertThat(newList.hasNext(), equalTo(false));
        assertThat(newList.next(), equalTo(0));
        assertThat(newList.toString(), equalTo("4, 6, 8, 10"));
        assertThat(newList.original(), equalTo("4,6,8,10"));
        // Iterate through the list.
        Iterator<Integer> iter = newList.new Iter();
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo(4));
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo(6));
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo(8));
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), equalTo(10));
        assertThat(iter.hasNext(), equalTo(false));
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
    @Test
    public void testFloatList() {
        // Start with an empty list.
        FloatList newList = new FloatList();
        assertThat(newList.size(), equalTo(0));
        assertThat(newList.isEmpty(), equalTo(true));
        assertThat(newList.hasNext(), equalTo(false));
        assertThat(newList.toString(), equalTo(""));
        assertThat(newList.original(), equalTo(""));
        assertThat(newList.last(), equalTo(0.0));
        // Repeat with an empty string.
        newList = new FloatList("");
        assertThat(newList.size(), equalTo(0));
        assertThat(newList.isEmpty(), equalTo(true));
        assertThat(newList.hasNext(), equalTo(false));
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
        assertThat(newList.hasNext(), equalTo(true));
        assertThat(newList.next(), closeTo(0.4, 1e-6));
        assertThat(newList.hasNext(), equalTo(true));
        assertThat(newList.next(), closeTo(0.6, 1e-6));
        assertThat(newList.hasNext(), equalTo(true));
        assertThat(newList.next(), closeTo(0.8, 1e-6));
        assertThat(newList.hasNext(), equalTo(true));
        assertThat(newList.next(), closeTo(0.1, 1e-6));
        assertThat(newList.hasNext(), equalTo(false));
        assertThat(newList.next(), equalTo(0.0));
        assertThat(newList.toString(), equalTo("0.4, 0.6, 0.8, 0.1"));
        assertThat(newList.original(), equalTo("0.4,0.6,0.8,0.1"));
        // Iterate through the list.
        Iterator<Double> iter = newList.new Iter();
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), closeTo(0.4, 1e-6));
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), closeTo(0.6, 1e-6));
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), closeTo(0.8, 1e-6));
        assertThat(iter.hasNext(), equalTo(true));
        assertThat(iter.next(), closeTo(0.1, 1e-6));
        assertThat(iter.hasNext(), equalTo(false));
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
    @Test
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

    @Test
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

    @Test
    public void testBestColumn() {
        double[] values = { 0.0, -1.0, 1.0, 0.5, 0.3, 0.2, 0.1, 0.6 };
        BestColumn test = new BestColumn();
        for (int i = 0; i < values.length; i++)
            test.merge(i, values[i]);
        assertThat(test.getBestIdx(), equalTo(2));
        assertThat(test.getBestValue(), equalTo(1.0));
    }

    @Test
    public void testAccumulate() {
        CountMap<String> map1 = new CountMap<String>();
        map1.count("AAA", 1);
        map1.count("BBB", 2);
        map1.count("CCC", 3);
        assertThat(map1.sum(), equalTo(6));
        assertThat(map1.size(), equalTo(3));
        CountMap<String> map2 = new CountMap<String>();
        map2.count("AAA", 10);
        map2.count("CCC", 30);
        map2.count("DDD", 40);
        assertThat(map2.sum(), equalTo(80));
        assertThat(map2.size(), equalTo(3));
        map1.accumulate(map2);
        assertThat(map1.size(), equalTo(4));
        assertThat(map2.size(), equalTo(3));
        assertThat(map1.getCount("AAA"), equalTo(11));
        assertThat(map2.getCount("AAA"), equalTo(10));
        assertThat(map1.getCount("BBB"), equalTo(2));
        assertThat(map2.getCount("BBB"), equalTo(0));
        assertThat(map1.getCount("CCC"), equalTo(33));
        assertThat(map2.getCount("CCC"), equalTo(30));
        assertThat(map1.getCount("DDD"), equalTo(40));
        assertThat(map2.getCount("DDD"), equalTo(40));
        assertThat(map1.sum(), equalTo(86));
        assertThat(map2.sum(), equalTo(80));
    }

    @Test
    public void testWeightMap() {
        WeightMap map1 = new WeightMap(3);
        map1.count("AAA", 1.0);
        map1.count("AAA", 0.2);
        map1.count("AAA", 1.5);
        assertThat(map1.getCount("AAA"), closeTo(2.7, 0.05));
        map1.count("BBB", 1.3);
        map1.count("CCC", 3.5);
        assertThat(map1.size(), equalTo(3));
        assertThat(map1.getCount("BBB"), closeTo(1.3, 0.05));
        assertThat(map1.getCount("CCC"), closeTo(3.5, 0.05));
        assertThat(map1.getCount("AAA"), closeTo(2.7, 0.05));
        WeightMap map2 = new WeightMap();
        map2.count("BBB", 0.7);
        map2.count("CCC", 0.2);
        map2.count("DDD", 0.6);
        map1.accumulate(map2);
        assertThat(map1.size(), equalTo(4));
        assertThat(map1.getCount("AAA"), closeTo(2.7, 0.05));
        assertThat(map1.getCount("BBB"), closeTo(2.0, 0.05));
        assertThat(map1.getCount("CCC"), closeTo(3.7, 0.05));
        assertThat(map1.getCount("DDD"), closeTo(0.6, 0.05));
        var counts = map1.counts();
        assertThat(counts.size(), equalTo(4));
        for (var count : counts) {
            switch (count.getKey()) {
            case "AAA" :
                assertThat("AAA", count.getCount(), closeTo(2.7, 0.05));
                break;
            case "BBB" :
                assertThat("BBB", count.getCount(), closeTo(2.0, 0.05));
                break;
            case "CCC" :
                assertThat("CCC", count.getCount(), closeTo(3.7, 0.05));
                break;
            case "DDD" :
                assertThat("DDD", count.getCount(), closeTo(0.6, 0.05));
                break;
            default :
                assertThat("Invalid count key \"" + count.getKey() + "\".", false);
            }
        }
        assertThat(map1.sum(), closeTo(9.0, 0.05));
        var best = map1.getBestEntry();
        assertThat(best.getKey(), equalTo("CCC"));
        assertThat(map1.getCount("DDD"), closeTo(0.6, 0.05));
        var keys = map1.keys();
        assertThat(keys, contains("AAA", "BBB", "CCC", "DDD"));
        map2.remove("DDD");
        assertThat(map2.size(), equalTo(2));
        assertThat(map2.getCount("DDD"), equalTo(0.0));
        assertThat(map2.findCounter("DDD"), nullValue());
    }

    @Test
    public void testWeightAccum() {
        WeightMap map1 = new WeightMap(3);
        map1.count("AAA", 1.0);
        map1.count("AAA", 0.2);
        map1.count("AAA", 1.5);
        assertThat(map1.getCount("AAA"), closeTo(2.7, 0.05));
        map1.count("BBB", 1.3);
        map1.count("CCC", 3.5);
        assertThat(map1.size(), equalTo(3));
        assertThat(map1.getCount("BBB"), closeTo(1.3, 0.05));
        assertThat(map1.getCount("CCC"), closeTo(3.5, 0.05));
        assertThat(map1.getCount("AAA"), closeTo(2.7, 0.05));
        WeightMap map2 = new WeightMap();
        map2.count("BBB", 0.7);
        map2.count("CCC", 0.2);
        map2.count("DDD", 0.6);
        map1.accumulate(map2, 2.0);
        assertThat(map1.size(), equalTo(4));
        assertThat(map1.getCount("AAA"), closeTo(2.7, 0.05));
        assertThat(map1.getCount("BBB"), closeTo(2.7, 0.05));
        assertThat(map1.getCount("CCC"), closeTo(3.9, 0.05));
        assertThat(map1.getCount("DDD"), closeTo(1.2, 0.05));
    }


    public static class Thing extends MagicObject implements Comparable<Thing> {

        /**
         * serialization object type ID
         */
        private static final long serialVersionUID = -5855290474686618053L;

        /** Create a blank thing. */
        public Thing() { }

        /** Create a new thing with a given ID and description. */
        public Thing(String thingId, String thingDesc) {
            super(thingId, thingDesc);
        }

        @Override
        public int compareTo(Thing o) {
            return super.compareTo(o);
        }

        @Override
        protected String normalize(String name) {
            // Convert all sequences of non-word characters to a single space and lower-case it.
            String retVal = name.replaceAll("\\W+", " ").toLowerCase();
            return retVal;
        }

    }

    public static class ThingMap extends MagicMap<Thing> {

        public ThingMap() {
            super(new Thing());
        }

        /**
         * Find the named thing.  If it does not exist, a new thing will be created.
         *
         * @param thingDesc	the thing name
         *
         * @return	a Thing object for the thing
         */
        public Thing findOrInsert(String thingDesc) {
            Thing retVal = this.getByName(thingDesc);
            if (retVal == null) {
                // Create a thing without an ID.
                retVal = new Thing(null, thingDesc);
                // Store it in the map to create the ID.
                this.put(retVal);
            }
            return retVal;
        }

        public void save(File saveFile) {
            try {
                PrintWriter printer = new PrintWriter(saveFile);
                for (Thing thing : this.objectValues()) {
                    printer.format("%s\t%s%n", thing.getId(), thing.getName());
                }
                printer.close();
            } catch (IOException e) {
                throw new RuntimeException("Error saving thing map.", e);
            }
        }

    }

}

