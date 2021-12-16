/**
 *
 */
package org.theseed.rna;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Feature;
import org.theseed.io.TabbedLineReader;
import org.theseed.reports.NaturalSort;
;

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
    /** expression percent required for a good sample */
    public static double MIN_EXPRESSED = 50.0;

    /**
     * This class is a simple filter for finding RNA databases in a directory.
     */
    public static class FileFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return StringUtils.endsWith(name, ".tpm.ser");
        }

    }

    /**
     * This sub-object represents the data for a single sample.
     */
    public static class JobData implements Serializable {
        private static final long serialVersionUID = 3865134821481413921L;
        private String name;
        private double production;
        private double opticalDensity;
        private String oldName;
        private boolean suspicious;
        private double quality;
        private int readCount;
        private long baseCount;
        private LocalDate creation;
        private double expressPercent;

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
         */
        public double getExpressedPercent() {
            return this.expressPercent;
        }

        /**
         * @return TRUE if this is a good sample, else FALSE
         */
        public boolean isGood() {
            return ! this.isSuspicious() && this.getExpressedPercent() >= MIN_EXPRESSED;
        }

        /**
         * Update additional quality metrics for this job.  Currently, this is
         * just the expressed percent.  This method is called before the database is stored.
         *
         * @param data	RNA database
         */
        protected void updateQuality(RnaData data) {
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
            this.expressPercent = 0.0;
            if (total > 0)
                this.expressPercent = (count * 100.0) / total;
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
            init();
        }

        /**
         * Initialize the data structures of this object.
         */
        private void init() {
            // Clear the weights.
            this.weights = new Weight[RnaData.this.jobs.size()];
        }

        /**
         * Create a row for a feature copied from another RNA database.
         *
         * @param row		source RNA database.
         */
        public Row(Row row) {
            this.feat = row.feat;
            this.neighbor = row.neighbor;
            this.init();
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

        /**
         * @return TRUE if the weight at the specified index is good
         *
         * @param iCol	column of interest
         */
        public boolean isGood(int iCol) {
            Weight w = this.getWeight(iCol);
            return (w != null && w.exactHit && Double.isFinite(w.weight));
        }

        /**
         * @return an iterable for the good weights
         */
        public Iterable<Weight> goodWeights() {
            return RnaData.this.new GoodWeights(this);
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
     * This nested class iterates over the good weights in a row.
     */
    public class GoodWeights implements Iterable<Weight> {

        /** collection of good weights */
        private Collection<Weight> weights;

        /**
         * Create a simple good weights list.
         *
         * @param row	row whose weights are desired
         */
        public GoodWeights(RnaData.Row row) {
            this.setup(row, false);
        }

        /**
         * Create a good weights list with optional filtering.
         *
         * @param row	row whose weights are desired
         * @param pure	if TRUE, bad samples will be skipped
         */
        public GoodWeights(RnaData.Row row, boolean pure) {
            setup(row, pure);
        }

        /**
         * Initialize a good weights list.
         *
         * @param row	row containing the weights
         * @param pure	TRUE if only good samples should be included
         */
        protected void setup(RnaData.Row row, boolean pure) {
            this.weights = new ArrayList<Weight>(row.size());
            for (int i = 0; i < row.size(); i++) {
                if (! pure || RnaData.this.getJob(i).isGood()) {
                    Weight w = row.getWeight(i);
                    if (w != null && w.exactHit && Double.isFinite(w.weight))
                        this.weights.add(w);
                }
            }
        }

        @Override
        public Iterator<Weight> iterator() {
            return this.weights.iterator();
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
     * Create a new RNA data repository with the same features as this one but only a subset of
     * the samples.  No cloning is done.  A modification to this database will have unpredictable
     * effects on the new database.
     */
    public RnaData getSubset(Collection<String> samples) {
        RnaData retVal = new RnaData();
        // This array maps from the new database's job indices to our indices.
        int[] idxMap = new int[samples.size()];
        // First, we must add the jobs.
        int pos = 0;
        for (String sample : samples) {
            int jobIdx = this.getColIdx(sample);
            idxMap[pos] = jobIdx;
            JobData sampleJob = this.getJob(jobIdx);
            retVal.jobs.add(sampleJob);
            retVal.colMap.put(sample, pos);
            pos++;
        }
        // The job array is all filled in.  Now we can add the features.  We use the index map
        // to guide us.
        for (Row row : this.getRows()) {
            Row newRow = retVal.new Row(row);
            String fid = row.getFeat().getId();
            for (int i = 0; i < idxMap.length; i++)
                newRow.weights[i] = row.getWeight(idxMap[i]);
            retVal.rowMap.put(fid, newRow);
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
     * @param operon			operon name
     */
    public void storeRegulonData(String fid, int atomicRegulon, String modulons, String operon) {
        Row fRow = this.getRow(fid);
        if (fRow == null)
            log.warn("Invalid feature ID {} specified for regulon data.", fid);
        else {
            RnaFeatureData fData = fRow.getFeat();
            fData.setAtomicRegulon(atomicRegulon);
            fData.setiModulons(modulons);
            fData.setOperon(operon);
        }
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

    /**
     * Update the quality data for all the jobs.
     */
    public void updateQuality() {
        for (JobData job : this.jobs)
            job.updateQuality(this);
    }

    /**
     * @return the sample at the specified array index
     *
     * @param jobIdx	index of desired job
     */
    public JobData getJob(int jobIdx) {
        return this.jobs.get(jobIdx);
    }

    /**
     * @return the cluster map from the specified file
     *
     * @param clusterFile	2-column tab-delimited file with the cluster ID in column 1 and the member name in column 2
     *
     * @throws IOException
     */
    public static Map<String, Set<String>> readClusterMap(File clusterFile) throws IOException {
        Map<String, Set<String>> retVal;
        try (TabbedLineReader inStream = new TabbedLineReader(clusterFile)) {
            retVal = new HashMap<String, Set<String>>(100);
            int count = 0;
            for (TabbedLineReader.Line line : inStream) {
                String cluster = line.get(0);
                Set<String> list = retVal.computeIfAbsent(cluster, k -> new TreeSet<String>());
                list.add(line.get(1));
                count++;
            }
            log.info("{} samples found in {} clusters.", count, retVal.size());
        }
        return retVal;
    }

    /**
     * @return the jobs in this database
     */
    public Collection<JobData> getJobs() {
        return this.jobs;
    }

}
