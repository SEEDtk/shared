package org.theseed.genomes;

import org.theseed.locations.Location;

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


    /** Create this feature from its JsonObject.
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


}
