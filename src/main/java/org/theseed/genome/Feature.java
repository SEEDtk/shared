package org.theseed.genome;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.locations.Location;
import org.theseed.locations.Region;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.roles.RoleUtilities;
import org.theseed.sequence.MD5Hex;

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
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(Feature.class);
    /** ID of this feature */
    private String id;
    /** type of feature (CDS, rna, crispr...) */
    private String type;
    /** assigned function */
    private String function;
    /** protein sequence translation (if applicable) */
    private String protein_translation;
    /** location of the feature (contig, offset, length) */
    private Location location;
    /** genus-specific protein family */
    private String plfam;
    /** global protein family */
    private String pgfam;
    /** fig protein family */
    private String figfam;
    /** annotation history */
    private List<Annotation> annotations;
    /** original JSON definition */
    private JsonObject original;
    /** gene ontology ID numbers */
    private Collection<GoTerm> goTerms;
    /** alternate names (map of type -> aliases */
    private Map<String, NavigableSet<String>> aliases;
    /** genome containing this feature */
    private Genome parent;
    /** list of functionally-coupled features */
    private SortedSet<Coupling> couplings;
    /** subsystem membership data */
    private Set<SubsystemRow.Role> subsystemRoles;
    /** parsing pattern for feature type */
    private static final Pattern TYPE_PATTERN = Pattern.compile("fig\\|\\d+\\.\\d+\\.([^.]+)\\.\\d+");
    /** parsing pattern for function-to-roles */
    private static final Pattern SEP_PATTERN = Pattern.compile("\\s+[\\/@]\\s+|\\s*;\\s+");
    /** pattern for extracting genome ID from feature ID */
    public static final Pattern FID_PARSER = Pattern.compile("fig\\|(\\d+\\.\\d+)\\.[^.]+\\.\\d+");
    /** gene name match pattern */
    protected static final Pattern GENE_NAME = Pattern.compile("(?:[a-z]{3,4}(?:[A-Z]+[0-9]?||[0-9][a-z]?||[A-Z][0-9]*(?:_[0-9])?||[a-z][0-9]+||[a-z]+)?)||[a-z]{2}[0-9]+||[A-Z][0-9]+");
    /** GI match pattern */
    protected static final Pattern GENERAL_ALIAS = Pattern.compile("([^:|]+)(?::|\\|)(.+)");
    /** alias mappings */
    protected static final Map<Pattern, String> ALIAS_TYPE_MAP =
    		Map.of( Pattern.compile("Uniprot\\w*", Pattern.CASE_INSENSITIVE), "UniProt",
    				Pattern.compile("uni", Pattern.CASE_INSENSITIVE), "UniProt",
    				Pattern.compile("Swiss-?prot", Pattern.CASE_INSENSITIVE), "SwissProt",
    				Pattern.compile("gene_?id", Pattern.CASE_INSENSITIVE), "GeneID",
    				Pattern.compile("locus", Pattern.CASE_INSENSITIVE), "LocusTag",
    				Pattern.compile("gene", Pattern.CASE_INSENSITIVE), "gene_name");

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
            } else if (retVal.contentEquals("mp")) {
                retVal = "mat_peptide";
            }
        }
        return retVal;
    }

    /**
     * @return the genome ID portion of a feature ID
     *
     * @param fid	feature ID to parse for a genome ID
     */
    public static String genomeOf(String fid) {
        String retVal;
        if (fid == null) {
            retVal = "<none>";
        } else {
            Matcher m = FID_PARSER.matcher(fid);
            if (! m.matches()) {
                throw new IllegalArgumentException("Invalid feature ID" + fid);
            } else {
                retVal = m.group(1);
            }
        }
        return retVal;
    }

    /**
     * @return TRUE if a function is hypothetical
     *
     * @param function	the functional assignment of interest
     */
    public static boolean isHypothetical(String function) {
        boolean retVal = (function == null);
        if (! retVal) {
            String normalized = StringUtils.substringBefore(function, "#").trim().toLowerCase();
            retVal = (normalized.contains("hypothetical") || normalized.isEmpty());
        }
        return retVal;
    }

    /** This enum defines the keys used and their default values.
     */
    public static enum FeatureKeys implements JsonKey {
        ID("missing"),
        TYPE("CDS"),
        FUNCTION(""),
        PROTEIN_TRANSLATION(""),
        FAMILY_ASSIGNMENTS(null),
        LOCATION(null),
        GO_TERMS(null),
        ALIAS_PAIRS(null),
        ALIASES(null),
        COUPLINGS(null),
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
     * Comparator for sorting by function.  Hypotheticals go at the end.
     */
    public static class ByFunction implements Comparator<Feature> {

        @Override
        public int compare(Feature arg0, Feature arg1) {
            int retVal;
            String function0 = arg0.getFunction();
            String function1 = arg1.getFunction();
            boolean hypo0 = isHypothetical(function0);
            boolean hypo1 = isHypothetical(function1);
            if (hypo0 && hypo1) {
                retVal = StringUtils.compare(function0, function1);
            } else if (hypo0) {
                retVal = 1;
            } else if (hypo1) {
                retVal = -1;
            } else {
                retVal = StringUtils.compare(function0, function1);
            }
            if (retVal == 0) {
                // If the functions are the same, compare the feature IDs.
                retVal = arg0.compareTo(arg1);
            }
            return retVal;
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
        // Denote we have no subsystem connections.
        this.subsystemRoles = new HashSet<SubsystemRow.Role>();
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
        // Get the GO terms.
        JsonArray goTermList = feat.getCollectionOrDefault(FeatureKeys.GO_TERMS);
        this.goTerms = new ArrayList<GoTerm>();
        if (goTermList != null) {
            for (Object goTermObj : goTermList) {
                GoTerm goTerm = new GoTerm((JsonArray) goTermObj);
                this.goTerms.add(goTerm);
            }
        }
        // Get the aliases. This is presented as a list of lists. Each sub-list is of
        // the form [type,alias]. Note that we use trees because we have few alias types
        // and fewer aliases per type.
        JsonArray aliasList = feat.getCollectionOrDefault(FeatureKeys.ALIAS_PAIRS);
        this.aliases = new TreeMap<String, NavigableSet<String>>();
        if (aliasList != null) {
            for (int i = 0; i < aliasList.size(); i++) {
                JsonArray aliasPair = aliasList.getCollection(i);
                if (aliasPair.size() != 2)
                    log.error("Invalid alias pair at position {} for feature {}.", i, this.id);
                else {
                    String aliasType = aliasPair.getString(0);
                    String alias = aliasPair.getString(1);
                    this.addAlias(aliasType, alias);
                }
            }
        }
        // Now we need to untangle old-format aliases.
        aliasList = feat.getCollectionOrDefault(FeatureKeys.ALIASES);
        if (aliasList != null) {
        	for (Object aliasO : aliasList) {
        		String aliasPair = (String) aliasO;
        		var splitAliases = analyzeAlias(aliasPair);
        		for (var splitAlias : splitAliases)
        			this.addAlias(splitAlias.getKey(), splitAlias.getValue());
        	}
        }
        // Get the couplings.
        JsonArray couplingsList = feat.getCollectionOrDefault(FeatureKeys.COUPLINGS);
        this.couplings = new TreeSet<Coupling>();
        if (couplingsList != null && couplingsList.size() > 0) {
            for (Object couplingList : couplingsList) {
                JsonArray coupling = (JsonArray) couplingList;
                String target = coupling.getString(0);
                int size = coupling.getInteger(1);
                double strength = coupling.getDouble(2);
                this.addCoupling(target, size, strength);
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
                } else if (type.contentEquals("FIGFAM")) {
                    this.figfam = value;
                }
            }
        }
    }

    /**
     * Compute the type and name of a single-string alias. Most aliases are encoded with the
     * type name, a separator (: or |), and the name. The gene name is recognized by its
     * structure. Everything else is treated as a locus tag.
     *
	 * @param aliasPair		alias in single-string format
	 *
	 * @return a list of entries, containing alias types and names, respectively
	 */
	public static List<Map.Entry<String, String>> analyzeAlias(String aliasPair) {
		var retVal = new ArrayList<Map.Entry<String, String>>(1);
		Matcher m = GENERAL_ALIAS.matcher(aliasPair);
		if (m.matches()) {
			// We have several complications here. An ID that is used for two types is
			// represented by the type names separated with a slash. Also, there are a
			// lot of type synonyms. First, we split up the types.
			String[] aTypes = StringUtils.split(m.group(1), "/");
			for (String aType : aTypes) {
				// Find a match for one of the type patterns.
				aType = fixAliasType(aType);
				retVal.add(new AbstractMap.SimpleEntry<String, String>(aType, m.group(2)));
			}
		} else {
			m = GENE_NAME.matcher(aliasPair);
			if (m.matches())
				retVal.add(new AbstractMap.SimpleEntry<String, String>("gene_name", aliasPair));
			else
				retVal.add(new AbstractMap.SimpleEntry<String, String>("LocusTag", aliasPair));
		}
		return retVal;
	}

	/**
	 * This method normalizes an alias type. We use the alias type map to find out if
	 * there is a preferred synonym for a type string.
	 *
	 * @param aType		alias type to check
	 *
	 * @return the preferred alias type
	 */
	private static String fixAliasType(String aType) {
		var iter = ALIAS_TYPE_MAP.entrySet().iterator();
		String retVal = null;
		// Loop until we find a match or we run out of patterns.
		while (retVal == null && iter.hasNext()) {
			var aliasEntry = iter.next();
			Pattern aliasPattern = aliasEntry.getKey();
			if (aliasPattern.matcher(aType).matches())
				retVal = aliasEntry.getValue();
		}
		// If no match, return the input.
		if (retVal == null)
			retVal = aType;
		return retVal;
	}

	/**
     * Add a new coupling specification for this feature.  If a coupling already exists for the same
     * target, it is overwritten.
     *
     * @param target	target coupled feature
     * @param size		size of the coupling group
     * @param strength	strength of the coupling
     */
    public void addCoupling(String target, int size, double strength) {
        Coupling coupling = new Coupling(target, size, strength);
        if (this.couplings.contains(coupling)) this.couplings.remove(coupling);
        this.couplings.add(coupling);
    }

    /**
     * @return the gene name for this feature
     */
    public String getGeneName() {
        String retVal;
        // Note we expect only one gene name.
        NavigableSet<String> names = this.aliases.get("gene_name");
        if (names == null || names.isEmpty())
            retVal = "";
        else
            retVal = names.first();
        return retVal;
    }

    /**
     * @return the list of couplings for this feature. Modifying this list will not modify the feature
     */
    public Coupling[] getCouplings() {
        int n = this.couplings.size();
        Coupling[] retVal = new Coupling[n];
        if (n > 0)
            retVal = this.couplings.toArray(retVal);
        return retVal;
    }

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
        Location loc = Location.create(contigId, strand, left, right);
        this.init(fid, function, loc);
    }

    /**
     * Create a basic feature at a specific location.
     *
     * @param fid		ID to give to the feature
     * @param function	the functional assignment of the feature
     * @param loc		the location of the feature
     */
    public Feature(String fid, String function, Location loc) {
        this.init(fid, function, loc);
    }

    /**
     * Initialize a new feature.
     *
     * @param fid		ID to give to the feature
     * @param function	the functional assignment of the feature
     * @param loc		the location of the feature
     */
    protected void init(String fid, String function, Location loc) {
        this.id = fid;
        this.type = fidType(fid);
        this.function = function;
        this.location = loc;
        this.protein_translation = "";
        this.subsystemRoles = new HashSet<SubsystemRow.Role>();
        this.annotations = new ArrayList<Annotation>(3);
        this.goTerms = new ArrayList<GoTerm>(2);
        this.aliases = new TreeMap<String, NavigableSet<String>>();
        this.couplings = new TreeSet<Coupling>();
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
     * @return the function, or "hypothetical protein" if there is none
     */
    public String getPegFunction() {
        String retVal = this.function;
        if (retVal == null || retVal.isEmpty())
            retVal = "hypothetical protein";
        return retVal;
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
        return retVal;
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
            String commentFree = RoleUtilities.commentFree(function);
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

    /**
     * This class sorts by location, but it is strand-based.  That is, different strands
     * are treated as different contigs.  If two features have the
     * same location information, it will fall back to the feature ID, to insure no
     * two distinct features compare equal.
     */
    public static class StrandComparator implements Comparator<Feature> {

        private Comparator<Location> comparator;

        public StrandComparator() {
            this.comparator = new Location.StrandSorter();
        }

        @Override
        public int compare(Feature o1, Feature o2) {
            int retVal = this.comparator.compare(o1.location, o2.location);
            if (retVal == 0)
                retVal = o1.compareTo(o2);
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
     * @return TRUE if this feature has the same data as another feature
     *
     * @param other		other feature to check
     */
    public boolean same(Feature other) {
        boolean retVal = this.id.equals(other.id);
        if (retVal) {
            retVal = this.function.equals(other.function);
            if (retVal && ! StringUtils.isBlank(this.protein_translation)) {
                retVal = this.protein_translation.equals(other.protein_translation);
            }
        }
        if (retVal) {
            retVal = this.aliases.equals(other.aliases);
            if (retVal) {
                retVal = (StringUtils.equals(this.pgfam, other.pgfam) && StringUtils.equals(this.plfam, other.plfam)
                        && this.location.equals(other.location) && this.type.equals(other.type));
                if (retVal) {
                    retVal = this.annotations.equals(other.annotations);
                    if (retVal) {
                        Set<GoTerm> goTerms = new HashSet<GoTerm>(this.goTerms);
                        Set<GoTerm> otherTerms = new HashSet<GoTerm>(other.goTerms);
                        retVal = goTerms.equals(otherTerms);
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * @return the roles in this feature that are also found in the specified role map.
     *
     * @param map	map containing the roles considered useful
     */
    public List<Role> getUsefulRoles(RoleMap map) {
        return usefulRoles(map, this.function);
    }

    /**
     * This a version of "getUsefulRoles" for just deciding if a feature is interesting based
     * on its roles.  It is slightly faster than the other method, and more convenient.
     *
     * @param map		role map containing all useful roles
     *
     * @return TRUE if the roles in this feature are also found in the specified role map
     */
    public boolean isInteresting(RoleMap map) {
        String[] roleNames = rolesOfFunction(this.function);
        boolean retVal = Arrays.stream(roleNames).anyMatch(x -> map.getByName(x) != null);
        return retVal;
    }

    /**
     * @return a list of the useful roles in a functional assignment
     *
     * @param map		role map containing all useful roles
     * @param function	array of role descriptions to scan
     */
    public static List<Role> usefulRoles(RoleMap map, String function) {
        String[] roleNames = rolesOfFunction(function);
        List<Role> retVal = new ArrayList<Role>(roleNames.length);
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
    public boolean isProtein() {
        return this.type.contentEquals("CDS")  || this.type.contentEquals("mat_peptide");
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
        // Another array of arrays:  GO terms.
        JsonArray goList = new JsonArray();
        for (GoTerm goTerm : this.goTerms) {
            JsonArray goTerm0 = new JsonArray().addChain(String.format("GO:%07d", goTerm.getNumber()));
            if (goTerm.getDescription() != null)
                goTerm0.addChain(goTerm.getDescription());
            goList.add(goTerm0);
        }
        if (goList.size() > 0)
            retVal.put(FeatureKeys.GO_TERMS.getKey(), goList);
        // Similarly, couplings.
        JsonArray couplingList = new JsonArray();
        for (Coupling coupling : this.couplings) {
            JsonArray coupling0 = new JsonArray().addChain(coupling.getTarget()).addChain(coupling.getSize())
                    .addChain(coupling.getStrength());
            couplingList.add(coupling0);
        }
        retVal.put(FeatureKeys.COUPLINGS.getKey(), couplingList);
        // Next, aliases.
        JsonArray aliasList = new JsonArray();
        for (var aliasEntry : this.aliases.entrySet()) {
            String aliasType = aliasEntry.getKey();
            for (String alias : aliasEntry.getValue()) {
                JsonArray aliasPair = new JsonArray();
                aliasPair.add(aliasType);
                aliasPair.add(alias);
                aliasList.add(aliasPair);
            }
        }
        retVal.put(FeatureKeys.ALIAS_PAIRS.getKey(), aliasList);
        // Insure the old aliases aren't hanging around.
        retVal.remove("aliases");
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
        if (this.figfam != null) {
            JsonArray ffam = new JsonArray().addChain("FIGFAM").addChain(this.figfam);
            famList.add(ffam);
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
     * Add a new annotation.
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
     * Add an old annotation.
     *
     * @param comment		annotation comment
     * @param time			annotation time
     * @param annotator		annotator name
     *
     */
    public void addAnnotation(String comment, double time, String annotator) {
        Annotation newAnno = new Annotation(comment, time, annotator);
        this.annotations.add(newAnno);
    }

    /**
     * Store the local protein family ID
     *
     * @param plfam 	the plfam to set
     */
    public void setPlfam(String plfam) {
        if (plfam != null && ! plfam.isEmpty())
            this.plfam = plfam;
        else
            this.plfam = null;
    }

    /**
     * Store the global protein family ID
     *
     * @param pgfam 	the pgfam to set
     */
    public void setPgfam(String pgfam) {
        if (pgfam != null && ! pgfam.isEmpty())
            this.pgfam = pgfam;
        else
            this.pgfam = null;
    }

    /**
     * Store this feature's protein translation.
     *
     * @param protein	the protein string to store
     */
    public void setProteinTranslation(String protein) {
        this.protein_translation = protein;
    }

    /**
     * @return this feature's gene ontology terms
     */
    public Collection<GoTerm> getGoTerms() {
        return this.goTerms;
    }

    /**
     * Add a GO term to this feature.
     *
     * @param goString	a GO term string, containing "GO:" followed by the GO number and optionally a bar ("|")
     * 					followed by the description
     */
    public void addGoTerm(String goString) {
        GoTerm newGoTerm = new GoTerm(goString);
        this.goTerms.add(newGoTerm);
    }

    /**
     * Add a new alias ID for this feature. If a blank alias is added, all aliases of that type
     * are deleted.
     *
     * @param aliasType	alias type
     * @param aliasId	alias
     */
    public void addAlias(String aliasType, String aliasId) {
        if (StringUtils.isBlank(aliasId))
            this.aliases.remove(aliasType);
        else {
            Set<String> aliasSet = this.aliases.computeIfAbsent(aliasType, x -> new TreeSet<String>());
            aliasSet.add(aliasId);
        }
    }

    /**
     * @return this feature's list of aliases.
     */
    public Collection<String> getAliases() {
        List<String> retVal = new ArrayList<String>(this.aliases.size());
        this.aliases.values().stream().forEach(x -> retVal.addAll(x));
        return retVal;
    }

    @Override
    public String toString() {
        return "Feature [" + (id != null ? id : "") + (function != null ? ", " + function : "")
                + "]";
    }

    /**
     * Specify a new functional assignment for this feature.
     *
     * @param function		new function to assign
     */
    public void setFunction(String function) {
        this.function = function;
    }

    /**
     * @return the parent genome
     */
    public Genome getParent() {
        return parent;
    }

    /**
     * @param parent 	attach this feature to a parent genome
     */
    public void setParent(Genome parent) {
        this.parent = parent;
    }

    /**
     * Erase all the couplings for this feature.
     */
    public void clearCouplings() {
        this.couplings.clear();
    }

    /**
     * @return the set of subsystems containing this feature
     */
    public SortedSet<String> getSubsystems() {
        SortedSet<String> retVal = new TreeSet<String>();
        for (SubsystemRow.Role subRole : this.subsystemRoles) {
            retVal.add(subRole.getSubName());
        }
        return retVal;
    }

    /**
     * @return the set of subsystem rows containing this feature
     */
    public SortedSet<SubsystemRow> getSubsystemRows() {
        SortedSet<SubsystemRow> retVal = new TreeSet<SubsystemRow>();
        for (SubsystemRow.Role subRole : this.subsystemRoles) {
            retVal.add(subRole.getRow());
        }
        return retVal;
    }

    /**
     * @return TRUE if this feature is the sole instance of a subsystem role, else FALSE
     */
    public boolean isSubsystemLocked() {
        boolean retVal = this.subsystemRoles.stream().anyMatch(x -> x.getFeatures().size() <= 1);
        return retVal;
    }

    /**
     * Connect this feature to a subsystem role.
     *
     * @param role		role to which this feature is connected
     */
    protected void connectSubsystem(SubsystemRow.Role role) {
        this.subsystemRoles.add(role);
    }

    /**
     * @return the alias map
     */
    public Map<String, NavigableSet<String>> getAliasMap() {
        return this.aliases;
    }


    /**
     * @return the upstream gap region of this feature
     */
    public Location upstreamGap() {
        // Get the length to the appropriate end of the contig.
        int gap = this.location.getLeft() - 1;
        if (this.location.getDir() == '-') {
            Contig contig = this.parent.getContig(this.location.getContigId());
            gap = contig.length() - this.location.getRight();
        }
        // Find the smallest upstream gap to another encoding feature.
        for (Feature other : this.parent.getFeatures()) {
            if (other.isProtein() || other.getType().contentEquals("rna")) {
                int otherGap = this.location.upstreamDistance(other.location);
                if (otherGap < gap) gap = otherGap;
            }
        }
        // Return the upstream gap of the specified length.
        return this.location.upstream(gap);
    }

    /**
     * @return TRUE if this feature is upstream of a specified other feature, else FALSE
     *
     * @param other		other feature to compare
     */
    public boolean isUpstream(Feature other) {
        return this.location.isUpstream(other.location);
    }

    /**
     * This is a utility enum that allows programs to specify extraction of a particular type of
     * sequence for a feature.
     */
    public static enum SeqType {
        PROTEIN {
            @Override
            public String get(Feature feat) {
                return feat.getProteinTranslation();
            }
        }, DNA {
            @Override
            public String get(Feature feat) {
                return feat.parent.getDna(feat.getLocation());
            }
        };

        /**
         * @return the sequence of this type for the specified feature
         *
         * @param feat	feature of interest
         */
        public abstract String get(Feature feat);
    }

    /**
     * Specify the FIG protein family for this feature.
     *
     * @param ffam	proposed FigFam for this feature
     */
    public void setFigfam(String ffam) {
        if (ffam != null && ! ffam.isEmpty())
            this.figfam = ffam;
        else
            this.figfam = null;
    }

    /**
     * Disconnect this feature from all couplings and subsystems.  This may cause a subsystem to
     * become invalid.
     *
     * @return TRUE if all subsystems are still valid, else FALSE
     */
    protected boolean disconnect() {
        boolean retVal = true;
        // First, disconnect the couplings from the coupled features.
        for (Coupling coupling : this.couplings) {
            Feature other = this.parent.getFeature(coupling.getTarget());
            // Find the coupling to this feature and remove it.
            Iterator<Coupling> iter = other.couplings.iterator();
            while (iter.hasNext()) {
                Coupling otherCoupling = iter.next();
                if (otherCoupling.getTarget().contentEquals(this.id))
                    iter.remove();
            }
        }
        // Next, delete this feature from all the subsystems.
        for (var subRole : this.subsystemRoles) {
            if (! subRole.disconnectFeature(this.id)) retVal = false;
        }
        // Return the success indicator.
        return retVal;
    }

    /**
     * @return the FIG protein family for this feature
     */
    public String getFigfam() {
        return this.figfam;
    }

    /**
     * @return the protein MD5, or NULL if there is no protein
     */
    public String getMD5() {
        String retVal = null;
        String prot = this.protein_translation;
        if (! StringUtils.isBlank(prot)) try {
            MD5Hex computer = new MD5Hex();
            retVal = computer.checksum(this.protein_translation);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing MD5 algorithm. " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error in MD5 computation. " + e.getMessage());
        }
        return retVal;
    }

    /**
     * @return the DNA for this feature
     */
    public String getDna() {
        return this.parent.getDna(this.location);
    }

    /**
     * @return a key for a pair of feature IDs
     *
     * @param iFid	first feature ID
     * @param jFid	second feature ID
     */
    public static String pairKey(String iFid, String jFid) {
        StringBuilder retVal = new StringBuilder(20);
        // Split the strings in parts.
        String[] iParts = StringUtils.splitPreserveAllTokens(iFid, "|.");
        String[] jParts = StringUtils.split(jFid, "|.");
        // Prime with the fig|.
        retVal.append("fig");
        String delim = "|";
        int cmp = 0;
        // Find the first differing part.
        int split = -1;
        for (int k = 1; cmp == 0 && k < iParts.length; k++) {
            cmp = iParts[k].compareTo(jParts[k]);
            if (cmp == 0)
                retVal.append(delim).append(iParts[k]);
            else
                split = k;
            delim = ".";
        }
        // Now "k" points to the first difference, and "cmp" is the comparison result.
        // Put the lexically lower string into "iParts".
        if (cmp > 0) {
            String[] buffer = jParts;
            jParts = iParts;
            iParts = buffer;
        }
        if (cmp != 0) {
            // Now finish off the pairing.
            delim = ":";
            for (int k = split; k < iParts.length; k++) {
                retVal.append(delim).append(iParts[k]);
                delim = ".";
            }
            delim = "/";
            for (int k = split; k < jParts.length; k++) {
                retVal.append(delim).append(jParts[k]);
                delim = ".";
            }
        }
        return retVal.toString();
    }

    /**
     * Specify a new gene name. The gene name is in the list of aliases and it has type
     * "gene_name".
     *
     * @param name		proposed new gene name, or an empty string for none
     */
    public void setGeneName(String name) {
        if (StringUtils.isBlank(name))
            this.aliases.remove("gene_name");
        else {
            var nameSet = new TreeSet<String>();
            nameSet.add(name);
            this.aliases.put("gene_name", nameSet);
        }
    }

}
