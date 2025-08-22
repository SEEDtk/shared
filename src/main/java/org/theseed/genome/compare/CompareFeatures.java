/**
 *
 */
package org.theseed.genome.compare;

import java.security.NoSuchAlgorithmException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.theseed.genome.Feature;

/**
 * This class contains utilities for genome comparison.
 *
 * @author Bruce Parrello
 *
 */
public class CompareFeatures extends CompareORFs {

    // FIELDS

    /** ORFS annotated in the old genome but not the new one */
    private SortedSet<Feature> oldOnly;
    /** ORFs annotated in the new genome but not the old one */
    private SortedSet<Feature> newOnly;
    /** number of ORFs identical in both genomes */
    private int identical;
    /** number of ORFs that are commonly annotated but have different functions */
    private int differentFunctions;
    /** number of ORFs that have the same function, but are longer in the new genome */
    private int longer;
    /** number of ORFs that have the same function, but are shorted in the new genome */
    private int shorter;


    /** Initialize a comparison processor
     *
     * @throws NoSuchAlgorithmException
     */
    public CompareFeatures() throws NoSuchAlgorithmException {
        super();
        this.orfSorter = new OrfSorter();
    }

    /**
     * Process features that occupy the same ORF in both genomes.
     *
     * @param oldFeature	feature in the old genome
     * @param newFeature	feature in the new genome
     */
    @Override
    protected void both(Feature oldFeature, Feature newFeature) {
        // Both features match.  Check the annotations.
        if (! newFeature.getFunction().contentEquals(oldFeature.getFunction())) {
            differentFunctions++;
        } else {
            // Annotations match.  Check the lengths.
            int comp = newFeature.getLocation().getLength() - oldFeature.getLocation().getLength();
            if (comp < 0) {
                this.shorter++;
            } else if (comp > 0) {
                this.longer++;
            } else {
                this.identical++;
            }
        }
    }

    /**
     * Process a feature that occupies the ORF in the new genome that corresponds to an empty
     * ORF in the old genome.
     *
     * @param newFeature	feature in the new genome
     */
    @Override
    protected void newOnly(Feature newFeature) {
        this.newOnly.add(newFeature);
    }

    /**
     * Process a feature that occupies the ORF in the old genome that corresponds to an empty
     * ORF in the new genome.
     *
     * @param oldFeature	feature in the old genome
     */
    @Override
    protected void oldOnly(Feature oldFeature) {
        this.oldOnly.add(oldFeature);
    }

    /**
     * Initialize the data structures for a comparison.
     */
    @Override
    protected void initCompareData() {
        this.oldOnly = new TreeSet<>();
        this.newOnly = new TreeSet<>();
        this.identical = 0;
        this.differentFunctions = 0;
        this.longer = 0;
        this.shorter = 0;
    }

    /**
     * @return the number of ORFs only annotated in the old genome
     */
    public int getOldOnlyCount() {
        return oldOnly.size();
    }

    /**
     * @return the number of ORFs only annotated in the new genome
     */
    public int getNewOnlyCount() {
        return newOnly.size();
    }

    /**
     * @return the features from ORFs only annotated in the old genome
     */
    public SortedSet<Feature> getOldOnly() {
        return oldOnly;
    }

    /**
     * @return the features from ORFs only annotated in the new genome
     */
    public SortedSet<Feature> getNewOnly() {
        return newOnly;
    }

    /**
     * @return the number of ORFs identically annotated in both genomes
     */
    public int getIdentical() {
        return identical;
    }

    /**
     * @return the number of ORFs annotated in both genomes, but with different functions
     */
    public int getDifferentFunctions() {
        return differentFunctions;
    }

    /**
     * @return the number of ORFs annotated identically, but are longer in the new genome
     */
    public int getLonger() {
        return longer;
    }

    /**
     * @return the number of ORFs annotated identically, but are shorter in the new genome
     */
    public int getShorter() {
        return shorter;
    }

    /**
     * @return the number of ORFs annotated in both genomes
     */
    public int getCommon() {
        return (identical + differentFunctions + longer + shorter);
    }

}
