/**
 *
 */
package org.theseed.locations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.locations.DiscreteLocationList.Edge;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestLocations {

    private static Genome myGto = null;

    public TestLocations() throws IOException {
        myGto = new Genome(new File("data/gto_test", "1313.7001.gto"));
    }

    /**
     * Test location merging.
     */
    @Test
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
    @Test
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
            assertThat("Failed to add " + loc + " to list.", newList.addLocation(loc), equalTo(true));
        }
        Location badLoc = Location.create("yourContig", "+", 1000, 4999);
        assertThat("Added wrong contig successfully.", newList.addLocation(badLoc), equalTo(false));
        assertThat("Invalid contig ID in location list.", newList.getContigId(), equalTo("myContig"));
        // Now we need to verify that none of the stored locations overlap.
        Iterator<Location> iter = newList.iterator();
        Location prev = iter.next();
        while (iter.hasNext()) {
            Location next = iter.next();
            assertThat(prev + " overlaps " + next, prev.getRight() < next.getLeft(), equalTo(true));
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
                assertThat(loc + " is valid but it is in " + found + " input locations.", loc.isValid(), equalTo(false));
            } else if (invalid) {
                assertThat(loc + " is valid but it is in a segmented input location.", loc.isValid(), equalTo(false));
            } else if (found == 0) {
                fail(loc + " was not found in the input locations.");
            } else {
                assertThat(loc + " is not on the correct strand.", loc.getDir(), equalTo(strand));
            }
        }
        // Now for each input location, we need to show that every position is in a location
        // of the location list.
        for (Location loc : locs) {
            for (int pos = loc.getLeft(); pos <= loc.getRight(); pos++) {
                assertThat("Position " + pos + " of location " + loc + " is not in the list.",
                        newList.computeStrand(pos) != '0', equalTo(true));
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
        assertThat("Invalid frame for contig start.", newList.computeRegionFrame(1, 9), equalTo(Frame.F0));
        assertThat("Invalid frame for segmented position.", newList.computeRegionFrame(40, 45), equalTo(Frame.XX));
        assertThat("Invalid frame for simple minus position.", newList.computeRegionFrame(390, 399), equalTo(Frame.M1));
        assertThat("Invalid frame for simple plus position.", newList.computeRegionFrame(4009, 4054), equalTo(Frame.P0));
        assertThat("Invalid frame for near-overlap position.", newList.computeRegionFrame(5235, 5245), equalTo(Frame.P2));
        assertThat("Invalid frame for overlap position.", newList.computeRegionFrame(5235, 5255), equalTo(Frame.XX));
        assertThat("Invalid frame for extron position.", newList.computeRegionFrame(7306, 7316), equalTo(Frame.F0));
    }

    /**
     * Test overlap testing
     */
    @Test
    public void testLocOverlap() {
        Location loc1 = Location.create("c1", 100, 200);
        Location loc2 = Location.create("c2", 50, 150);
        Location loc3 = Location.create("c1", 400, 200);
        Location loc4 = Location.create("c1", 50, 150);
        Location loc5 = Location.create("c1", 90, 10);
        Location loc6 = Location.create("c1", 210, 300);
        assertThat(loc1.isOverlapping(loc2), equalTo(false));
        assertThat(loc1.isOverlapping(loc3), equalTo(true));
        assertThat(loc1.isOverlapping(loc4), equalTo(true));
        assertThat(loc1.isOverlapping(loc5), equalTo(false));
        assertThat(loc1.isOverlapping(loc6), equalTo(false));
        assertThat(loc2.isOverlapping(loc1), equalTo(false));
        assertThat(loc2.isOverlapping(loc3), equalTo(false));
        assertThat(loc2.isOverlapping(loc4), equalTo(false));
        assertThat(loc2.isOverlapping(loc5), equalTo(false));
        assertThat(loc2.isOverlapping(loc6), equalTo(false));
        assertThat(loc3.isOverlapping(loc1), equalTo(true));
        assertThat(loc3.isOverlapping(loc2), equalTo(false));
        assertThat(loc3.isOverlapping(loc4), equalTo(false));
        assertThat(loc3.isOverlapping(loc5), equalTo(false));
        assertThat(loc3.isOverlapping(loc6), equalTo(true));
    }

    /**
     * test the sorted location list
     */
    @Test
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
        assertThat(locList.get(5), nullValue());
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
            assertThat(old.compareTo(loc) <= 0, equalTo(true));
            old = loc;
        }
        // Test sublists.
        List<Location> subList = locList.contigRange(3);
        assertThat(subList, contains(loc3, loc4, loc5));
        subList = locList.contigRange(6);
        assertThat(subList, contains(loc6, loc7));
        // Test contains.  This location is a dup of loc1.
        old = Location.create("c1", "+", 100, 200);
        assertThat(locList.contains(old), equalTo(true));
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
        assertThat(iter.hasNext(), equalTo(false));
        locList.add(loc4);
        assertThat(locList.size(), equalTo(1));
        assertThat(locList.get(0), equalTo(loc4));
    }

    /**
     * Basic test for location-list maps.
     */
    @Test
    public void testContigMapping() {
        Map<String, DiscreteLocationList> gList = DiscreteLocationList.createGenomeCodingMap(myGto);
        DiscreteLocationList contig0036 = gList.get("1313.7001.con.0036");
        assertThat("Contig 0036 not found.", contig0036, not(nullValue()));
        assertThat("Incorrect strand found for test position 33996.", contig0036.computeStrand(33996), equalTo('+'));
        assertThat("Incorrect strand found for test position 30980.", contig0036.computeStrand(30980), equalTo('-'));
        assertThat("Incorrect strand found for test position 30984.", contig0036.computeStrand(30984), equalTo('0'));
    }
    /**
     * Test of plus-locations.
     */
    @Test
    public void testPlusLocations() {
        Location plusLoc = Location.create("mySequence1", "+");
        plusLoc.addRegion(402, 500);
        assertThat("Segmentation error on single segment.", plusLoc.isSegmented(), equalTo(false));
        assertThat("Location does not default to valid.", plusLoc.isValid(), equalTo(true));
        assertThat("Invalid begin in plusLoc.", plusLoc.getBegin(), equalTo(402));
        plusLoc.addRegion(10, 200);
        assertThat("Invalid left position in plusLoc.", plusLoc.getLeft(), equalTo(10));
        assertThat("Invalid right position in plusLoc.", plusLoc.getRight(), equalTo(901));
        assertThat("Invalid contig in plusLoc.", plusLoc.getContigId(), equalTo("mySequence1"));
        assertThat("Invalid strand in plusLoc.", plusLoc.getDir(), equalTo('+'));
        assertThat("Invalid length in plusLoc.", plusLoc.getLength(), equalTo(892));
        assertThat("Invalid begin in plusLoc after add.", plusLoc.getBegin(), equalTo(10));
        plusLoc.putRegion(1, 6);
        assertThat("Invalid left position in plusLoc after put.", plusLoc.getLeft(), equalTo(1));
        assertThat("Invalid right position in plusLoc after put.", plusLoc.getRight(), equalTo(901));
        plusLoc.putRegion(250, 349);
        assertThat("Invalid left position in plusLoc after internal put.", plusLoc.getLeft(), equalTo(1));
        assertThat("Invalid right position in plusLoc after internal put.", plusLoc.getRight(), equalTo(901));
        assertThat("Segmentation error on multiple segments.", plusLoc.isSegmented(), equalTo(true));
        plusLoc.invalidate();
        assertThat("Location did not invalidate.", plusLoc.isValid(), equalTo(false));
        plusLoc.setLeft(260);
        Collection<Region> plusRegions = plusLoc.getRegions();
        for (Region region : plusRegions) {
            int left = region.getLeft();
            assertThat("Invalid region " + region + " extends past 260.", left >= 260, equalTo(true));
        }
        Location cloneLoc = (Location) plusLoc.clone();
        assertThat("Clone contig does not match.", cloneLoc.getContigId(), equalTo(plusLoc.getContigId()));
        assertThat("Clone direction does not match.", cloneLoc.getDir(), equalTo(plusLoc.getDir()));
        assertThat("Clone left does not match.", cloneLoc.getLeft(), equalTo(plusLoc.getLeft()));
        assertThat("Clone length does not match.", cloneLoc.getLength(), equalTo(plusLoc.getLength()));
        assertThat("Segmentation error on clone.", cloneLoc.isSegmented(), equalTo(true));
        assertThat("Validation error on clone.", cloneLoc.isValid(), equalTo(false));
        Collection<Region> cloneRegions = cloneLoc.getRegions();
        for (Region region : plusRegions) {
            assertThat("Region " + region + " not found in clone.", region.containedIn(cloneRegions), equalTo(true));
        }
    }

    /**
     * Test of minus-locations.
     */
    @Test
    public void testMinusLocations() {
        Location minusLoc = Location.create("mySequence1", "-");
        minusLoc.addRegion(901, 500);
        assertThat("Segmentation error on single segment.", minusLoc.isSegmented(), equalTo(false));
        assertThat("Location does not default to valid.", minusLoc.isValid(), equalTo(true));
        assertThat("Invalid begin in minusLoc.", minusLoc.getBegin(), equalTo(901));
        minusLoc.addRegion(209, 200);
        assertThat("Invalid left position in minusLoc.", minusLoc.getLeft(), equalTo(10));
        assertThat("Invalid right position in minusLoc.", minusLoc.getRight(), equalTo(901));
        assertThat("Invalid contig in minusLoc.", minusLoc.getContigId(), equalTo("mySequence1"));
        assertThat("Invalid strand in minusLoc.", minusLoc.getDir(), equalTo('-'));
        assertThat("Invalid length in minusLoc.", minusLoc.getLength(), equalTo(892));
        assertThat("Invalid begin in minusLoc after add.", minusLoc.getBegin(), equalTo(901));
        minusLoc.putRegion(1, 6);
        assertThat("Invalid left position in minusLoc after put.", minusLoc.getLeft(), equalTo(1));
        assertThat("Invalid right position in minusLoc after put.", minusLoc.getRight(), equalTo(901));
        minusLoc.putRegion(250, 349);
        assertThat("Invalid left position in minusLoc after internal put.", minusLoc.getLeft(), equalTo(1));
        assertThat("Invalid right position in minusLoc after internal put.", minusLoc.getRight(), equalTo(901));
        assertThat("Segmentation error on multiple segments.", minusLoc.isSegmented(), equalTo(true));
        minusLoc.invalidate();
        assertThat("Location did not invalidate.", minusLoc.isValid(), equalTo(false));
        minusLoc.setRight(260);
        Collection<Region> minusRegions = minusLoc.getRegions();
        for (Region region : minusRegions) {
            int right = region.getRight();
            assertThat("Invalid region extends to " + right + " past 260.", right <= 260, equalTo(true));
        }
        Location cloneLoc = (Location) minusLoc.clone();
        assertThat("Clone contig does not match.", cloneLoc.getContigId(), equalTo(minusLoc.getContigId()));
        assertThat("Clone direction does not match.", cloneLoc.getDir(), equalTo(minusLoc.getDir()));
        assertThat("Clone left does not match.", cloneLoc.getLeft(), equalTo(minusLoc.getLeft()));
        assertThat("Clone length does not match.", cloneLoc.getLength(), equalTo(minusLoc.getLength()));
        assertThat("Segmentation error on clone.", cloneLoc.isSegmented(), equalTo(true));
        assertThat("Validation error on clone.", cloneLoc.isValid(), equalTo(false));
        Collection<Region> cloneRegions = cloneLoc.getRegions();
        for (Region region :minusRegions) {
            assertThat("Region " + region + " not found in clone.", region.containedIn(cloneRegions), equalTo(true));
        }
    }

    /**
     * Test of simple locations
     */
    @Test
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
    @Test
    public void testLocations() {
        Region region1 = new Region(1000, 2000);
        Region region2 = new Region(1000, 2000);
        Region region3 = new Region(2000, 3000);
        assertThat("region1 not equal to region 2.", region1.equals(region2), equalTo(true));
        assertThat("region1 equal to region 3.", region1.equals(region3), equalTo(false));
        assertThat("Region equals is not commutative.", region2.equals(region1), equalTo(true));
        assertThat("Equal regions have different hash codes.", region2.hashCode(), equalTo(region1.hashCode()));
        assertThat(region1.getBegin("+"), equalTo(1000));
        assertThat(region1.getBegin("-"), equalTo(2000));
        assertThat(region1.getLength(), equalTo(1001));
        Location loc1 = Location.create("myContig", "+", 1000, 1999);
        Location loc2 = Location.create("myContig", "-", 1100, 1199);
        Location loc3 = Location.create("myContig", "-", 1150, 1249);
        Location loc4 = Location.create("yourContig", "-", 1150, 1249);
        assertThat("loc1 is not less than loc2.", loc1.compareTo(loc2) < 0, equalTo(true));
        assertThat("loc2 is not less than loc3.", loc2.compareTo(loc3) < 0, equalTo(true));
        assertThat("loc1 does not contain loc2.", loc1.contains(loc2), equalTo(true));
        assertThat("loc2 contains loc3.", loc2.contains(loc3), equalTo(false));
        assertThat("loc3 contains loc2.", loc3.contains(loc2), equalTo(false));
        assertThat("loc1 contains loc4.", loc1.contains(loc4), equalTo(false));
        assertThat(loc1.offsetPoint(100), equalTo(1100));
        assertThat(loc1.offsetPoint(-100), equalTo(900));
        assertThat(loc4.offsetPoint(100), equalTo(1149));
        assertThat(loc4.offsetPoint(-100), equalTo(1349));
        Location loc5 = Location.create("myContig", "+",  1000, 2000, 3000, 4000);
        Location loc6 = Location.create("myContig", "+", 3000, 4000, 1000, 2000);
        Location loc7 = Location.create("yourContig", "+", 1000, 2000, 3000, 4000);
        Location loc8 = Location.create("myContig", "-", 1000, 2000, 3000, 4000);
        Location loc9 = Location.create("myContig",  "+",  1000, 1999, 3000, 4000);
        assertThat("loc5 not equal to loc6.", loc5.equals(loc6), equalTo(true));
        assertThat("Equal locations have different hash codes.", loc6.hashCode(), equalTo(loc5.hashCode()));
        assertThat("Different contigs compare equal.", loc5.equals(loc7), equalTo(false));
        assertThat("Different strands compare equal.", loc5.equals(loc8), equalTo(false));
        assertThat("Different region counts compare equal.", loc5.equals(loc1), equalTo(false));
        assertThat("Different region extents compare equal.", loc5.equals(loc9), equalTo(false));
        assertThat(loc5.toSeedString(), equalTo("myContig_1000_2000,myContig_3000_4000"));
        assertThat(loc8.toSeedString(), equalTo("myContig_2000_1000,myContig_4000_3000"));
        Location[] locArray = new Location[] { loc1, loc2, loc3, loc4, loc5, loc6, loc7, loc8, loc9 };
        for (Location loc : locArray) {
            String locString = loc.toString();
            Location locRecurse = Location.fromString(locString);
            assertThat(locString, locRecurse.getContigId(), equalTo(loc.getContigId()));
            assertThat(locString, locRecurse.getDir(), equalTo(loc.getDir()));
            assertThat(locString, locRecurse.getLeft(), equalTo(loc.getLeft()));
            assertThat(locString, locRecurse.getRight(), equalTo(loc.getRight()));
            assertThat(locString, locRecurse.getLength(), equalTo(loc.getLength()));
            assertThat(locString, locRecurse.equals(loc), equalTo(true));
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
    @Test
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
    @Test
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

    @Test
    public void testDistance() {
        Location t1 = Location.create("myContig", "+", 10, 20);
        Location t2 = Location.create("myContig", "-", 25, 45);
        assertThat("Wrong initial distance.", t1.distance(t2), equalTo(4));
        assertThat("Distance not commutative", t2.distance(t1), equalTo(4));
        t1.setRight(24);
        assertThat("Distance wrong when left adjacent.", t1.distance(t2), equalTo(0));
        t1.setRight(25);
        assertThat("Distance wrong at overlap left edge.", t1.distance(t2), equalTo(-1));
        t1.setRight(100);
        assertThat("Distance wrong when contained.", t1.distance(t2), equalTo(-1));
        t1.setLeft(45);
        assertThat("Distance wrong at overlap right edge.", t1.distance(t2), equalTo(-1));
        t1.setLeft(46);
        assertThat("Distance wrong at right adjacent.", t1.distance(t2), equalTo(0));
        t1.setLeft(51);
        assertThat("Distance wrong at end.", t1.distance(t2), equalTo(5));
        Feature f1 = myGto.getFeature("fig|1313.7001.peg.841");
        Feature f2 = myGto.getFeature("fig|1313.7001.peg.847");
        assertThat("Feature distance not equal location distance.", f1.distance(f2),
                equalTo(f1.getLocation().distance(f2.getLocation())));
        Feature f3 = myGto.getFeature("fig|1313.7001.peg.1113");
        assertThat("Contig mismatch not caught.", f1.distance(f3), equalTo(Integer.MAX_VALUE));
    }

    @Test
    public void testUpstream() {
        Location t1 = Location.create("myContig", "+", 400, 500);
        Location t2 = Location.create("otherContig", "+", 200, 300);
        assertThat(t1.upstreamDistance(t2), equalTo(Integer.MAX_VALUE));
        assertThat(t1.isUpstream(t2), equalTo(false));
        assertThat(t2.isUpstream(t1), equalTo(false));
        t2 = Location.create("myContig", "-", 200, 300);
        assertThat(t1.upstreamDistance(t2), equalTo(Integer.MAX_VALUE));
        assertThat(t1.isUpstream(t2), equalTo(false));
        assertThat(t2.isUpstream(t1), equalTo(false));
        t2 = Location.create("myContig", "+", 200, 300);
        assertThat(t1.upstreamDistance(t2), equalTo(99));
        assertThat(t2.upstreamDistance(t1), equalTo(Integer.MAX_VALUE));
        assertThat(t1.isUpstream(t2), equalTo(false));
        assertThat(t2.isUpstream(t1), equalTo(true));
        t2 = Location.create("myContig", "+", 600, 700);
        assertThat(t1.upstreamDistance(t2), equalTo(Integer.MAX_VALUE));
        t1 = Location.create("myContig", "-", 600, 700);
        t2 = Location.create("myContig", "-", 1000, 1100);
        assertThat(t1.upstreamDistance(t2), equalTo(299));
        assertThat(t2.upstreamDistance(t1), equalTo(Integer.MAX_VALUE));
        assertThat(t1.isUpstream(t2), equalTo(false));
        assertThat(t2.isUpstream(t1), equalTo(true));
        t2 = t1.upstream(50);
        assertThat(t1.upstreamDistance(t2), equalTo(0));
        assertThat(t2.getLength(), equalTo(50));
        assertThat(t2.getDir(), equalTo(t1.getDir()));
        assertThat(t2.getContigId(), equalTo(t1.getContigId()));
        t2 = t1.downstream(50);
        assertThat(t1.getEnd() - 1, equalTo(t2.getBegin()));
        assertThat(t2.getLength(), equalTo(50));
        assertThat(t2.getDir(), equalTo(t1.getDir()));
        assertThat(t2.getContigId(), equalTo(t1.getContigId()));
        t1 = Location.create("otherContig", "+", 200, 300);
        t2 = t1.upstream(50);
        assertThat(t1.upstreamDistance(t2), equalTo(0));
        assertThat(t2.getLength(), equalTo(50));
        assertThat(t2.getDir(), equalTo(t1.getDir()));
        assertThat(t2.getContigId(), equalTo(t1.getContigId()));
        assertThat(t1.isUpstream(t2), equalTo(false));
        assertThat(t2.isUpstream(t1), equalTo(true));
        t2 = t1.downstream(50);
        assertThat(t1.getEnd() + 1, equalTo(t2.getBegin()));
        assertThat(t2.getLength(), equalTo(50));
        assertThat(t2.getDir(), equalTo(t1.getDir()));
        assertThat(t2.getContigId(), equalTo(t1.getContigId()));
    }

    @Test
    public void testFeatureUpstream() {
        Feature feat = myGto.getFeature("fig|1313.7001.peg.1600");
        Feature feat2 = myGto.getFeature("fig|1313.7001.peg.1631");
        assertThat(feat.isUpstream(feat2), equalTo(true));
        assertThat(feat2.isUpstream(feat), equalTo(false));
        feat = myGto.getFeature("fig|1313.7001.peg.1651");
        assertThat(feat.isUpstream(feat2), equalTo(false));
        assertThat(feat2.isUpstream(feat), equalTo(false));
        feat2 = myGto.getFeature("fig|1313.7001.peg.1605");
        assertThat(feat.isUpstream(feat2), equalTo(true));
        assertThat(feat2.isUpstream(feat), equalTo(false));
    }

    @Test
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

    @Test
    public void testDnaEdges() {
        Location loc1 = Location.create("1313.7001.con.0002", 5, 10);
        Location loc2 = loc1.upstream(10);
        String dna = myGto.getDna(loc2);
        assertThat(dna, equalTo("aaag"));
        loc1 = Location.create("1313.7001.con.0026", 2092, 2080);
        loc2 = loc1.upstream(20);
        dna = myGto.getDna(loc2);
        assertThat(dna, equalTo("aaggtacat"));
    }

    @Test
    public void testReversal() {
        Location floc = Location.create("contig1", "+", 1000, 1099, 1200, 1299);
        assertThat(floc.getLeft(), equalTo(1000));
        assertThat(floc.getBegin(), equalTo(1000));
        assertThat(floc.getRight(), equalTo(1299));
        assertThat(floc.getEnd(), equalTo(1299));
        assertThat(floc.getRegionLength(), equalTo(200));
        assertThat(floc.getLength(), equalTo(300));
        Location bloc = floc.reverse();
        assertThat(floc.getLeft(), equalTo(1000));
        assertThat(floc.getBegin(), equalTo(1000));
        assertThat(floc.getRight(), equalTo(1299));
        assertThat(floc.getEnd(), equalTo(1299));
        assertThat(floc.getRegionLength(), equalTo(200));
        assertThat(floc.getLength(), equalTo(300));
        assertThat(bloc.getLeft(), equalTo(1000));
        assertThat(bloc.getBegin(), equalTo(1299));
        assertThat(bloc.getRight(), equalTo(1299));
        assertThat(bloc.getEnd(), equalTo(1000));
        assertThat(bloc.getRegionLength(), equalTo(200));
        assertThat(bloc.getLength(), equalTo(300));
        Location floc2 = bloc.reverse();
        assertThat(floc2, equalTo(floc));
    }

}
