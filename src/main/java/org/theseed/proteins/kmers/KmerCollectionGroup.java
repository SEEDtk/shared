/**
 *
 */
package org.theseed.proteins.kmers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.theseed.sequence.ProteinKmers;
import org.theseed.sequence.Sequence;
import org.theseed.sequence.SequenceKmers;

/**
 * This class loads a list of sequences into a named sequence collection.  The sequences can then be used to
 * determine how far a sequence is from a specific collection.
 *
 * @author Bruce Parrello
 *
 */
public class KmerCollectionGroup {

    // FIELDS
    /** map of list names to sequence lists */
    Map<String, Collection<ProteinKmers>> sequenceLists;

    /**
     * Create new, blank sequence-list collection
     */
    public KmerCollectionGroup() {
        this.sequenceLists = new HashMap<>();
    }

    /**
     * Add a sequence to one of the lists.
     *
     * @param seq		sequence to add
     * @param label		name to give to the sequence's list
     */
    public void addSequence(Sequence seq, String label) {
        String seqProt = seq.getSequence();
        addSequence(seqProt, label);
    }

    /**
     * Add a sequence to one of the lists.
     *
     * @param seqProt	sequence to add
     * @param label		name to give to the sequence's list
     */
    public void addSequence(String seqProt, String label) {
        // Create the protein kmers object.
        ProteinKmers kmers = new ProteinKmers(seqProt);
        // Find the list for this label.
        Collection<ProteinKmers> target = this.sequenceLists.get(label);
        if (target == null ) {
            target = new ArrayList<ProteinKmers>();
            this.sequenceLists.put(label, target);
        }
        // Add this sequence to the list.
        target.add(kmers);
    }

    /**
     * @return the distance from a sequence to a label's collection.
     *
     * @param seq		sequence to test
     * @param label		list against which to test it
     */
    public double getDistance(Sequence seq, String label) {
        String seqProt = seq.getSequence();
        double retVal = getDistance(seqProt, label);
        // Return the best distance.
        return retVal;
    }

    /**
     * @return the distance from a sequence to a label's collection.
     *
     * @param seqProt		sequence to test
     * @param label		list against which to test it
     */
    public double getDistance(String seqProt, String label) {
        // Start with the max possible distance.
        double retVal = 1.0;
        // Find the list for this label.
        Collection<ProteinKmers> target = this.sequenceLists.get(label);
        if (target != null) {
            // Create kmers for the sequence.
            ProteinKmers kmers = new ProteinKmers(seqProt);
            // Search for the best distance.
            for (SequenceKmers protein : target) {
                double dist = protein.distance(kmers);
                if (dist < retVal) retVal = dist;
            }
        }
        return retVal;
    }

    /**
     * Result class for returning the best group.
     */
    public static class Result {
        private double distance;
        private String group;

        private Result() {
            this.distance = 1.0;
            this.group = null;
        }

        /**
         * @return the best distance
         */
        public double getDistance() {
            return distance;
        }

        /**
         * @return the best group
         */
        public String getGroup() {
            return group;
        }

        /**
         * Update this object if we have a better result.
         *
         * @param grp	relevant group
         * @param dist	new distance
         */
        protected void merge(String grp, double dist) {
            if (dist < this.distance) {
                this.group = grp;
                this.distance = dist;
            }
        }

    }

    /**
     * Find the group closest to the specified sequence and return the name and distance.
     *
     * @param seq	sequence to check
     *
     * @return a Result object containing the group name and distance
     */
    public Result getBest(Sequence seq) {
        String seqProt = seq.getSequence();
        return getBest(seqProt);
    }

    /**
     * Find the group closest to the specified sequence and return the name and distance.
     *
     * @param seqProt	sequence to check
     *
     * @return a Result object containing the group name and distance
     */
    public Result getBest(String seqProt) {
        Result retVal = new Result();
        for (String grp : this.sequenceLists.keySet()) {
            double distance = this.getDistance(seqProt, grp);
            retVal.merge(grp, distance);
        }
        return retVal;
    }

    /**
     * @return the number of groups in this object
     */
    public int size() {
        return this.sequenceLists.size();
    }

    /**
     * @return the group IDs for this object
     */
    public Collection<String> getKeys() {
        return this.sequenceLists.keySet();
    }

}
