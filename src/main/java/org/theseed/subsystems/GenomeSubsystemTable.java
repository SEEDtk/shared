/**
 *
 */
package org.theseed.subsystems;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.LineReader;

/**
 * This object manages a table that maps feature IDs to subsystems from a single genome.  The
 * object is initialized from a flat file created using the SubsystemRowDescriptor object.
 *
 * @author Bruce Parrello
 *
 */
public class GenomeSubsystemTable {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(GenomeSubsystemTable.class);
    /** map of feature IDs to subsystems */
    private Map<String, Set<SubData>> subMap;
    /** map of subsystem IDs to features IDs */
    private SortedMap<String, Set<String>> subFeatureMap;

    /**
     * This sub-object contains the ID and description of a single subsystem.
     */
    public static class SubData implements Comparable<SubData> {

        private String id;
        private String name;
        private String[] classes;

        private SubData(String id, String name, String classes) {
            this.id = id;
            this.name = name;
            this.classes = StringUtils.splitByWholeSeparatorPreserveAllTokens(classes, SubsystemRowDescriptor.CLASS_DELIM);
        }

        @Override
        public int compareTo(SubData o) {
            return this.name.compareTo(o.name);
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
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SubData other = (SubData) obj;
            if (this.id == null) {
                if (other.id != null)
                    return false;
            } else if (!this.id.equals(other.id))
                return false;
            return true;
        }

        /**
         * @return the subsystem ID
         */
        public String getId() {
            return this.id;
        }

        /**
         * @return the subsystem name
         */
        public String getName() {
            return this.name;
        }

        /**
         * @return the classifications
         */
        public String[] getClasses() {
            return this.classes;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            StringBuilder retVal = new StringBuilder(this.classes.length * 20 + this.name.length() + 10);
            if (this.classes.length > 0)
                retVal.append(StringUtils.join(this.classes, SubsystemRowDescriptor.CLASS_DELIM)).append(": ");
            retVal.append(this.name);
            return retVal.toString();
        }

    }


    /**
     * Create a genome subsystem table from an input file.
     *
     * @param inFile	name of a file created using SubsystemRowDescriptor.createFile
     *
     * @throws IOException
     */
    public GenomeSubsystemTable(File inFile) throws IOException {
        // Initialize the subsystem map.
        this.subMap = new HashMap<String, Set<SubData>>(3000);
        this.subFeatureMap = new TreeMap<String, Set<String>>();
        // Loop through the subsystem file.
        try (LineReader reader = new LineReader(inFile)) {
            int count = 0;
            for (String line : reader) {
                String[] parts = StringUtils.splitPreserveAllTokens(line, "\t");
                // Create the subsystem descriptor.
                SubData sub = new SubData(parts[0], parts[1], parts[2]);
                // Get the feature ID list.
                Set<String> fidSet = new HashSet<String>(Arrays.asList(StringUtils.splitByWholeSeparator(parts[3], SubsystemRowDescriptor.FID_DELIM)));
                // Add it to the main list.
                this.subFeatureMap.put(sub.getId(), fidSet);
                // Add it to each feature's subsystem set.
                for (String fid : fidSet) {
                    Set<SubData> fidSubs = this.subMap.computeIfAbsent(fid, x -> new TreeSet<SubData>());
                    fidSubs.add(sub);
                }
                count++;
            }
            log.info("{} subsystems read from {}.", count, inFile);
        }
    }

    /**
     * Get the set of subsystems for a given feature.
     *
     * @param fid	ID of the feature of interest
     *
     * @return the set of subsystems containing the feature
     */
    public Set<SubData> getSubsystems(String fid) {
        return this.subMap.get(fid);
    }

    /**
     * Get the set of features for a given subsystem ID.
     *
     * @param sub	ID of the subsystem of interest
     *
     * @return the set of IDs for the features in the subsystem
     */
    public Set<String> getSubFeatures(String sub) {
        return this.subFeatureMap.getOrDefault(sub, Collections.emptySet());
    }

    /**
     * Get the list of subsystems for the genome.
     */
    public List<String> getAllSubsystems() {
        return new ArrayList<String>(this.subFeatureMap.keySet());
    }

    /**
     * @return TRUE if the specified feature is in any subsystem
     */
    public boolean isInSubsystem(String fid) {
        return this.subMap.containsKey(fid);
    }

}
