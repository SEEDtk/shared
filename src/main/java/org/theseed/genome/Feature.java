package org.theseed.genome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.theseed.locations.Location;
import org.theseed.locations.Region;
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
    private String plfam;
    private String pgfam;
    private List<Annotation> annotations;
    private JsonObject original;

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
    public static enum FeatureKeys implements JsonKey {
        ID("missing"),
        TYPE("CDS"),
        FUNCTION("hypothetical protein"),
        PROTEIN_TRANSLATION(""),
        FAMILY_ASSIGNMENTS(null),
        LOCATION(null),
        ANNOTATIONS(null);

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
        // Save the original object.
        this.original = feat;
        // Extract the scalar fields.
        this.id = feat.getStringOrDefault(FeatureKeys.ID);
        this.type = fidType(this.id);
        this.function = feat.getStringOrDefault(FeatureKeys.FUNCTION);
        this.protein_translation = feat.getStringOrDefault(FeatureKeys.PROTEIN_TRANSLATION);
        // Now we need to do the location.  This will come back as a list of regions.
        this.location = null;
        JsonArray regions = feat.getCollectionOrDefault(FeatureKeys.LOCATION);
        // Only proceed if there is at least one region.  The regions are [contigId, begin, strand, length].
        if (regions != null && regions.size() > 0) {
            JsonArray first = (JsonArray) regions.get(0);
            this.location = Location.create((String) first.get(0), (String) first.get(2));
            // Now loop through all the regions, adding them.
            for (Object regionO : regions) {
                JsonArray region = (JsonArray) regionO;
                int begin = region.getInteger(1);
                int length = region.getInteger(3);
                this.location.addRegion(begin, length);
            }
        }
        // Get the annotations.
        JsonArray annotationList = feat.getCollectionOrDefault(FeatureKeys.ANNOTATIONS);
        this.annotations = new ArrayList<Annotation>();
        if (annotationList != null) {
            for (Object annotationItem : annotationList) {
                Annotation annotation = new Annotation((JsonArray) annotationItem);
                this.annotations.add(annotation);
            }
        }
        // Finally, we look for the protein families.
        JsonArray families = feat.getCollectionOrDefault(FeatureKeys.FAMILY_ASSIGNMENTS);
        if (families != null && families.size() > 0) {
            for (Object family0 : families) {
                JsonArray family = (JsonArray) family0;
                String type = family.getString(0);
                String value = family.getString(1);
                if (type.contentEquals("PGFAM")) {
                    this.pgfam = value;
                } else if (type.contentEquals("PLFAM")) {
                    this.plfam = value;
                }
            }
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
        this.protein_translation = "";
        this.annotations = new ArrayList<Annotation>(3);
        // Save a blank JSON object as the original.
        this.original = new JsonObject();
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
     * @return the protein length
     */
    public int getProteinLength() {
        int retVal = 0;
        if (protein_translation != null) {
            retVal = protein_translation.length();
        }
        return retVal;
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
     * @return the content of the specified JSON field
     *
     * @param name	name of the field to retrieve
     */
    public Object getJsonField(String name) {
        return this.original.get(name);
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

    /**
     * @return the local protein family ID (if any)
     */
    public String getPlfam() {
        return this.plfam;
    }

    /**
     * @return the global protein family ID (if any)
     */
    public String getPgfam() {
        return this.pgfam;
    }

    /**
     * @return TRUE if this is a protein-encoding feature
     */
    public boolean isCDS() {
        return this.type.contentEquals("CDS");
    }

    /**
     * Store the protein translation for this feature.
     *
     * @param aa_sequence	protein translation to store
     */
    public void storeProtein(String aa_sequence) {
        this.protein_translation = aa_sequence;
    }

    /**
     * Store the local protein family for this feature.
     *
     * @param plfam		protein family ID to store
     */
    public void storeLocalFamily(String plfam) {
        if (plfam != null && ! plfam.isEmpty())
            this.plfam = plfam;
    }

    /**
     * @return a json object for this feature
     */
    public JsonObject toJson() {
        // Get the original json object.
        JsonObject retVal = this.original;
        retVal.put(FeatureKeys.ID.getKey(), this.id);
        retVal.put(FeatureKeys.TYPE.getKey(), this.type);
        retVal.put(FeatureKeys.FUNCTION.getKey(), this.function);
        retVal.put(FeatureKeys.PROTEIN_TRANSLATION.getKey(), this.protein_translation);
        // The location is an array of arrays.  This will be the master.
        JsonArray locs = new JsonArray();
        if (this.location != null) {
            String contig = this.location.getContigId();
            String dir = this.location.getStrand();
            for (Region region : this.location.getRegions()) {
                JsonArray loc = new JsonArray().addChain(contig).addChain(region.getBegin(dir)).addChain(dir).addChain(region.getLength());
                locs.add(loc);
            }
        }
        retVal.put(FeatureKeys.LOCATION.getKey(), locs);
        // Store the annotations.  These are also an array of arrays.
        JsonArray annoList = new JsonArray();
        for (Annotation anno : this.annotations) {
            JsonArray anno0 = new JsonArray().addChain(anno.getComment()).addChain(anno.getAnnotator()).addChain(anno.getAnnotationTime());
            annoList.add(anno0);
        }
        retVal.put(FeatureKeys.ANNOTATIONS.getKey(), annoList);
        // Finally, store the protein families.
        JsonArray famList = new JsonArray();
        if (this.plfam != null) {
            JsonArray lfam = new JsonArray().addChain("PLFAM").addChain(this.plfam);
            famList.add(lfam);
        }
        if (this.pgfam != null) {
            JsonArray gfam = new JsonArray().addChain("PGFAM").addChain(this.pgfam);
            famList.add(gfam);
        }
        retVal.put(FeatureKeys.FAMILY_ASSIGNMENTS.getKey(), famList);
        return retVal;
    }

    /**
     * @return the annotations
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * Add an annotation.
     *
     * @param comment		annotation comment
     * @param annotator		annotator name
     *
     */
    public void addAnnotation(String comment, String annotator) {
        Annotation newAnno = new Annotation(comment, annotator);
        this.annotations.add(newAnno);
    }

    /**
     * Store the local protein family ID
     *
     * @param plfam 	the plfam to set
     */
    public void setPlfam(String plfam) {
        this.plfam = plfam;
    }

    /**
     * Store the global protein family ID
     *
     * @param pgfam 	the pgfam to set
     */
    public void setPgfam(String pgfam) {
        this.pgfam = pgfam;
    }


}
