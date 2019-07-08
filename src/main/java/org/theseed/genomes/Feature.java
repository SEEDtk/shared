package org.theseed.genomes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.theseed.locations.Location;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/** This class implements a feature.  The feature is constructible from a JsonObject and contains
 * the key fields used in feature analysis.
 *
 * @author Bruce Parrello
 *
 */
public class Feature implements Comparable<Feature> {

    // FIELDS

    private String id;
    private String type;
    private String function;
    private String protein_translation;
    private Location location;

    /** parsing pattern for feature type */
    private static final Pattern TYPE_PATTERN = Pattern.compile("fig\\|\\d+\\.\\d+\\.(\\w+)\\.\\d+");

    /** parsing pattern for function-to-roles */
    private static final Pattern SEP_PATTERN = Pattern.compile("\\s+[\\/@]\\s+|\\s*;\\s+");

    /** parsing pattern for removing function comments */
    private static final Pattern COMMENT_PATTERN = Pattern.compile("\\s*[#!].+");

    /**
     * @return the feature type of a feature ID.
     *
     * @param fid	feature ID of interest
     */
    public static String fidType(String fid) {
        Matcher m = TYPE_PATTERN.matcher(fid);
        String retVal = "error";
        if (m.matches()) {
            retVal = m.group(1);
            // For historical reasons, CDS features have a name of "peg".
            if (retVal.contentEquals("peg")) {
                retVal = "CDS";
            }
        }
        return retVal;
    }


    /** This enum defines the keys used and their default values.
     */
    private enum FeatureKeys implements JsonKey {
        ID("missing"),
        TYPE("CDS"),
        FUNCTION("hypothetical protein"),
        PROTEIN_TRANSLATION(""),
        LOCATION(null);

        private final Object m_value;

        FeatureKeys(final Object value) {
            this.m_value = value;
        }

        /** This is the string used as a key in the incoming JsonObject map.
         */
        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        /** This is the default value used when the key is not found.
         */
        @Override
        public Object getValue() {
            return this.m_value;
        }

    }


    /**
     * Create this feature from its JsonObject.
     *
     * @param feat	JsonObject read in for this feature
     */
    public Feature(JsonObject feat) {
        this.id = feat.getStringOrDefault(FeatureKeys.ID);
        this.type = feat.getStringOrDefault(FeatureKeys.TYPE);
        this.function = feat.getStringOrDefault(FeatureKeys.FUNCTION);
        this.protein_translation = feat.getStringOrDefault(FeatureKeys.PROTEIN_TRANSLATION);
        // Now we need to do the location.  This will come back as a list of regions.
        this.location = null;
        JsonArray regions = feat.getCollectionOrDefault(FeatureKeys.LOCATION);
        // Only proceed if there is at least one region.  The regions are [contigId, begin, strand, length].
        if (regions.size() > 0) {
            JsonArray first = (JsonArray) regions.get(0);
            this.location = Location.create((String) first.get(0), (String) first.get(2));
        }
        // Now loop through all the regions, adding them.
        for (Object regionO : regions) {
            JsonArray region = (JsonArray) regionO;
            int begin = region.getInteger(1);
            int length = region.getInteger(3);
            this.location.addRegion(begin, length);
        }
    }

    /**

    /**
     * Create a basic feature at a specific location.
     *
     * @param fid		ID to give to the feature
     * @param function	the functional role of the feature
     * @param contigId	the ID of the contig containing the feature
     * @param strand	the strand containing the feature- "+" or "-"
     * @param left		the leftmost position of the feature
     * @param right		the rightmost position of the feature
     */
    public Feature(String fid, String function, String contigId, String strand, int left, int right) {
        this.id = fid;
        this.type = fidType(fid);
        this.function = function;
        Location loc = Location.create(contigId, strand, left, right);
        this.location = loc;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the function
     */
    public String getFunction() {
        return function;
    }

    /**
     * @return the protein_translation
     */
    public String getProteinTranslation() {
        return protein_translation;
    }

    /**
     * @return the location for this feature
     */
    public Location getLocation() {
        return this.location;
    }

    /**
     * Compare two features based on feature ID.  The types are compared first (lexically), and
     * then the ID numbers compared.
     */
    @Override
    public int compareTo(Feature other) {
        // Compare the feature types.
        int retVal = this.type.compareTo(other.type);
        if (retVal == 0) {
            // Now we need to compare the index numbers at the end.  That means pulling them out.
            int idx = this.getIndexNum();
            int otherIdx = other.getIndexNum();
            retVal = idx - otherIdx;
        }
        return 0;
    }

    /**
     * @return the index number from this feature's ID.  If the ID is invalid, we return MAXVALUE.
     */
    private int getIndexNum() {
        int retVal = Integer.MAX_VALUE;
        // Find the last period.
        int i = this.id.lastIndexOf('.');
        if (i >= 0) {
            String numString = this.id.substring(i + 1);
            try {
                retVal = Integer.parseInt(numString);
            } catch (NumberFormatException e) {
                // Invalid ID.  We will return MAXVALUE.
            }
        }
        return retVal;
    }

    /**
     * @return the distance between this feature and another feature, or -1 if the features
     * 		   overlap
     *
     * @param other		other location to measure against this one
     */
    public int distance(Feature other) {
        return this.location.distance(other.location);
    }

    /**
     * @return the roles of this feature's function
     */
    public String[] getRoles() {
        return rolesOfFunction(this.function);
    }

    /**
     * @return the roles for a given function
     *
     * @param function	functional assignment string of interest
     */
    public static String[] rolesOfFunction(String function) {
        String[] retVal;
        if (function == null) {
            retVal = new String[0];
        } else {
            // Remove any comments.
            String commentFree = RegExUtils.removeFirst(function, COMMENT_PATTERN);
            // Split the function into roles.
            retVal = Arrays.stream(SEP_PATTERN.split(commentFree)).
                    filter(value -> value.length() > 0).toArray(String[]::new);
        }
        return retVal;
    }

    /**
     * This class is used to sort the features by location.  If two features have the
     * same location information, it will fall back to the feature ID, to insure no
     * two distinct features compare equal.
     */
    public static class LocationComparator implements Comparator<Feature> {

        @Override
        public int compare(Feature arg0, Feature arg1) {
            int retVal= arg0.location.compareTo(arg1.location);
            if (retVal == 0) {
                retVal = arg0.compareTo(arg1);
            }
            return retVal;
        }

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
        if (!(obj instanceof Feature))
            return false;
        Feature other = (Feature) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        } else if (!this.id.equals(other.id))
            return false;
        return true;
    }

    /**
     * @return the roles in this feature that are also found in the specified role map.
     *
     * @param map	map containing the roles considered useful
     */
    public List<Role> getUsefulRoles(RoleMap map) {
        String[] roleNames = this.getRoles();
        ArrayList<Role> retVal = new ArrayList<Role>(roleNames.length);
        for (String roleName : roleNames) {
            Role role = map.getByName(roleName);
            if (role != null) {
                retVal.add(role);
            }
        }
        return retVal;
    }


}
