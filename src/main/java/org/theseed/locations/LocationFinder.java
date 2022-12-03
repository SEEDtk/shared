/**
 *
 */
package org.theseed.locations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.theseed.genome.Contig;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;

/**
 * This class creates a data structure that converts locations in a genome to feature IDs.
 *
 * Each [contigID, strand] pair is associated with a sorted list of feature regions.  A multi-region feature
 * will have multiple locations in the list.  The locations are sorted by left position.  We currently
 * search sequentially until we find a region whose left position is past the right position of the
 * search location.  Note this works for both strands, even though the left position is the end on the minus
 * strand and the start on the plus strand.  We insist that each feature associated with a region completely
 * cover it, and we split the regions so they are non-overlapping.
 *
 * @author Bruce Parrello
 *
 */
public class LocationFinder {

    // FIELDS
    /** map of contig/strand pairs to region lists */
    private Map<String, List<ContigRegion>> contigMap;
    /** ID of the source genome */
    private String genomeId;

    /**
     * This computes the contig/strand code for a location, which determines which list it belongs in.
     *
     * @param loc	location to scan
     *
     * @return the contig ID followed by the strand code
     */
    public static String getContigStrand(Location loc) {
        return loc.getContigId() + loc.getStrand();
    }

    /**
     * This object describes a region on a contig strand and the set of features that overlap it.
     */
    private static class ContigRegion {

        /** left position of the region (1-based) */
        private int left;
        /** position past the right end of the region (1-based) */
        private int right;
        /** IDs of the features in this region */
        private Set<Feature> feats;

        /**
         * Construct a specific contig region.
         *
         * @param left0		left position of the region
         * @param right0	position past right of the region
         */
        protected ContigRegion(int left0, int right0) {
            this.left = left0;
            this.right = right0;
            this.feats = new TreeSet<Feature>();
        }

        /**
         * Split a contig region and return the second half.
         *
         * @param newRight	new right portion of this region
         */
        protected ContigRegion split(int newRight) {
            ContigRegion retVal = new ContigRegion(newRight, this.right);
            this.right = newRight;
            return retVal;
        }

        /**
         * @return the left position of this region
         */
        protected int getLeft() {
            return this.left;
        }

        /**
         * @return the position past the right edge of this region
         */
        protected int getRight() {
            return this.right;
        }

        /**
         * @return the set of features in this region
         */
        protected Set<Feature> getFeats() {
            return this.feats;
        }

        /**
         * Add the specified feature to this region's feature set.
         *
         * @param feat	feature to add
         */
        public void addFeat(Feature feat) {
            this.feats.add(feat);
        }

    }

    /**
     * Create the location finder for a genome.
     *
     * @param genome	genome of interest
     */
    public LocationFinder(Genome genome) {
        // Save the genome ID.
        this.genomeId = genome.getId();
        // Initialize the map.
        this.contigMap = new HashMap<String, List<ContigRegion>>(genome.getContigCount() * 8 / 3 + 1);
        // Loop through all the features in the genome.
        for (Feature feat : genome.getFeatures()) {
            // Get this feature's location.
            Location loc = feat.getLocation();
            // Get the contig information.
            String contigID = loc.getContigId();
            Contig contig = genome.getContig(contigID);
            // Only proceed if the contig ID is valid.  Features with bad contig IDs are always bad imports.
            if (contig != null) {
                // Get the list for this strand.
                List<ContigRegion> regionList = this.contigMap.computeIfAbsent(getContigStrand(loc),
                        x -> this.createContigList(contig));
                // Now we need to incorporate this feature's regions into the strand list.
                for (Region r : loc.getRegions())
                    this.addRegion(regionList, r, feat);
            }
        }
    }

    /**
     * Create a region list for the specified contig.  It will be pre-allocated to a good estimate of
     * the region's segment count, with a single-region descriptor already in it.
     *
     * @param contig	contig of interest
     *
     * @return a contig-region list that can be used to build the location finder for the contig
     */
    private List<ContigRegion> createContigList(Contig contig) {
        int len = contig.length();
        List<ContigRegion> retVal = new ArrayList<ContigRegion>(len / 1000 + 10);
        retVal.add(new ContigRegion(1, len + 1));
        return retVal;
    }

    /**
     * Add a region to the region list.  It will be split out from any existing contig regions, and the
     * specified feature added to the feature set.
     *
     * @param regionList	region list to update
     * @param region		region containing the specified feature
     * @param feat			specified feature
     */
    private void addRegion(List<ContigRegion> regionList, Region r, Feature feat) {
        // Get the feature region's start and end points.
        int start = r.getLeft();
        int end = r.getRight() + 1;
        // Find where this region's start position lies.
        int idx = this.findContigRegion(regionList, start);
        // Only proceed if the start position is on the contig.  We are building a structure to search
        // for existing regions only.
        if (idx >= 0) {
            ContigRegion curr;
            while (idx < regionList.size() && (curr = regionList.get(idx)).getLeft() < end) {
                // Here the region of interest from the feature overlaps this contig region.
                // If the feature region starts in the middle, we need to split the unused part
                // from the contig region and place ourselves after the new, smaller region.
                if (start > curr.getLeft()) {
                    ContigRegion next = curr.split(start);
                    idx++;
                    regionList.add(idx, next);
                    curr = next;
                }
                // Now the current contig region starts at or to the left of the feature region.
                // If the feature region ends in the middle, we need to split a new region and
                // place it after this one.
                if (end < curr.getRight()) {
                    ContigRegion next = curr.split(end);
                    regionList.add(idx + 1, next);
                }
                // Add the feature to the current region.
                curr.addFeat(feat);
                // Move to the next region.
                idx++;
            }
        }
    }

    /**
     * Locate the specified contig position in the specified region list.
     *
     * @param regionList	region list to search
     * @param pos			position to find (1-based)
     *
     * @return the position of the first region in the list that contains the specified start
     * 		   point, or -1 if there is none
     */
    private int findContigRegion(List<ContigRegion> regionList, int pos) {
        final int n = regionList.size();
        int retVal = 0;
        while (retVal < n && regionList.get(retVal).getRight() <= pos) retVal++;
        if (retVal >= n)
            retVal = -1;
        return retVal;
    }

    /**
     * Return the set of features that overlap the specified location
     *
     * @param loc		location of interest
     *
     * @return the set of features (possible empty) that overlaps the region
     */
    public Set<Feature> getFeatures(Location loc) {
        Set<Feature> retVal = new TreeSet<Feature>();
        // Get the region list for this location's strand.
        List<ContigRegion> regionList = this.contigMap.get(getContigStrand(loc));
        // If we don't find the strand, there are no features on it.
        if (regionList != null) {
            // Do a separate search for each region of the location.
            for (Region r : loc.getRegions()) {
                // Get the first contig region for this feature region.
                int start = r.getLeft();
                int end = r.getRight() + 1;
                int idx = this.findContigRegion(regionList, start);
                // Only proceed if this region is in the contig.
                if (idx >= 0) {
                    ContigRegion curr;
                    while (idx < regionList.size() && (curr = regionList.get(idx)).getLeft() < end) {
                        // All all of this region's features to the return set.
                        retVal.addAll(curr.getFeats());
                        // Move to the next region.
                        idx++;
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * @return the ID of the source genome
     */
    public String getGenomeId() {
        return this.genomeId;
    }

}
