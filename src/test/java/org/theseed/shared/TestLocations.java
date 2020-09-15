/**
 *
 */
package org.theseed.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.theseed.test.Matchers.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.locations.DiscreteLocationList;
import org.theseed.locations.Frame;
import org.theseed.locations.Location;
import org.theseed.locations.Region;
import org.theseed.locations.SortedLocationList;
import org.theseed.locations.DiscreteLocationList.Edge;

import junit.framework.TestCase;

/**
 * @author Bruce Parrello
 *
 */
public class TestLocations extends TestCase {

    /**
     * @param name
     */
    public TestLocations(String name) throws IOException {
        super(name);
        this.myGto = new Genome(new File("data/gto_test", "1313.7001.gto"));
    }

    private Genome myGto = null;

    /**
     * Test location merging.
     */
    public void testLocationMerge() {
        Location loc1 = Location.create("MyContig", "+", 100, 110, 200, 210);
        Location loc2 = Location.create("MyContig", "+", 120, 130, 150, 250);
        loc2.merge(loc1);
        assertThat(loc2.getContigId(), equalTo("MyContig"));
        assertThat(loc2.getDir(), equalTo('+'));
        assertThat(loc2.getLeft(), equalTo(100));
        assertThat(loc2.getRight(), equalTo(250));
        assertThat(loc2.getBegin(), equalTo(100));
        assertThat(loc2.getEnd(), equalTo(250));
        loc1.setBegin(50);
        assertThat(loc1.getLeft(), equalTo(50));
        assertThat(loc1.getBegin(), equalTo(50));
        loc2 = Location.create("MyContig", "-", 1000, 2000);
        loc1 = Location.create("MyContig", "-", 500, 2500);
        loc2.merge(loc1);
        assertThat(loc2.getContigId(), equalTo("MyContig"));
        assertThat(loc2.getDir(), equalTo('-'));
        assertThat(loc2.getLeft(), equalTo(500));
        assertThat(loc2.getRight(), equalTo(2500));
        assertThat(loc2.getBegin(), equalTo(2500));
        assertThat(loc2.getEnd(), equalTo(500));
        loc1.setBegin(2000);
        assertThat(loc1.getRight(), equalTo(2000));
        assertThat(loc1.getBegin(), equalTo(2000));
    }

    /**
     * Main discrete location list test
     */
    public void testDiscreteLocationList() {
        DiscreteLocationList newList = new DiscreteLocationList("myContig");
        Location[] locs = { Location.create("myContig", "+", 10, 99, 102, 199),
                            Location.create("myContig", "-", 100, 400),
                            Location.create("myContig", "-", 500, 999),
                            Location.create("myContig", "-", 3000, 3199),
                            Location.create("myContig", "+", 4000, 4999),
                            Location.create("myContig", "-", 4100, 4199),
                            Location.create("myContig", "-", 6000, 6199),
                            Location.create("myContig", "+", 5000, 5099),
                            Location.create("myContig", "-", 5000, 5099),
                            Location.create("myContig", "+", 5200, 5299),
                            Location.create("myContig", "-", 5250, 5299),
                            Location.create("myContig", "+", 5350, 5399),
                            Location.create("myContig", "-", 5300, 5399),
                            Location.create("myContig", "+", 6800, 7299),
                            Location.create("myContig", "+", 8000, 8199),
                            Location.create("myContig", "+", 8400, 8599),
                            Location.create("myContig", "-", 8100, 8449),
                            Location.create("myContig", "+", 9100, 9199),
                            Location.create("myContig", "+", 9200, 9299),
                            Location.create("myContig", "-", 9300, 9399),
                            Location.create("myContig", "+", 9400, 9499),
                            Location.create("myContig", "-", 9000, 9999),
                          };
        // Add all these locations to the list.
        for (Location loc : locs) {
            assertTrue("Failed to add " + loc + " to list.", newList.addLocation(loc));
        }
        Location badLoc = Location.create("yourContig", "+", 1000, 4999);
        assertFalse("Added wrong contig successfully.", newList.addLocation(badLoc));
        assertEquals("Invalid contig ID in location list.", "myContig", newList.getContigId());
        // Now we need to verify that none of the stored locations overlap.
        Iterator<Location> iter = newList.iterator();
        Location prev = iter.next();
        while (iter.hasNext()) {
            Location next = iter.next();
            assertTrue(prev + " overlaps " + next, prev.getRight() < next.getLeft());
        }
        // Next, we want to show that every location in the location list is wholly
        // contained in one or more locations of the input list.  If it is contained
        // in more than one, or it is contained in a segmented one, it should be
        // invalid.  Otherwise, it should be on the same strand.
        for (Location loc : newList) {
            // This will remember the strand of the last location found.
            char strand = '0';
            // This will be set to TRUE if a location is segmented.
            boolean invalid = false;
            // This will count the locations found.
            int found = 0;
            for (Location loc2 : locs) {
                if (loc2.contains(loc)) {
                    strand = loc2.getDir();
                    if (loc2.isSegmented()) {
                        invalid = true;
                    }
                    found++;
                }
            }
            if (found > 1) {
                assertFalse(loc + " is valid but it is in " + found + " input locations.", loc.isValid());
            } else if (invalid) {
                assertFalse(loc + " is valid but it is in a segmented input location.", loc.isValid());
            } else if (found == 0) {
                fail(loc + " was not found in the input locations.");
            } else {
                assertEquals(loc + " is not on the correct strand.", strand, loc.getDir());
            }
        }
        // Now for each input location, we need to show that every position is in a location
        // of the location list.
        for (Location loc : locs) {
            for (int pos = loc.getLeft(); pos <= loc.getRight(); pos++) {
                assertTrue("Position " + pos + " of location " + loc + " is not in the list.",
                        newList.computeStrand(pos) != '0');
            }
        }
        // Test edge computation.
        assertThat(newList.isEdge(10, false), equalTo(Edge.START));
        assertThat(newList.isEdge(50, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(197, false), equalTo(Edge.STOP));
        assertThat(newList.isEdge(400, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(102, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(999, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(502, false), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(8000, false), equalTo(Edge.START));
        assertThat(newList.isEdge(8197, false), equalTo(Edge.STOP));
        assertThat(newList.isEdge(10, true), equalTo(Edge.START));
        assertThat(newList.isEdge(50, true), equalTo(Edge.OTHER));
        assertThat(newList.isEdge(197, true), equalTo(Edge.STOP));
        assertThat(newList.isEdge(400, true), equalTo(Edge.START));
        assertThat(newList.isEdge(102, true), equalTo(Edge.STOP));
        assertThat(newList.isEdge(999, true), equalTo(Edge.START));
        assertThat(newList.isEdge(502, true), equalTo(Edge.STOP));
        assertThat(newList.isEdge(8000, true), equalTo(Edge.START));
        assertThat(newList.isEdge(8197, true), equalTo(Edge.STOP));
        // Finally, we want to test the frame computation.
        assertEquals("Invalid frame for contig start.", Frame.F0, newList.computeRegionFrame(1, 9));
        assertEquals("Invalid frame for segmented position.", Frame.XX, newList.computeRegionFrame(40, 45));
        assertEquals("Invalid frame for simple minus position.", Frame.M1, newList.computeRegionFrame(390, 399));
        assertEquals("Invalid frame for simple plus position.", Frame.P0, newList.computeRegionFrame(4009, 4054));
        assertEquals("Invalid frame for near-overlap position.", Frame.P2, newList.computeRegionFrame(5235, 5245));
        assertEquals("Invalid frame for overlap position.", Frame.XX, newList.computeRegionFrame(5235, 5255));
        assertEquals("Invalid frame for extron position.", Frame.F0, newList.computeRegionFrame(7306, 7316));
    }

    /**
     * Test overlap testing
     */
    public void testLocOverlap() {
        Location loc1 = Location.create("c1", 100, 200);
        Location loc2 = Location.create("c2", 50, 150);
        Location loc3 = Location.create("c1", 400, 200);
        Location loc4 = Location.create("c1", 50, 150);
        Location loc5 = Location.create("c1", 90, 10);
        Location loc6 = Location.create("c1", 210, 300);
        assertFalse(loc1.isOverlapping(loc2));
        assertTrue(loc1.isOverlapping(loc3));
        assertTrue(loc1.isOverlapping(loc4));
        assertFalse(loc1.isOverlapping(loc5));
        assertFalse(loc1.isOverlapping(loc6));
        assertFalse(loc2.isOverlapping(loc1));
        assertFalse(loc2.isOverlapping(loc3));
        assertFalse(loc2.isOverlapping(loc4));
        assertFalse(loc2.isOverlapping(loc5));
        assertFalse(loc2.isOverlapping(loc6));
        assertTrue(loc3.isOverlapping(loc1));
        assertFalse(loc3.isOverlapping(loc2));
        assertFalse(loc3.isOverlapping(loc4));
        assertFalse(loc3.isOverlapping(loc5));
        assertTrue(loc3.isOverlapping(loc6));
    }

    /**
     * test the sorted location list
     */
    public void testSortedLocations() {
        // Create some test locations.
        Location loc1 = Location.create("c1", "+", 100, 200);
        Location loc2 = Location.create("c1", "-", 150, 250);
        Location loc3 = Location.create("c1", "+", 1000, 2000);
        Location loc4 = Location.create("c1", "+", 1100, 1500);
        Location loc5 = Location.create("c1", "-", 1100, 1500);
        Location loc6 = Location.create("c2", "+", 600, 700);
        Location loc7 = Location.create("c2", "-", 600, 800);
        // Start building the list.
        SortedLocationList locList = new SortedLocationList(loc7, loc2, loc3, loc4, loc1);
        assertThat(locList.get(0), sameInstance(loc1));
        assertThat(locList.get(1), sameInstance(loc2));
        assertThat(locList.get(2), sameInstance(loc3));
        assertThat(locList.get(3), sameInstance(loc4));
        assertThat(locList.get(4), sameInstance(loc7));
        assertNull(locList.get(5));
        locList.addAll(loc5, loc6, loc3);
        assertThat(locList.get(0), sameInstance(loc1));
        assertThat(locList.get(1), sameInstance(loc2));
        assertThat(locList.get(2), sameInstance(loc3));
        assertThat(locList.get(3), sameInstance(loc3));
        assertThat(locList.get(4), sameInstance(loc4));
        assertThat(locList.get(5), sameInstance(loc5));
        assertThat(locList.get(6), sameInstance(loc6));
        assertThat(locList.get(7), sameInstance(loc7));
        Iterator<Location> iter = locList.iterator();
        Location old = iter.next();
        while (iter.hasNext()) {
            Location loc = iter.next();
            assertTrue(old.compareTo(loc) <= 0);
            old = loc;
        }
        // Test sublists.
        List<Location> subList = locList.contigRange(3);
        assertThat(subList, contains(loc3, loc4, loc5));
        subList = locList.contigRange(6);
        assertThat(subList, contains(loc6, loc7));
        // Test contains.  This location is a dup of loc1.
        old = Location.create("c1", "+", 100, 200);
        assertTrue(locList.contains(old));
        assertThat(locList.size(), equalTo(8));
        Location[] locs = locList.toArray();
        assertThat(locs[0], sameInstance(loc1));
        assertThat(locs[1], sameInstance(loc2));
        assertThat(locs[2], sameInstance(loc3));
        assertThat(locs[3], sameInstance(loc3));
        assertThat(locs[4], sameInstance(loc4));
        assertThat(locs[5], sameInstance(loc5));
        assertThat(locs[6], sameInstance(loc6));
        assertThat(locs[7], sameInstance(loc7));
        locList.clear();
        assertThat(locList.size(), equalTo(0));
        iter = locList.iterator();
        assertFalse(iter.hasNext());
        locList.add(loc4);
        assertThat(locList.size(), equalTo(1));
        assertThat(locList.get(0), equalTo(loc4));
    }

    /**
     * Basic test for location-list maps.
     */
    public void testContigMapping() {
        Map<String, DiscreteLocationList> gList = DiscreteLocationList.createGenomeCodingMap(this.myGto);
        DiscreteLocationList contig0036 = gList.get("1313.7001.con.0036");
        assertNotNull("Contig 0036 not found.", contig0036);
        assertEquals("Incorrect strand found for test position 33996.", '+', contig0036.computeStrand(33996));
        assertEquals("Incorrect strand found for test position 30980.", '-', contig0036.computeStrand(30980));
        assertEquals("Incorrect strand found for test position 30984.", '0', contig0036.computeStrand(30984));
    }
    /**
     * Test of plus-locations.
     */
    public void testPlusLocations() {
        Location plusLoc = Location.create("mySequence1", "+");
        plusLoc.addRegion(402, 500);
        assertFalse("Segmentation error on single segment.", plusLoc.isSegmented());
        assertTrue("Location does not default to valid.", plusLoc.isValid());
        assertEquals("Invalid begin in plusLoc.", 402, plusLoc.getBegin());
        plusLoc.addRegion(10, 200);
        assertEquals("Invalid left position in plusLoc.", 10, plusLoc.getLeft());
        assertEquals("Invalid right position in plusLoc.", 901, plusLoc.getRight());
        assertEquals("Invalid contig in plusLoc.", "mySequence1", plusLoc.getContigId());
        assertEquals("Invalid strand in plusLoc.", '+', plusLoc.getDir());
        assertEquals("Invalid length in plusLoc.", 892, plusLoc.getLength());
        assertEquals("Invalid begin in plusLoc after add.", 10, plusLoc.getBegin());
        plusLoc.putRegion(1, 6);
        assertEquals("Invalid left position in plusLoc after put.", 1, plusLoc.getLeft());
        assertEquals("Invalid right position in plusLoc after put.", 901, plusLoc.getRight());
        plusLoc.putRegion(250, 349);
        assertEquals("Invalid left position in plusLoc after internal put.", 1, plusLoc.getLeft());
        assertEquals("Invalid right position in plusLoc after internal put.", 901, plusLoc.getRight());
        assertTrue("Segmentation error on multiple segments.", plusLoc.isSegmented());
        plusLoc.invalidate();
        assertFalse("Location did not invalidate.", plusLoc.isValid());
        plusLoc.setLeft(260);
        Collection<Region> plusRegions = plusLoc.getRegions();
        for (Region region : plusRegions) {
            int left = region.getLeft();
            assertTrue("Invalid region " + region + " extends past 260.", left >= 260);
        }
        Location cloneLoc = (Location) plusLoc.clone();
        assertEquals("Clone contig does not match.", plusLoc.getContigId(), cloneLoc.getContigId());
        assertEquals("Clone direction does not match.", plusLoc.getDir(), cloneLoc.getDir());
        assertEquals("Clone left does not match.", plusLoc.getLeft(), cloneLoc.getLeft());
        assertEquals("Clone length does not match.", plusLoc.getLength(), cloneLoc.getLength());
        assertTrue("Segmentation error on clone.", cloneLoc.isSegmented());
        assertFalse("Validation error on clone.", cloneLoc.isValid());
        Collection<Region> cloneRegions = cloneLoc.getRegions();
        for (Region region : plusRegions) {
            assertTrue("Region " + region + " not found in clone.", region.containedIn(cloneRegions));
        }
    }

    /**
     * Test of minus-locations.
     */
    public void testMinusLocations() {
        Location minusLoc = Location.create("mySequence1", "-");
        minusLoc.addRegion(901, 500);
        assertFalse("Segmentation error on single segment.", minusLoc.isSegmented());
        assertTrue("Location does not default to valid.", minusLoc.isValid());
        assertEquals("Invalid begin in minusLoc.", 901, minusLoc.getBegin());
        minusLoc.addRegion(209, 200);
        assertEquals("Invalid left position in minusLoc.", 10, minusLoc.getLeft());
        assertEquals("Invalid right position in minusLoc.", 901, minusLoc.getRight());
        assertEquals("Invalid contig in minusLoc.", "mySequence1", minusLoc.getContigId());
        assertEquals("Invalid strand in minusLoc.", '-', minusLoc.getDir());
        assertEquals("Invalid length in minusLoc.", 892, minusLoc.getLength());
        assertEquals("Invalid begin in minusLoc after add.", 901, minusLoc.getBegin());
        minusLoc.putRegion(1, 6);
        assertEquals("Invalid left position in minusLoc after put.", 1, minusLoc.getLeft());
        assertEquals("Invalid right position in minusLoc after put.", 901, minusLoc.getRight());
        minusLoc.putRegion(250, 349);
        assertEquals("Invalid left position in minusLoc after internal put.", 1, minusLoc.getLeft());
        assertEquals("Invalid right position in minusLoc after internal put.", 901, minusLoc.getRight());
        assertTrue("Segmentation error on multiple segments.", minusLoc.isSegmented());
        minusLoc.invalidate();
        assertFalse("Location did not invalidate.", minusLoc.isValid());
        minusLoc.setRight(260);
        Collection<Region> minusRegions = minusLoc.getRegions();
        for (Region region : minusRegions) {
            int right = region.getRight();
            assertTrue("Invalid region extends to " + right + " past 260.", right <= 260);
        }
        Location cloneLoc = (Location) minusLoc.clone();
        assertEquals("Clone contig does not match.", minusLoc.getContigId(), cloneLoc.getContigId());
        assertEquals("Clone direction does not match.", minusLoc.getDir(), cloneLoc.getDir());
        assertEquals("Clone left does not match.", minusLoc.getLeft(), cloneLoc.getLeft());
        assertEquals("Clone length does not match.", minusLoc.getLength(), cloneLoc.getLength());
        assertTrue("Segmentation error on clone.", cloneLoc.isSegmented());
        assertFalse("Validation error on clone.", cloneLoc.isValid());
        Collection<Region> cloneRegions = cloneLoc.getRegions();
        for (Region region :minusRegions) {
            assertTrue("Region " + region + " not found in clone.", region.containedIn(cloneRegions));
        }
    }

    /**
     * Test of simple locations
     */
    public void testSimpleLocations() {
        Location loc1 = Location.create("contig1", 100, 200);
        assertThat(loc1.getDir(), equalTo('+'));
        assertThat(loc1.getContigId(), equalTo("contig1"));
        assertThat(loc1.getBegin(), equalTo(100));
        assertThat(loc1.getEnd(), equalTo(200));
        assertThat(loc1.getLeft(), equalTo(100));
        assertThat(loc1.getRight(), equalTo(200));
        loc1 = Location.create("contig2", 200, 100);
        assertThat(loc1.getContigId(), equalTo("contig2"));
        assertThat(loc1.getDir(), equalTo('-'));
        assertThat(loc1.getBegin(), equalTo(200));
        assertThat(loc1.getEnd(), equalTo(100));
        assertThat(loc1.getLeft(), equalTo(100));
        assertThat(loc1.getRight(), equalTo(200));
    }

    /**
     * Location comparison test
     */
    public void testLocations() {
        Region region1 = new Region(1000, 2000);
        Region region2 = new Region(1000, 2000);
        Region region3 = new Region(2000, 3000);
        assertTrue("region1 not equal to region 2.", region1.equals(region2));
        assertFalse("region1 equal to region 3.", region1.equals(region3));
        assertTrue("Region equals is not commutative.", region2.equals(region1));
        assertEquals("Equal regions have different hash codes.", region1.hashCode(), region2.hashCode());
        assertThat(region1.getBegin("+"), equalTo(1000));
        assertThat(region1.getBegin("-"), equalTo(2000));
        assertThat(region1.getLength(), equalTo(1001));
        Location loc1 = Location.create("myContig", "+", 1000, 1999);
        Location loc2 = Location.create("myContig", "-", 1100, 1199);
        Location loc3 = Location.create("myContig", "-", 1150, 1249);
        Location loc4 = Location.create("yourContig", "-", 1150, 1249);
        assertTrue("loc1 is not less than loc2.", loc1.compareTo(loc2) < 0);
        assertTrue("loc2 is not less than loc3.", loc2.compareTo(loc3) < 0);
        assertTrue("loc1 does not contain loc2.", loc1.contains(loc2));
        assertFalse("loc2 contains loc3.", loc2.contains(loc3));
        assertFalse("loc3 contains loc2.", loc3.contains(loc2));
        assertFalse("loc1 contains loc4.", loc1.contains(loc4));
        assertThat(loc1.offsetPoint(100), equalTo(1100));
        assertThat(loc1.offsetPoint(-100), equalTo(900));
        assertThat(loc4.offsetPoint(100), equalTo(1149));
        assertThat(loc4.offsetPoint(-100), equalTo(1349));
        Location loc5 = Location.create("myContig", "+",  1000, 2000, 3000, 4000);
        Location loc6 = Location.create("myContig", "+", 3000, 4000, 1000, 2000);
        Location loc7 = Location.create("yourContig", "+", 1000, 2000, 3000, 4000);
        Location loc8 = Location.create("myContig", "-", 1000, 2000, 3000, 4000);
        Location loc9 = Location.create("myContig",  "+",  1000, 1999, 3000, 4000);
        assertTrue("loc5 not equal to loc6.", loc5.equals(loc6));
        assertEquals("Equal locations have different hash codes.", loc5.hashCode(), loc6.hashCode());
        assertFalse("Different contigs compare equal.", loc5.equals(loc7));
        assertFalse("Different strands compare equal.", loc5.equals(loc8));
        assertFalse("Different region counts compare equal.", loc5.equals(loc1));
        assertFalse("Different region extents compare equal.", loc5.equals(loc9));
        Location[] locArray = new Location[] { loc1, loc2, loc3, loc4, loc5, loc6, loc7, loc8, loc9 };
        for (Location loc : locArray) {
            String locString = loc.toString();
            Location locRecurse = Location.fromString(locString);
            assertThat(locString, locRecurse.getContigId(), equalTo(loc.getContigId()));
            assertThat(locString, locRecurse.getDir(), equalTo(loc.getDir()));
            assertThat(locString, locRecurse.getLeft(), equalTo(loc.getLeft()));
            assertThat(locString, locRecurse.getRight(), equalTo(loc.getRight()));
            assertThat(locString, locRecurse.getLength(), equalTo(loc.getLength()));
            assertTrue(locString, locRecurse.equals(loc));
            String locString2 = locRecurse.toString();
            assertThat(locString2, equalTo(locString));
        }
        // Test upstream extension.
        Location loc9u = loc9.expandUpstream(1500, 5000);
        assertThat(loc9.getLeft(), equalTo(1000));
        assertThat(loc9.getRight(), equalTo(4000));
        assertThat(loc9u.getLeft(), equalTo(1));
        assertThat(loc9u.getRight(), equalTo(4000));
        assertThat(loc9u.getContigId(), equalTo(loc9.getContigId()));
        loc9u = loc9.expandUpstream(500, 5000);
        assertThat(loc9u.getLeft(), equalTo(500));
        assertThat(loc9u.getRegionLength(), equalTo(loc9.getRegionLength() + 500));
        Location loc8u = loc8.expandUpstream(1500, 5000);
        assertThat(loc8.getLeft(), equalTo(1000));
        assertThat(loc8.getRight(), equalTo(4000));
        assertThat(loc8u.getLeft(), equalTo(1000));
        assertThat(loc8u.getRight(), equalTo(5000));
        assertThat(loc8u.getContigId(), equalTo(loc8.getContigId()));
        loc8u = loc8.expandUpstream(500, 5000);
        assertThat(loc8u.getRight(), equalTo(4500));
        assertThat(loc8u.getRegionLength(), equalTo(loc8.getRegionLength() + 500));
        // Test the strand sorter.
        Arrays.sort(locArray, new Location.StrandSorter());
        for (int i = 1; i < locArray.length; i++) {
            Location oldLoc = locArray[i-1];
            assertThat(oldLoc.getContigId(), lessThanOrEqualTo(locArray[i].getContigId()));
            if (oldLoc.getContigId().contentEquals(locArray[i].getContigId())) {
                assertThat(oldLoc.getDir(), lessThanOrEqualTo(locArray[i].getDir()));
                if (oldLoc.getDir() == locArray[i].getDir())
                    assertThat(oldLoc.getLeft(), lessThanOrEqualTo(locArray[i].getLeft()));
            }
        }
        Location locA = Location.copy(loc1);
        assertThat(locA.getContigId(), equalTo("myContig"));
        assertThat(locA.getBegin(), equalTo(1000));
        assertThat(locA.getEnd(), equalTo(1999));
        locA.expand(100, 101, 3000);
        assertThat(locA.getContigId(), equalTo("myContig"));
        assertThat(locA.getBegin(), equalTo(900));
        assertThat(locA.getEnd(), equalTo(2100));
        locA.expand(1000, 500, 3000);
        assertThat(locA.getContigId(), equalTo("myContig"));
        assertThat(locA.getBegin(), equalTo(1));
        assertThat(locA.getEnd(), equalTo(2600));
        locA.expand(0, 500, 3000);
        assertThat(locA.getContigId(), equalTo("myContig"));
        assertThat(locA.getBegin(), equalTo(1));
        assertThat(locA.getEnd(), equalTo(3000));
        // Test frames
        for (int i = 0; i < 200; i += 3) {
            assertThat(Location.create("Mycontig", "+", i, i+5).getFrame(), equalTo(Frame.P0));
            assertThat(Location.create("Mycontig", "+", i+1, i+6).getFrame(), equalTo(Frame.P1));
            assertThat(Location.create("Mycontig", "+", i+2, i+7).getFrame(), equalTo(Frame.P2));
            assertThat(Location.create("Mycontig", "-", i, i+5).getFrame(), equalTo(Frame.M0));
            assertThat(Location.create("Mycontig", "-", i-1, i+4).getFrame(), equalTo(Frame.M1));
            assertThat(Location.create("Mycontig", "-", i-2, i+3).getFrame(), equalTo(Frame.M2));
        }
    }

    /**
     * Test location addition.
     */
    public void testLocationAdd() {
        Location loc1 = Location.create("contig1", "-", 1000, 1999, 3000, 3999);
        Location loc2 = Location.create("contig1",  "-", 2100, 2899);
        assertThat(loc1.getLeft(), equalTo(1000));
        assertThat(loc1.getBegin(), equalTo(3999));
        assertThat(loc1.getRight(), equalTo(3999));
        assertThat(loc1.getEnd(), equalTo(1000));
        assertThat(loc1.getRegionLength(), equalTo(2000));
        assertThat(loc1.getLength(), equalTo(3000));
        assertThat(loc1.getRegions().size(), equalTo(2));
        loc1.add(loc2);
        assertThat(loc1.getLeft(), equalTo(1000));
        assertThat(loc1.getBegin(), equalTo(3999));
        assertThat(loc1.getRight(), equalTo(3999));
        assertThat(loc1.getEnd(), equalTo(1000));
        assertThat(loc1.getRegionLength(), equalTo(2800));
        assertThat(loc1.getLength(), equalTo(3000));
        assertThat(loc1.getRegions().size(), equalTo(3));
        loc2 = Location.create("contig1",  "-", 5000, 5999);
        loc1.add(loc2);
        assertThat(loc1.getLeft(), equalTo(1000));
        assertThat(loc1.getBegin(), equalTo(5999));
        assertThat(loc1.getRight(), equalTo(5999));
        assertThat(loc1.getEnd(), equalTo(1000));
        assertThat(loc1.getRegionLength(), equalTo(3800));
        assertThat(loc1.getLength(), equalTo(5000));
        assertThat(loc1.getRegions().size(), equalTo(4));
    }

    /**
     * Parse seed locations.
     */
    public void testSeedParse() {
        Location loc = Location.parseSeedLocation("NC_10045_1000_1999");
        assertThat(loc.getContigId(), equalTo("NC_10045"));
        assertThat(loc.getBegin(), equalTo(1000));
        assertThat(loc.getEnd(), equalTo(1999));
        assertThat(loc.getDir(), equalTo('+'));
        loc = Location.parseSeedLocation("frogger_2999_2000");
        assertThat(loc.getContigId(), equalTo("frogger"));
        assertThat(loc.getLeft(), equalTo(2000));
        assertThat(loc.getRight(), equalTo(2999));
        assertThat(loc.getDir(), equalTo('-'));
        loc = Location.parseSeedLocation("NC_1000_2100_2000,NC_1000_2200_2150");
        assertThat(loc.getContigId(), equalTo("NC_1000"));
        assertThat(loc.getDir(), equalTo('-'));
        List<Region> regions = loc.getRegions();
        assertThat(regions.get(0).getLeft(), equalTo(2000));
        assertThat(regions.get(0).getRight(), equalTo(2100));
        assertThat(regions.get(1).getLeft(), equalTo(2150));
        assertThat(regions.get(1).getRight(), equalTo(2200));
    }

    public void testDistance() {
        Location t1 = Location.create("myContig", "+", 10, 20);
        Location t2 = Location.create("myContig", "-", 25, 45);
        assertEquals("Wrong initial distance.", 4, t1.distance(t2));
        assertEquals("Distance not commutative", 4, t2.distance(t1));
        t1.setRight(24);
        assertEquals("Distance wrong when left adjacent.", 0, t1.distance(t2));
        t1.setRight(25);
        assertEquals("Distance wrong at overlap left edge.", -1, t1.distance(t2));
        t1.setRight(100);
        assertEquals("Distance wrong when contained.", -1, t1.distance(t2));
        t1.setLeft(45);
        assertEquals("Distance wrong at overlap right edge.", -1, t1.distance(t2));
        t1.setLeft(46);
        assertEquals("Distance wrong at right adjacent.", 0, t1.distance(t2));
        t1.setLeft(51);
        assertEquals("Distance wrong at end.", 5, t1.distance(t2));
        Feature f1 = myGto.getFeature("fig|1313.7001.peg.841");
        Feature f2 = myGto.getFeature("fig|1313.7001.peg.847");
        assertEquals("Feature distance not equal location distance.", f1.getLocation().distance(f2.getLocation()),
                f1.distance(f2));
        Feature f3 = myGto.getFeature("fig|1313.7001.peg.1113");
        assertEquals("Contig mismatch not caught.", Integer.MAX_VALUE, f1.distance(f3));
    }

    public void testUpstream() {
        Location t1 = Location.create("myContig", "+", 400, 500);
        Location t2 = Location.create("otherContig", "+", 200, 300);
        assertThat(t1.upstreamDistance(t2), equalTo(Integer.MAX_VALUE));
        assertThat(t1.isUpstream(t2), isFalse());
        assertThat(t2.isUpstream(t1), isFalse());
        t2 = Location.create("myContig", "-", 200, 300);
        assertThat(t1.upstreamDistance(t2), equalTo(Integer.MAX_VALUE));
        assertThat(t1.isUpstream(t2), isFalse());
        assertThat(t2.isUpstream(t1), isFalse());
        t2 = Location.create("myContig", "+", 200, 300);
        assertThat(t1.upstreamDistance(t2), equalTo(99));
        assertThat(t2.upstreamDistance(t1), equalTo(Integer.MAX_VALUE));
        assertThat(t1.isUpstream(t2), isFalse());
        assertThat(t2.isUpstream(t1), isTrue());
        t2 = Location.create("myContig", "+", 600, 700);
        assertThat(t1.upstreamDistance(t2), equalTo(Integer.MAX_VALUE));
        t1 = Location.create("myContig", "-", 600, 700);
        t2 = Location.create("myContig", "-", 1000, 1100);
        assertThat(t1.upstreamDistance(t2), equalTo(299));
        assertThat(t2.upstreamDistance(t1), equalTo(Integer.MAX_VALUE));
        assertThat(t1.isUpstream(t2), isFalse());
        assertThat(t2.isUpstream(t1), isTrue());
        t2 = t1.upstream(50);
        assertThat(t1.upstreamDistance(t2), equalTo(0));
        assertThat(t2.getLength(), equalTo(50));
        assertThat(t2.getDir(), equalTo(t1.getDir()));
        assertThat(t2.getContigId(), equalTo(t1.getContigId()));
        t1 = Location.create("otherContig", "+", 200, 300);
        t2 = t1.upstream(50);
        assertThat(t1.upstreamDistance(t2), equalTo(0));
        assertThat(t2.getLength(), equalTo(50));
        assertThat(t2.getDir(), equalTo(t1.getDir()));
        assertThat(t2.getContigId(), equalTo(t1.getContigId()));
        assertThat(t1.isUpstream(t2), isFalse());
        assertThat(t2.isUpstream(t1), isTrue());
    }

    public void testFeatureUpstream() {
        Feature feat = myGto.getFeature("fig|1313.7001.peg.1600");
        Feature feat2 = myGto.getFeature("fig|1313.7001.peg.1631");
        assertThat(feat.isUpstream(feat2), isTrue());
        assertThat(feat2.isUpstream(feat), isFalse());
        feat = myGto.getFeature("fig|1313.7001.peg.1651");
        assertThat(feat.isUpstream(feat2), isFalse());
        assertThat(feat2.isUpstream(feat), isFalse());
        feat2 = myGto.getFeature("fig|1313.7001.peg.1605");
        assertThat(feat.isUpstream(feat2), isTrue());
        assertThat(feat2.isUpstream(feat), isFalse());
    }

    public void testSubLocations() {
        Location loc1 = Location.create("contig1", "+", 1000, 1099, 1200, 1299, 1400, 1499);
        Location loc2 = Location.create("contig2", "-", 1400, 1499, 1200, 1299, 1000, 1099);
        Location loc1a = loc1.subLocation(150, 120);
        assertThat(loc1a.getContigId(), equalTo(loc1.getContigId()));
        assertThat(loc1a.getRegionLength(), equalTo(120));
        assertThat(loc1a.getLeft(), equalTo(1250));
        Location loc2a = loc2.subLocation(110, 120);
        assertThat(loc2a.getContigId(), equalTo(loc2.getContigId()));
        assertThat(loc2a.getRegionLength(), equalTo(120));
        assertThat(loc2a.getRight(), equalTo(1289));
        loc1a = loc1.subLocation(50, 200);
        assertThat(loc1a.getContigId(), equalTo(loc1.getContigId()));
        assertThat(loc1a.getRegionLength(), equalTo(200));
        assertThat(loc1a.getLeft(), equalTo(1050));
        loc2a = loc2.subLocation(50, 200);
        assertThat(loc2a.getContigId(), equalTo(loc2.getContigId()));
        assertThat(loc2a.getRegionLength(), equalTo(200));
        assertThat(loc2a.getRight(), equalTo(1449));
    }

}
