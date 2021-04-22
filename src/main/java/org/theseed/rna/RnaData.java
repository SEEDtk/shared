/**
 *
 */
package org.theseed.rna;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Feature;
import org.theseed.reports.NaturalSort;

/**
 * This object contains the data for all the samples processed during an FPKM summary.  For each named job, it contains the
 * threonine production data.  For each feature, it contains all the weights.
 *
 * @author Bruce Parrello
 *
 */
public class RnaData implements Iterable<RnaData.Row>, Serializable {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(RnaData.class);
    /** object version ID */
    private static final long serialVersionUID = 3381600661613511530L;
    /** list of sample descriptors */
    List<JobData> jobs;
    /** map of features to data rows */
    private Map<String, Row> rowMap;
    /** map of job names to column indices */
    private Map<String, Integer> colMap;
    /** scale value for normalizing weights */
    private static double SCALE_FACTOR = 1000000.0;
    /** feature ID sorter */
    private static final Comparator<String> SORTER = new NaturalSort();

    /**
     * This sub-object represents the data for a single sample.
     */
    public static class JobData implements Serializable {
        private static final long serialVersionUID = 3865134821481413920L;
        private String name;
        private double production;
        private double opticalDensity;
        private String oldName;
        private boolean suspicious;
        private double quality;
        private int readCount;
        private long baseCount;
        private LocalDate creation;

        /**
         * Construct a sample-data object.
         *
         * @param name				sample name
         * @param production		threonine production level (g/l)
         * @param opticalDensity	optical density (nm600)
         * @param oldName			original sample name
         * @param suspicious		TRUE if the quality is suspect
         */
        private JobData(String name, double production, double opticalDensity, String oldName, boolean suspicious) {
            this.name = name;
            this.production = production;
            this.opticalDensity = opticalDensity;
            this.oldName = oldName;
            this.suspicious = suspicious;
            this.quality = 0.0;
            this.readCount = 0;
            this.baseCount = 0;
            this.creation = LocalDate.now();
        }

        /**
         * @return the name
         */
        public String getName() {
            return this.name;
        }

        /**
         * @return the threonine production level (or NaN if it is unknown)
         */
        public double getProduction() {
            return this.production;
        }

        /**
         * @return the optical density (or NaN if it is unknown)
         */
        public double getOpticalDensity() {
            return this.opticalDensity;
        }

        /**
         * @return the string for one of our numbers
         */
        public String stringOf(double value) {
            String retVal = "";
            if (! Double.isNaN(value))
                retVal = String.format("%8.4f", value);
            return retVal;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof JobData)) {
                return false;
            }
            JobData other = (JobData) obj;
            if (this.name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!this.name.equals(other.name)) {
                return false;
            }
            return true;
        }

        /**
         * @return the old sample name
         */
        public String getOldName() {
            return this.oldName;
        }

        /**
         * @return TRUE if this sample is of suspicious quality
         */
        public boolean isSuspicious() {
            return this.suspicious;
        }

        /**
         * @return the percent reads in this sample with a map quality >= 30%
         */
        public double getQuality() {
            return this.quality;
        }

        /**
         * Specify the read quality percentage.
         *
         * @param quality 	the percent reads with a map quality >= 30%
         */
        public void setQuality(double quality) {
            this.quality = quality;
        }

        /**
         * @return the number of reads in this sample
         */
        public int getReadCount() {
            return this.readCount;
        }

        /**
         * Specify the number of reads in this sample.
         *
         * @param readCount 	the number of reads
         */
        public void setReadCount(int readCount) {
            this.readCount = readCount;
        }

        /**
         * @return the number of base pairs in this sample
         */
        public long getBaseCount() {
            return this.baseCount;
        }

        /**
         * Specify the number of base pairs in this sample
         *
         * @param baseCount 	the number of base pairs
         */
        public void setBaseCount(long baseCount) {
            this.baseCount = baseCount;
        }

        /**
         * @return the processing date of the sample
         */
        public LocalDate getProcessingDate() {
            return this.creation;
        }

        /**
         * Specify the processing date of this sample.
         *
         * @param creation 	the creation date to set
         */
        public void setProcessingDate(LocalDate creation) {
            this.creation = creation;
        }

        /**
         * @return the mean length of a read in this sample
         */
        public double getMeanReadLen() {
            double retVal = 0.0;
            if (this.readCount > 0)
                retVal = ((double) this.baseCount) / this.readCount;
            return retVal;
        }

        /**
         * @return the coverage depth of this sample
         *
         * @param gLen	number of base pairs in the relevant genome
         */
        public double getCoverage(int gLen) {
            return (this.baseCount * this.quality) / (gLen * 100.0);
        }

        /**
         * @return the percent of genes with expression values
         *
         * @param data	RNA database
         */
        public double getExpressedPercent(RnaData data) {
            int colIdx = data.getColIdx(this.name);
            int count = 0;
            int total = 0;
            for (RnaData.Row row : data) {
                RnaData.Weight weight = row.getWeight(colIdx);
                if (weight != null && weight.isExactHit()) {
                    double wVal = weight.getWeight();
                    if (Double.isFinite(wVal) && wVal > 0.0)
                        count++;
                }
                total++;
            }
            double retVal = 0.0;
            if (total > 0)
                retVal = (count * 100.0) / total;
            return retVal;
        }

    }

    /**
     * This nested class represents a weight report
     */
    public static class Weight implements Serializable {
        private static final long serialVersionUID = -3059148113478652509L;
        private boolean exactHit;
        private double weight;

        /**
         * Create a weight record.
         *
         * @param exact		TRUE if this is the weight for an exact hit
         * @param wValue	weight of the hit
         */
        private Weight(boolean exact, double wValue) {
            this.exactHit = exact;
            this.weight = wValue;
        }

        /**
         * @return TRUE if this was an exact hit
         */
        public boolean isExactHit() {
            return this.exactHit;
        }

        /**
         * @return the weight of the hit
         */
        public double getWeight() {
            return this.weight;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (this.exactHit ? 1231 : 1237);
            long temp;
            temp = Double.doubleToLongBits(this.weight);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Weight)) {
                return false;
            }
            Weight other = (Weight) obj;
            if (this.exactHit != other.exactHit) {
                return false;
            }
            if (Double.doubleToLongBits(this.weight) != Double.doubleToLongBits(other.weight)) {
                return false;
            }
            return true;
        }

    }

    /**
     * This nested class represents a feature row.
     */
    public class Row implements Serializable, Comparable<Row> {
        /** object version ID */
        private static final long serialVersionUID = 1L;
        /** feature hit */
        private RnaFeatureData feat;
        /** neighbor feature (or NULL) */
        private RnaFeatureData neighbor;
        /** hit list */
        private Weight[] weights;

        /**
         * Create a row for a feature.
         *
         * @param fData		target feature
         * @param neighbor	useful neighbor
         */
        private Row(RnaFeatureData fData, Feature neighbor) {
            this.feat = fData;
            this.neighbor = null;
            if (neighbor != null)
                this.neighbor = new RnaFeatureData(neighbor);
            // Clear the weights.
            this.weights = new Weight[RnaData.this.jobs.size()];
        }

        /**
         * Store a weight for the current column.
         *
         * @param jobName	name of the sample for the current column
         * @param exact		TRUE if the weight is for an exact hit
         * @param wValue	value of the weight
         */
        public void store(String jobName, boolean exact, double wValue) {
            Weight w = new Weight(exact, wValue);
            int idx = RnaData.this.colMap.get(jobName);
            this.weights[idx] = w;
        }

        /**
         * @return the target feature
         */
        public RnaFeatureData getFeat() {
            return this.feat;
        }

        /**
         * @return the neighbor feature
         */
        public RnaFeatureData getNeighbor() {
            return this.neighbor;
        }

        /**
         * @return the weight in the specified column
         *
         * @param iCol	column of interest
         */
        public Weight getWeight(int iCol) {
            return this.weights[iCol];
        }

        /**
         * @return the number of values in the row
         */
        public int size() {
            return this.weights.length;
        }

        @Override
        public int compareTo(Row o) {
            int retVal = this.feat.getLocation().compareTo(o.feat.getLocation());
            if (retVal == 0)
                retVal = SORTER.compare(this.feat.getId(), o.feat.getId());
            return retVal;
        }

    }

    /**
     * Create a new RNA data repository.
     */
    public RnaData() {
        this.jobs = new ArrayList<JobData>();
        this.rowMap = new HashMap<String, Row>();
        this.colMap = new HashMap<String, Integer>();
    }

    /**
     * Load an RNA data repository from a file.
     *
     * @param file	file from which to load the repository
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static RnaData load(File file) throws IOException, ClassNotFoundException {
        RnaData retVal = null;
        try (FileInputStream fStream = new FileInputStream(file)) {
            ObjectInputStream oStream = new ObjectInputStream(fStream);
            retVal = (RnaData) oStream.readObject();
        }
        return retVal;
    }

    /**
     * Add a new sample.
     *
     * @param jobName			sample name
     * @param production		threonine production level
     * @param opticalDensity	optical density
     * @param oldName 			original name of sample
     * @param suspicious		return TRUE if this sample is of suspicious quality
     */
    public JobData addJob(String jobName, double production, double opticalDensity, String oldName, boolean suspicious) {
        // Save the array index for this sample.
        this.colMap.put(jobName, this.jobs.size());
        // Add the sample to the job list.
        JobData retVal = new JobData(jobName, production, opticalDensity, oldName, suspicious);
        this.jobs.add(retVal);
        return retVal;
    }

    @Override
    public Iterator<Row> iterator() {
        return this.rowMap.values().iterator();
    }

    /**
     * @return the list of sample descriptors
     */
    public List<JobData> getSamples() {
        return this.jobs;
    }

    /**
     * @return the number of samples in this repository
     */
    public int size() {
        return this.jobs.size();
    }

    /**
     * @return the number of rows in this repository
     */
    public int rows() {
        return this.rowMap.size();
    }

    /**
     * @return the row object for the specified feature, creating one if none exists
     *
     * @param feat		feature of interest
     * @param neighbor	nearest neighbor
     */
    public Row getRow(Feature feat, Feature neighbor) {
        RnaFeatureData fData = new RnaFeatureData(feat);
        Row retVal = this.rowMap.computeIfAbsent(feat.getId(), f -> new Row(fData, neighbor));
        return retVal;
    }

    /**
     * @return the row object for the specified feature ID, or NULL if none exists
     *
     * @param feat	feature of interest
     */
    public Row getRow(String fid) {
        Row retVal = this.rowMap.get(fid);
        return retVal;
    }

    /**
     * Store the regulon/modulon data for a feature.
     *
     * @param fid				ID of the target feature
     * @param atomicRegulon		atomic regulon number (or 0 for none)
     * @param modulons			comma-delimited iModulon string (empty for none)
     */
    public void storeRegulonData(String fid, int atomicRegulon, String modulons) {
        Row fRow = this.getRow(fid);
        if (fRow == null)
            throw new IllegalArgumentException("Invalid feature ID " + fid + " specified for regulon data.");
        RnaFeatureData fData = fRow.getFeat();
        fData.setAtomicRegulon(atomicRegulon);
        fData.setiModulons(modulons);
    }

    /**
     * @return the collection of feature rows in this database
     */
    public Collection<Row> getRows() {
        return this.rowMap.values();
    }

    /**
     * Save this object to a file.
     *
     * @param file	file into which this object will be stored
     *
     * @throws IOException
     */
    public void save(File file) throws IOException {
        try (FileOutputStream fStream = new FileOutputStream(file)) {
            ObjectOutputStream oStream = new ObjectOutputStream(fStream);
            oStream.writeObject(this);
        }
    }

    /**
     * @return the column index for the specified sample
     *
     * @param sample	name of the sample in question
     */
    public int getColIdx(String sample) {
        Integer retVal = this.colMap.get(sample);
        if (retVal == null) retVal = -1;
        return (int) retVal;
    }

    /**
     * Normalize this RNA data.  This includes deleting all RNA features and scaling the FPKM
     * numbers to TPM values. (TPM = FPKM * 10^6 / SUM(all FPKMs for sample))
     */
    public void normalize() {
        // Each row contains an array of weights.  This array keeps the totals.
        double[] totalWeights = new double[this.size()];
        // Count the RNAs removed.
        int removed = 0;
        // Loop through the rows.  We delete the RNA rows, and sum the rest.
        Iterator<Row> rowIter = this.rowMap.values().iterator();
        while (rowIter.hasNext()) {
            Row row = rowIter.next();
            if (row.feat.getId().contains(".rna.")) {
                // Here we have an RNA that sneaked through the sample filters.
                rowIter.remove();
                removed++;
            } else {
                // This row is being kept, so we accumulate its values.
                for (int i = 0; i < totalWeights.length; i++) {
                    Weight weight = row.getWeight(i);
                    if (weight != null)
                        totalWeights[i] += weight.weight;
                }
            }
        }
        // Now we scale each of the weights.
        for (int i = 0; i < totalWeights.length; i++)
            totalWeights[i] = SCALE_FACTOR / totalWeights[i];
        // Finally, we scale each of the individual weights.
        for (Row row : this) {
            for (int i = 0; i < totalWeights.length; i++) {
                if (totalWeights[i] > 0.0) {
                    Weight weight = row.getWeight(i);
                    if (weight != null)
                        weight.weight *= totalWeights[i];
                }
            }
        }
        log.info("{} RNA features removed during normalization, {} features remaining.", removed, this.rows());
    }

    /**
     * @return the column index of the sample, or NULL if it does not exist
     *
     * @param sample	name of the sample to find
     */
    public Integer findColIdx(String sample) {
        return this.colMap.get(sample);
    }

    /**
     * @return the job descriptor for the named job
     *
     * @param jobName	name of the sample whose job is desired
     */
    public JobData getJob(String jobName) {
        int colIdx = this.findColIdx(jobName);
        return this.jobs.get(colIdx);
    }

    /**
     * @return the baseline value for a row of expression data
     *
     * @param row	row for the feature whose baseline value is desired
     */
    public double getBaseline(Row row) {
        DescriptiveStatistics stats = getStats(row);
        double retVal = ((stats.getPercentile(25) + stats.getPercentile(75)) / 2.0 + stats.getPercentile(50)) / 2.0;
        return retVal;
    }

    /**
     * @return a map of feature IDs to baseline values for this RNA expression database
     */
    public SortedMap<String, Double> getBaselines() {
        SortedMap<String, Double> retVal = new TreeMap<String, Double>(new NaturalSort());
        for (Map.Entry<String, Row> rowEntry : this.rowMap.entrySet()) {
            double baseLine = this.getBaseline(rowEntry.getValue());
            retVal.put(rowEntry.getKey(), baseLine);
        }
        return retVal;
    }

    /**
     * @return a descriptive statistics object for the valid expression values in the specified row
     *
     * @param row	RNA database row for the feature of interest
     */
    public static DescriptiveStatistics getStats(RnaData.Row row) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 0; i < row.size(); i++) {
            Weight w = row.getWeight(i);
            if (w != null && w.isExactHit() && Double.isFinite(w.getWeight()))
                stats.addValue(w.getWeight());
        }
        return stats;
    }

    /**
     * @return the number of rows (features) in this database
     */
    public int height() {
        return this.rowMap.size();
    }
}
