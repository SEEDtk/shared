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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Feature;
import org.theseed.locations.Location;

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
    /** pattern for RNA functions */
    private static final Predicate<String> RNA_PREDICATE = Pattern.compile("ribosomal|[tr]RNA").asPredicate();
    /** scale value for normalizing weights */
    private static double SCALE_FACTOR = 1000000.0;

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

    }

    /**
     * This object contains key data fields for a feature.
     */
    public static class FeatureData implements Comparable<FeatureData>, Serializable {
        private static final long serialVersionUID = 3941353134302649698L;
        private String id;
        private String function;
        private Location location;
        private String gene;
        private String bNumber;
        private static final Pattern B_NUMBER = Pattern.compile("b\\d+");
        private static final Pattern GENE_NAME = Pattern.compile("[a-z]{3}(?:[A-Z])?");

        /**
         * Construct a feature-data object from a feature.
         *
         * @param feat	incoming feature
         */
        public FeatureData(Feature feat) {
            this.id = feat.getId();
            this.function = feat.getPegFunction();
            this.location = feat.getLocation();
            // Get the feature's gene name and blatner number.
            this.gene = "";
            this.bNumber = "";
            for (String alias : feat.getAliases()) {
                if (B_NUMBER.matcher(alias).matches())
                    this.bNumber = alias;
                else if (GENE_NAME.matcher(alias).matches())
                    this.gene = alias;
            }
        }

        /**
         * This object sorts by location.
         */
        @Override
        public int compareTo(FeatureData o) {
            int retVal = this.location.compareTo(o.location);
            if (retVal == 0)
                retVal = this.id.compareTo(o.id);
            return retVal;
        }

        /**
         * @return the feature ID
         */
        public String getId() {
            return this.id;
        }

        /**
         * @return the function of this feature
         */
        public String getFunction() {
            return this.function;
        }

        /**
         * @return the location of this feature
         */
        public Location getLocation() {
            return this.location;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FeatureData)) {
                return false;
            }
            FeatureData other = (FeatureData) obj;
            if (this.id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!this.id.equals(other.id)) {
                return false;
            }
            return true;
        }

        /** serialization method for FeatureData */
        private void writeObject(ObjectOutputStream os) throws IOException {
            os.writeUTF(this.id);
            os.writeUTF(this.function);
            os.writeUTF(this.location.toString());
            os.writeUTF(this.gene);
            os.writeUTF(this.bNumber);
        }

        /** deserialization method for FeatureData */
        private void readObject(ObjectInputStream is) throws IOException {
            this.id = is.readUTF();
            this.function = is.readUTF();
            String locString = is.readUTF();
            this.location = Location.fromString(locString);
            this.gene = is.readUTF();
            this.bNumber = is.readUTF();
        }

        /**
         * @return the gene name for this feature
         */
        public String getGene() {
            return this.gene;
        }

        /**
         * @return the Blatner number for this feature
         */
        public String getBNumber() {
            return this.bNumber;
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
    public class Row implements Serializable {
        /** object version ID */
        private static final long serialVersionUID = 1L;
        /** feature hit */
        private FeatureData feat;
        /** neighbor feature (or NULL) */
        private FeatureData neighbor;
        /** hit list */
        private Weight[] weights;

        /**
         * Create a row for a feature.
         *
         * @param fData		target feature
         * @param neighbor	useful neighbor
         */
        private Row(FeatureData fData, Feature neighbor) {
            this.feat = fData;
            this.neighbor = null;
            if (neighbor != null)
                this.neighbor = new FeatureData(neighbor);
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
        public FeatureData getFeat() {
            return this.feat;
        }

        /**
         * @return the neighbor feature
         */
        public FeatureData getNeighbor() {
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
        FeatureData fData = new FeatureData(feat);
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
        return this.colMap.get(sample);
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
            String function = row.feat.function;
            if (RNA_PREDICATE.test(function) || row.feat.id.contains(".rna.")) {
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

}
