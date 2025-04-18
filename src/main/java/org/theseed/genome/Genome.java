/**
 *
 */
package org.theseed.genome;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.locations.Location;
import org.theseed.locations.Region;
import org.theseed.proteins.Function;
import org.theseed.proteins.RoleMap;
import org.theseed.reports.LinkObject;
import org.theseed.roles.RoleUtilities;
import org.theseed.sequence.FastaOutputStream;
import org.theseed.sequence.Sequence;
import org.theseed.stats.Shuffler;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import j2html.tags.ContainerTag;

/**
 * GenomeTypeObject for Java
 *
 * This class implements a genome loaded from a GTO.  The entire genome resides in memory, so it is
 * recommended that only a few of these are read in at a time.  At the current time, only a limited
 * number of fields are implemented.  This will change if more are needed.
 *
 * @author Bruce Parrello
 *
 */
public class Genome  {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(Genome.class);
    /** ID of this genome */
    private String id;
    /** scientific name */
    private String name;
    /** protein translation code */
    private int geneticCode;
    /** domain (Bacteria, Archaea, etc.) */
    private String domain;
    /** NCBI taxonomy ID */
    private int taxonomyId;
    /** features, mapped by ID */
    private Map<String, Feature> features;
    /** contig sequences, mapped by ID */
    private Map<String, Contig> contigs;
    /** original JSON object */
    private JsonObject gto;
    /** full taxonomic lineage */
    private TaxItem[] lineage;
    /** list of close genomes */
    private SortedSet<CloseGenome> closeGenomes;
    /** database where the genome is stored */
    private String home;
    /** URL generator */
    private LinkObject linker;
    /** original genome source */
    private String source;
    /** original ID of the genome */
    private String sourceId;
    /** subsystems by name */
    private Map<String, SubsystemRow> subsystems;
    /** map of refseq sequence IDs to contig IDs */
    private Map<String, String> accessionMap;
    /** SSU-rRNA sequence (NULL if unknown, empty if not present */
    private String ssuRna;
    /** analysis event log */
    private List<AnalysisEvent> events;
    /** quality descriptor */
    private JsonObject quality;
    /** match pattern for SSU rRNA */
    public static final Pattern SSU_R_RNA = RoleUtilities.SSU_R_RNA;
    /** match pattern for LSU rRNA */
    public static final Pattern LSU_R_RNA = RoleUtilities.LSU_R_RNA;
    /** refseq location format */
    private static final Pattern ACCESSION_LOCATION = Pattern.compile("(\\w+):(\\d+)-(\\d+)");
    /** empty list used as a default intermediate value for cases where the contigs or features are missing */
    private static final Collection<JsonObject> noEntries = new ArrayList<JsonObject>();
    /** minimum length of a bad ambiguity run in an SSU */
    private static final String BAD_AMBIGUITY_RUN = "nnnnn";

    /** This enum defines the keys used and their default values.
     */
    public static enum GenomeKeys implements JsonKey {
        // GTO fields
        ID("0"),
        SCIENTIFIC_NAME("unknown organism"),
        NCBI_TAXONOMY_ID(2),
        GENETIC_CODE(11),
        DOMAIN("Bacteria"),
        CONTIGS(noEntries),
        NCBI_LINEAGE(noEntries),
        FEATURES(noEntries),
        CLOSE_GENOMES(noEntries),
        SSU_RRNA(null),
        ANALYSIS_EVENTS(noEntries),
        QUALITY(null),
        // PATRIC fields
        GENOME_ID("0"),
        GENOME_NAME("unknown organism"),
        TAXON_LINEAGE_IDS(noEntries),
        TAXON_ID(2),
        SUPERKINGDOM("Bacteria"),
        SOURCE(null),
        SOURCE_ID(null),
        HOME("none"),
        SUBSYSTEMS(noEntries);

        private final Object m_value;

        GenomeKeys(final Object value) {
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
     * This class is an iterator for pegs only.
     */
    public class Pegs implements Iterator<Feature> {

        /** iterator through the genome features */
        private Iterator<Feature> iter;
        /** next feature to return */
        private Feature nextFeature;

        /**
         * Construct a peg iterator for this genome.
         */
        public Pegs() {
            this.iter = Genome.this.features.values().iterator();
            this.nextFeature = this.findNext();
        }

        /**
         * @return the next peg in the genome, or NULL if there are none
         */
        private Feature findNext() {
            Feature retVal = null;
            while (retVal == null && this.iter.hasNext()) {
                Feature curr = this.iter.next();
                if (curr.getType().contentEquals("CDS"))
                    retVal = curr;
            }
            return retVal;
        }

        @Override
        public boolean hasNext() {
            return this.nextFeature != null;
        }

        @Override
        public Feature next() {
            Feature retVal = this.nextFeature;
            this.nextFeature = this.findNext();
            return retVal;
        }

    }

    /**
     * This class is an iterator for pegs with interesting roles.
     */
    public class InterestingPegs implements Iterator<Feature> {

        /** map of interesting roles */
        private RoleMap roleMap;
        /** next feature to return */
        private Feature nextFeature;
        /** iterator through the features */
        private Iterator<Feature> featIter;

        /**
         * Create an iterator through the genome based on the specified role map.
         *
         * @param roles		definition map for interesting roles
         */
        public InterestingPegs(RoleMap roles) {
            this.roleMap = roles;
            // Set up the iterator through the genome features.
            this.featIter = Genome.this.new Pegs();
            // Find the first interesting feature.
            this.nextFeature = this.findNext();
        }

        /**
         * @return the next interesting peg, or NULL if there are none
         */
        private Feature findNext() {
            Feature retVal = null;
            while (retVal == null && this.featIter.hasNext()) {
                // We stop when we find a feature that is a peg and has an interesting role.
                Feature curr = this.featIter.next();
                if (curr.isInteresting(this.roleMap))
                    retVal = curr;
            }
            return retVal;
        }

        @Override
        public boolean hasNext() {
            return this.nextFeature != null;
        }

        @Override
        public Feature next() {
            Feature retVal = this.nextFeature;
            this.nextFeature = this.findNext();
            return retVal;
        }

    }

    /**
     * Read a genome object from a file.
     *
     * @param inFile	the file containing the GTO
     *
     * @throws IOException
     */
    public Genome(File inFile) throws IOException {
        // Get a reader for the named file.
        try (FileReader reader = new FileReader(inFile)) {
            this.read(reader);
        }
    }

    /**
     * Read a genome object from a stream.
     *
     * @param stream	input stream containing the GTO
     *
     * @throws IOException
     */
    public Genome(InputStream stream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            this.read(reader);
        }
    }

    /**
     * Read a genome object from a JSON string.
     *
     * @param string	JSON string containing the GTO
     *
     * @throws IOException
     */
    public static Genome fromJson(String jsonString) throws IOException {
        Genome retVal = new Genome();
        try (StringReader reader = new StringReader(jsonString)) {
            retVal.read(reader);
        }
        return retVal;
    }

    /**
     * Read a genome into memory.
     *
     * @param reader	input stream containing the GTO.
     *
     * @throws IOException
     */
    private void read(Reader reader) throws IOException {
        // Read the genome from the file.
         try {
            this.gto = (JsonObject) Jsoner.deserialize(reader);
        } catch (JsonException e) {
            throw new IOException("Error reading JSON data.", e);
        }
        this.id = this.gto.getStringOrDefault(GenomeKeys.ID);
        this.name = this.gto.getStringOrDefault(GenomeKeys.SCIENTIFIC_NAME);
        this.taxonomyId = this.gto.getIntegerOrDefault(GenomeKeys.NCBI_TAXONOMY_ID);
        this.geneticCode = this.gto.getIntegerOrDefault(GenomeKeys.GENETIC_CODE);
        this.domain = this.gto.getStringOrDefault(GenomeKeys.DOMAIN);
        this.source = this.gto.getStringOrDefault(GenomeKeys.SOURCE);
        this.sourceId = this.gto.getStringOrDefault(GenomeKeys.SOURCE_ID);
        this.ssuRna = this.gto.getStringOrDefault(GenomeKeys.SSU_RRNA);
        // Get the quality object.
        this.quality = this.gto.getMapOrDefault(GenomeKeys.QUALITY);
        if (this.quality == null)
            this.quality = new JsonObject();
        // Extract the lineage IDs.
        Collection<JsonArray> lineageArray = this.gto.getCollectionOrDefault(GenomeKeys.NCBI_LINEAGE);
        this.lineage = lineageArray.stream().map(x -> new TaxItem(x)).toArray(n -> new TaxItem[n]);
        // Pull in the close genomes.
        this.closeGenomes = new TreeSet<CloseGenome>();
        Collection<JsonObject> closeList = this.gto.getCollectionOrDefault(GenomeKeys.CLOSE_GENOMES);
        for (JsonObject close : closeList)
            this.closeGenomes.add(new CloseGenome(close));
        // Now we need to process the features and contigs.
        Collection<JsonObject> featureList = this.gto.getCollectionOrDefault(GenomeKeys.FEATURES);
        this.features = new HashMap<String, Feature>();
        for (JsonObject feat : featureList) {
            Feature feature = new Feature(feat);
            this.addFeature(feature);
        }
        Collection<JsonObject> contigList = this.gto.getCollectionOrDefault(GenomeKeys.CONTIGS);
        this.contigs = new HashMap<String, Contig>();
        for (JsonObject contigObj : contigList) {
            Contig contig = new Contig(contigObj);
            this.contigs.put(contig.getId(), contig);
        }
        // We need any analysis events.
        Collection<JsonObject> events = this.gto.getCollectionOrDefault(GenomeKeys.ANALYSIS_EVENTS);
        this.events = new ArrayList<AnalysisEvent>();
        for (JsonObject eventObj : events) {
            AnalysisEvent event = new AnalysisEvent(eventObj);
            this.events.add(event);
        }
        // Finally, the subsystems.
        Collection<JsonObject> subList = this.gto.getCollectionOrDefault(GenomeKeys.SUBSYSTEMS);
        this.subsystems = new HashMap<String, SubsystemRow>();
        // The subsystem is put into the map by the constructor.
        for (JsonObject subsystemObj : subList)
            new SubsystemRow(this, subsystemObj);
        // Determine the Genome's home database.
        this.setHome(this.gto.getStringOrDefault(GenomeKeys.HOME));
    }

    /**
     * Create an empty genome.
     *
     * @param genomeId	ID of this genome
     * @param name		name of the genome
     * @param domain	domain of the genome ("Archaea", "Bacteria", etc.)
     * @param code		genetic code of the genome
     *
     * @throws NumberFormatException
     */
    public Genome(String genomeId, String name, String domain, int code) throws NumberFormatException {
        this.id = genomeId;
        this.name = name;
        this.taxonomyId = Integer.parseInt(StringUtils.substringBefore(genomeId, "."));
        this.domain = domain;
        this.geneticCode = code;
        setup();
    }

    /**
     * Set up the attached structures.
     */
    protected void setup() {
        // Create empty maps for features, close genomes, subsystems, and contigs.
        this.features = new HashMap<String, Feature>();
        this.contigs = new HashMap<String, Contig>();
        this.closeGenomes = new TreeSet<CloseGenome>();
        // Denote the genome has no subvsystems.
        this.subsystems = new HashMap<String, SubsystemRow>();
        // Create a blank original GTO.
        this.gto = new JsonObject();
        // Create a blank quality object.
        this.quality = new JsonObject();
        // Create an empty lineage.
        this.lineage = new TaxItem[0];
        // Create an empty event list.
        this.events = new ArrayList<AnalysisEvent>();
        // Denote the genome has no home.
        this.setHome("none");
        // Denote no accession map has been built.
        this.accessionMap = null;
    }

    /**
     * Specify the home location of this genome.
     *
     * @param string	a string indicating the home location-- PATRIC, CORE, or none.
     */
    public void setHome(String string) {
        this.home = string;
        // Determine the type of links to generate.
        switch (string) {
        case "PATRIC" :
        case "BV-BRC" :
            this.linker = new LinkObject.Patric();
            break;
        case "CORE" :
            this.linker = new LinkObject.Core();
            break;
        default:
            this.linker = new LinkObject.None();
        }
    }

    /**
     * Log a warning if the home location is unknown.
     */
    public void checkHome() {
        if (this.linker instanceof LinkObject.None)
            log.warn("Links are disabled for unknown home database \"{}\".", this.home);
    }

    /**
     * Initialize a bare-bones genome by ID (used by super-classes only).
     *
     * @param genomeId	ID of this genome
     */
    protected Genome(String genomeId) {
        this.id = genomeId;
        setup();
    }

    /**
     * Initialize a bare-bones genome by name (used by super-classes only).
     *
     * @param name		name of this genome
     * @param domain	domain of this genome
     */
    protected Genome(String name, String domain) {
        this.id = null;
        this.name = name;
        this.domain = domain;
        setup();
    }

    /**
     * Construct an empty genome.
     */
    private Genome() { }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the geneticCode
     */
    public int getGeneticCode() {
        return geneticCode;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return the taxonomyId
     */
    public int getTaxonomyId() {
        return taxonomyId;
    }

    /**
     * @return the taxonomy name
     */
    protected String getTaxonomyName() {
        String retVal;
        if (this.lineage == null || this.lineage.length == 0) {
            retVal = String.format("Unknown organism with taxonomic ID %d.", this.taxonomyId);
        } else {
            retVal = this.lineage[this.lineage.length - 1].getName();
        }
        return retVal;
    }

    /**
     * @return a collection of the features in this genome
     */
    public Collection<Feature> getFeatures() {
        return this.features.values();
    }

    /**
     * @return a shuffle-capable list of the protein features in this genome
     */
    public Shuffler<Feature> getPegs() {
        Collection<Feature> featList = this.getFeatures();
        Shuffler<Feature> retVal = new Shuffler<Feature>(featList.size());
        for (Feature feat : featList) {
            if (feat.isProtein()) {
                retVal.add(feat);
            }
        }
        return retVal;
    }

    /**
     * @return a collection of the contigs in this genome
     */
    public Collection<Contig> getContigs() {
        return this.contigs.values();
    }

    /**
     * @return 	the identified feature (or NULL)
     *
     * @param fid	the PATRIC/FIG ID of the desired feature
     */
    public Feature getFeature(String fid) {
        return this.features.get(fid);
    }

    /**
     * @return the DNA string for the specified feature, or an empty string if no DNA is found
     *
     * @param fid	the ID of the feature whose DNA should be returned
     */
    public String getDna(String fid) {
        String retVal = "";
        Feature feat = this.getFeature(fid);
        if (feat != null) {
            Location loc = feat.getLocation();
            if (loc != null) {
                retVal = getDna(loc);
            }
        }
        return retVal;
    }

    /**
     * @return the DNA at the specified location.
     *
     * @param loc	location containing the DNA
     */
    public String getDna(Location loc) {
        String retVal = "";
        if (loc != null) {
            Contig contig = this.getContig(loc.getContigId());
            if (contig != null) {
                for (Region region : loc.getRegions()) {
                    retVal += contig.getDna(region);
                }
                if (loc.getDir() == '-') {
                    retVal = Contig.reverse(retVal);
                }
            }
        }
        return retVal;
    }

    /**
     * @return the contig with the specified ID, or NULL if it does not exist
     *
     * @param contigId	the ID of the desired contig
     */
    public Contig getContig(String contigId) {
        return this.contigs.get(contigId);
    }

    /**
     * @return the number of contigs in this genome
     */
    public int getContigCount() {
        return this.contigs.size();
    }

    @Override
    public String toString() {
        return this.id + " (" + this.name + ")";
    }

    public FeatureList getContigFeatures(String contigId) {
        FeatureList retVal = new FeatureList(this, contigId);
        return retVal;
    }

    /**
     * @return the number of features in this genome
     */
    public int getFeatureCount() {
        return this.features.size();
    }

    /**
     * Add a feature to this genome.
     *
     * @param feat	feature to add
     */
    public void addFeature(Feature feat) {
        this.features.put(feat.getId(), feat);
        feat.setParent(this);
    }

    /**
     * Add a contig to this genome.
     *
     * @param contig	contig to add
     */
    public void addContig(Contig contig) {
        this.contigs.put(contig.getId(), contig);
    }

    /**
     * @return a json object containing the full gto
     */
    public JsonObject toJson() {
        // In case there is old data that we don't support, we start with the original
        // json object we read in.  If there is none, we create it.
        JsonObject retVal = this.gto;
        if (retVal == null) {
            retVal = new JsonObject();
            this.gto = retVal;
        }
        // Start with the scalars.
        retVal.put(GenomeKeys.ID.getKey(), this.id);
        retVal.put(GenomeKeys.SCIENTIFIC_NAME.getKey(), this.name);
        retVal.put(GenomeKeys.DOMAIN.getKey(), this.domain);
        retVal.put(GenomeKeys.GENETIC_CODE.getKey(), this.geneticCode);
        retVal.put(GenomeKeys.NCBI_TAXONOMY_ID.getKey(), this.taxonomyId);
        retVal.put(GenomeKeys.HOME.getKey(), this.home);
        // If we have an SSU rRNA, include it.
        if (this.ssuRna != null)
            retVal.put(GenomeKeys.SSU_RRNA.getKey(), this.ssuRna);
        // If we have source data, include it.
        if (this.source != null) {
            retVal.put(GenomeKeys.SOURCE.getKey(), this.source);
            retVal.put(GenomeKeys.SOURCE_ID.getKey(), this.sourceId);
        }
        // Add the lineage.
        JsonArray jtaxonomy = new JsonArray();
        for (TaxItem taxon : this.lineage) jtaxonomy.add(taxon.toJson());
        retVal.put(GenomeKeys.NCBI_LINEAGE.getKey(), jtaxonomy);
        // Add the contigs.
        JsonArray jcontigs = new JsonArray();
        for (Contig contig : this.getContigs()) jcontigs.add(contig.toJson());
        retVal.put(GenomeKeys.CONTIGS.getKey(), jcontigs);
        // Add the features.
        JsonArray jfeatures = new JsonArray();
        for (Feature feat : this.getFeatures()) jfeatures.add(feat.toJson());
        retVal.put(GenomeKeys.FEATURES.getKey(), jfeatures);
        // Add the close genomes.
        JsonArray jclose = new JsonArray();
        for (CloseGenome close : this.closeGenomes) jclose.add(close.toJson());
        retVal.put(GenomeKeys.CLOSE_GENOMES.getKey(), jclose);
        // Add the events.
        JsonArray jEvents = new JsonArray();
        for (AnalysisEvent event : this.events) jEvents.add(event.toJson());
        retVal.put(GenomeKeys.ANALYSIS_EVENTS.getKey(), jEvents);
        // Add the subsystems.
        JsonArray jsubs = new JsonArray();
        for (SubsystemRow subRow : this.subsystems.values()) jsubs.add(subRow.toJson());
        retVal.put(GenomeKeys.SUBSYSTEMS.getKey(), jsubs);
        // Store the quality object.
        retVal.put(GenomeKeys.QUALITY.getKey(), this.quality);
        // Return the rebuilt GTO.
        return retVal;
    }

    /**
     * Record an analysis event in the GTO.
     *
     * @param tool			name of the tool used
     * @param parameters	array of command-line parameters
     */
    public void recordEvent(String tool, String... parameters) {
        // Record this as an analysis event.
        JsonObject gto = this.toJson();
        // Get the events array.
        JsonArray events = (JsonArray) gto.get("analysis_events");
        if (events == null) {
            events = new JsonArray();
            gto.put("analysis_events", events);
        }
        // Build the parameters.
        JsonArray parms = new JsonArray();
        for (String parm : parameters) parms.add(parm);
        // Build the event.
        JsonObject thisEvent = new JsonObject().putChain("id", UUID.randomUUID().toString())
                .putChain("tool_name", tool)
                .putChain("parameters", parms)
                .putChain("execute_time", System.currentTimeMillis() / 1000.0);
        try {
            thisEvent.put("hostname", InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException e) { }
        events.add(thisEvent);

    }
    /**
     * @return the taxonomic lineage ids for this genome
     */
    public int[] getLineage() {
        int[] retVal = new int[this.lineage.length];
        for (int i = 0; i < this.lineage.length; i++) retVal[i] = this.lineage[i].getId();
        return retVal;
    }

    /**
     * @return an iterator through the taxonomic lineage, in order from smallest to largest
     */
    public Iterator<TaxItem> taxonomy() {
        return new TaxItem.TaxIterator(this.lineage);
    }

    /**
     * Write the internal GTO to the specified file in JSON format.  This is useful
     * if the GTO has been updated.
     *
     * @param outFile	output file
     *
     * @throws IOException
     */
    public void save(File outFile) throws IOException {
        try (PrintWriter gtoStream = new PrintWriter(outFile)) {
            saveToStream(gtoStream);
        }
    }

    /**
     * Write the internal GTO to the specified stream in JSON format.  This is useful
     * if the GTO has been updated.
     *
     * @param outStream		output stream
     *
     * @throws IOException
     */
    public void save(OutputStream outStream) throws IOException {
        try (PrintWriter gtoStream = new PrintWriter(outStream)) {
            saveToStream(gtoStream);
        }
    }

    /**
     * Write the GTO to a stream in json format.
     *
     * @param gtoStream		output writer for the GTO
     *
     * @throws IOException
     */
    private void saveToStream(Writer gtoStream) throws IOException {
        String jsonString = this.toJsonString();
        try {
            Jsoner.prettyPrint(new StringReader(jsonString), gtoStream, "    ", "\n");
        } catch (JsonException e) {
            throw new RuntimeException("Error updating GTO: " + e.toString());
        }
    }

    /**
     * @return a single-string JSON representation of this genome
     */
    public String toJsonString() {
        return Jsoner.serialize(this.toJson());
    }

    /**
     * Store the genome-level data retrieved from the PATRIC API.
     *
     * @param genomeData	genome-level data retrieved from PATRIC
     */
    protected void p3Store(JsonObject genomeData, List<TaxItem> taxRecords) {
        this.id = genomeData.getStringOrDefault(GenomeKeys.GENOME_ID);
        this.name = genomeData.getStringOrDefault(GenomeKeys.GENOME_NAME);
        this.taxonomyId = genomeData.getIntegerOrDefault(GenomeKeys.TAXON_ID);
        // Store the lineage.
        this.lineage = new TaxItem[taxRecords.size()];
        for (int i = 0; i < this.lineage.length; i++) {
             this.lineage[i] = taxRecords.get(i);
        }
        // Compute the domain.
        this.domain = genomeData.getStringOrDefault(GenomeKeys.SUPERKINGDOM);
        // Denote this is a PATRIC genome.
        this.setHome("PATRIC");
    }

    /**
     * Update the genetic code.
     *
     * @param code	code to store
     */
    public void setGeneticCode(int code) {
        this.geneticCode = code;
        for (Contig contig : this.contigs.values())
            contig.setGeneticCode(code);
    }

    /**
     * Store the contigs returned in the specified array/
     *
     * @param contigs	array of contigs read from the PATRIC API
     */
    protected void p3Contigs(Collection<JsonObject> contigs) {
        for (JsonObject contigObj : contigs) {
            Contig contig = new Contig(contigObj, this.geneticCode);
            this.contigs.put(contig.getId(), contig);
        }

    }

    /**
     * @return the close genomes, sorted from closest to furthest within each method
     */
    public SortedSet<CloseGenome> getCloseGenomes() {
        return closeGenomes;
    }

    /**
     * @return the length of the genome in base pairs
     */
    public int getLength() {
        int retVal = 0;
        for (Contig contig : this.contigs.values())
            retVal += contig.length();
        return retVal;
    }

    /**
     * Remove all annotations from this genome.
     */
    public void deAnnotate() {
        // Delete the features.
        this.features.clear();
    }

    /**
     * @return the next available ID number for features of the specified type
     *
     * @param type	feature type of interest
     */
    public int getNextIdNum(String type) {
        // Create a prefix that matches feature IDs of the desired type.
        String prefix = "fig|" + this.id + "." + type + ".";
        // Initialize us to a minimum feature ID.
        String maxFid = prefix + "0";
        // Loop through all the feature IDs.
        for (String fid : this.features.keySet()) {
            if (fid.startsWith(prefix)) {
                // Here we have a feature of the correct type. Keep it if it has
                // the maximum index value.  The strings are identical except for
                // the index value.  A shorter number is always smaller, and
                // otherwise a lexicographic comparison works.
                if (maxFid.length() < fid.length())
                    maxFid = fid;
                else if (maxFid.length() == fid.length() && maxFid.compareTo(fid) < 0)
                    maxFid = fid;
            }
        }
        // The next available index number is one more than the maximum we found.
        // The index number starts after the prefix.
        int retVal = Integer.valueOf(maxFid.substring(prefix.length())) + 1;
        return retVal;
    }

    /**
     * @return a link to a feature in this genome
     *
     * @param fid	feature to link to
     */
    public ContainerTag featureLink(String fid) {
        return this.linker.featureLink(fid);
    }

    /**
     * @return a link to a list of features from this genome
     *
     * @param fidList	list of the feature IDs to link to
     */
    public ContainerTag featureListLink(Collection<String> fidList) {
        return this.linker.featureListLink(fidList);
    }

    /**
     * @return a link to the context view of a feature in this genome
     *
     * @param fid	feature to link to
     */
    public ContainerTag featureRegionLink(String fid) {
        return this.linker.featureRegionLink(fid);
    }

    /**
     * @return a link to this genome's overview page
     */
    public ContainerTag genomeLink() {
        return this.linker.genomeLink(this.id);
    }

    /**
     * @return the home
     */
    public String getHome() {
        return home;
    }

    /**
     * @return TRUE if contig DNA is present, else FALSE
     */
    public boolean hasContigs() {
        boolean retVal = true;
        for (Contig contig : this.contigs.values()) {
            // If this is a nonempty contig but it has no DNA, then DNA is not present.
            if (contig.length() > 0 && contig.getSequence().length() == 0)
                retVal = false;
        }
        return retVal;
    }

    /**
     * @return TRUE if the other genome has the same ID and home
     *
     * @param other		other genome to compare
     */
    public boolean identical(Genome other) {
        return (other != null && this.id.contentEquals(other.id) && this.home.contentEquals(other.home));
    }

    /**
     * Change the ID of a contig.  This also updates all the feature locations.
     *
     * @param contig	contig to update
     * @param contig2Id	new ID for the contig
     */
    public void updateContigId(Contig contig, String contig2Id) {
        String contigId = contig.getId();
        contig.setId(contig2Id);
        for (Feature feat : this.getFeatures()) {
            Location loc = feat.getLocation();
            if (loc.getContigId().contentEquals(contigId))
                feat.getLocation().setContigId(contig2Id);
        }
    }

    /***
     * This method computes the DNA in the ORF containing the specified protein feature.  The portion of the
     * DNA that has been translated into the protein is shown in upper case, and the remainder in lower case.
     * The stop codons are not included.  The DNA is reverse-complemented before being returned.
     *
     * @param fid	ID of the feature of interest
     *
     * @return the desired DNA sequence with the translated portion in upper case
     */
    public String getProteinOrf(String fid) {
        String retVal = null;
        // Get the location of the feature.
        Feature feat = this.getFeature(fid);
        Location protLoc = feat.getLocation();
        Location orfLoc = protLoc.extendToOrf(this);
        // Get the ORF DNA
        retVal = this.getDna(orfLoc).toUpperCase();
        // Compute the prefix length.
        int extra = orfLoc.getLength() - protLoc.getLength();
        // Convert the prefix to lower case.
        if (extra > 0)
            retVal = retVal.substring(0, extra).toLowerCase() + retVal.substring(extra);
        return retVal;
    }

    /**
     * @return the source of the genome (i.e. "GenBank")
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source 	the source of the genome (i.e. "GenBank")
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the ID of the genome in its original source
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @param sourceId 	the ID of the genome in its original source
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * Erase the original GTO so that only the in-memory data is saved.
     */
    public void purify() {
        this.gto = new JsonObject();
    }

    /**
     * Specify the genome ID.  This does not fix feature IDs, so the genome should be basically empty.
     *
     * @param id 	the id of this genome
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param lineage 	the taxonomic lineage for this genome
     */
    public void setLineage(TaxItem[] lineage) {
        this.lineage = lineage;
        this.taxonomyId = lineage[lineage.length - 1].getId();
    }

    /**
     * @param name		the scientific name for this genome
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Save this genome's DNA contigs to the specified file.
     *
     * @param fastaFile		output file
     *
     * @throws IOException
     */
    public void saveDna(File fastaFile) throws IOException {
        try (FastaOutputStream outStream = new FastaOutputStream(fastaFile)) {
            Sequence contigSeq = new Sequence();
            for (Contig contig : this.getContigs()) {
                contigSeq.setLabel(contig.getId());
                contigSeq.setComment(contig.getDescription());
                contigSeq.setSequence(contig.getSequence());
                outStream.write(contigSeq);
            }
        }
    }

    /**
     * Get this genome's DNA as a list of sequences.
     *
     * @return a list of this genomes contig's in the form of sequences
     */
    public List<Sequence> getSequences() {
        List<Sequence> retVal = new ArrayList<Sequence>(this.getContigCount());
        for (Contig contig : this.getContigs()) {
            Sequence seq = new Sequence(contig.getId(), contig.getDescription(), contig.getSequence());
            retVal.add(seq);
        }
        return retVal;
    }

    /**
     * Save this genome's proteins to the specified protein FASTA file.
     *
     * @param fastaFile		output file
     *
     * @throws IOException
     */
    public void savePegs(File fastaFile) throws IOException {
        try (FastaOutputStream outStream = new FastaOutputStream(fastaFile)) {
            Sequence pegSeq = new Sequence();
            for (Feature peg : this.getPegs()) {
                pegSeq.setLabel(peg.getId());
                pegSeq.setComment(peg.getFunction());
                pegSeq.setSequence(peg.getProteinTranslation());
                outStream.write(pegSeq);
            }
        }
    }

    /**
     * Save this genome's features of the specified type to the specified DNA FASTA file.
     *
     * @param fastaFile		output file
     * @param type			type of feature to save
     *
     * @throws IOException
     */
    public void saveFeatures(File fastaFile, String type) throws IOException {
        try (FastaOutputStream outStream = new FastaOutputStream(fastaFile)) {
            Sequence fidSeq = new Sequence();
            for (Feature fid : this.getFeatures()) {
                if (fid.getType().contentEquals(type)) {
                    fidSeq.setLabel(fid.getId());
                    fidSeq.setComment(fid.getFunction());
                    fidSeq.setSequence(this.getDna(fid.getLocation()));
                    outStream.write(fidSeq);
                }
            }
        }
    }

    /**
     * Save this genome's features to the specified DNA FASTA file.
     *
     * @param fastaFile		output file
     *
     * @throws IOException
     */
    public void saveFeatures(File fastaFile) throws IOException {
        try (FastaOutputStream outStream = new FastaOutputStream(fastaFile)) {
            Sequence fidSeq = new Sequence();
            for (Feature fid : this.getFeatures()) {
                fidSeq.setLabel(fid.getId());
                fidSeq.setComment(fid.getFunction());
                fidSeq.setSequence(this.getDna(fid.getLocation()));
                outStream.write(fidSeq);
            }
        }
    }

    /**
     * @return this genome's link object
     */
    public LinkObject getLinker() {
        return this.linker;
    }

    /**
     * @return TRUE if there is a peg contained in the specified location, else FALSE
     *
     * @param loc	location to check
     */
    public boolean isCoding(Location loc) {
        // Denote we have not found a matching peg yet.
        boolean retVal = false;
        // Get an iterator through the features.
        Iterator<Feature> iter = this.features.values().iterator();
        while (! retVal && iter.hasNext()) {
            Feature peg = iter.next();
            retVal = (peg.isProtein() && loc.contains(peg.getLocation()) &&
                    (loc.getBegin() - peg.getLocation().getBegin()) % 3 == 0);
        }
        return retVal;
    }

    /**
     * @param domain 	high-level domain of this genome
     */
    protected void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @param taxonomyId 	taxonomic ID of this genome
     */
    protected void setTaxonomyId(int taxonomyId) {
        this.taxonomyId = taxonomyId;
    }

    /**
     * @return the implementation of the named subsystem, or NULL if it does not exist in this genome
     */
    public SubsystemRow getSubsystem(String name) {
        return this.subsystems.get(name);
    }

    /**
     * @return the list of subsystem implementations for this genome
     */
    public Collection<SubsystemRow> getSubsystems() {
        return this.subsystems.values();
    }

    /**
     * Connect a subsystem implementation to this genome.
     *
     * @param subsystemRow	subsystem to connect
     */
    /* package */ void connectSubsystem(SubsystemRow subsystemRow) {
        this.subsystems.put(subsystemRow.getName(), subsystemRow);
    }

    /**
     * Erase all of the subsystems.
     */
    public void clearSubsystems() {
        this.subsystems.clear();
    }

    /**
     * Remove the specified feature from this genome.  This may cause a subsystem to become invalid.
     * Currently, the only way to fix this is to re-project the genome's subsystems.
     *
     * @param feat	feature to remove
     *
     * @return TRUE if successful, FALSE if a subsystem becomes invalid
     */
    public boolean deleteFeature(Feature feat) {
        String fid = feat.getId();
        boolean retVal = feat.disconnect();
        this.features.remove(fid);
        return retVal;
    }

    /**
     * Remove the specified contig from this genome.  This also deletes all the features on the contig.  It
     * may cause a subsystem to become invalid, in which case the subsystems will have to be re-projected.
     *
     * @param contig	contig to remove
     *
     * @return TRUE if successful, FALSE if a subsystem becomes invalid
     */
    public boolean deleteContig(Contig contig) {
        String contigId = contig.getId();
        boolean retVal = true;
        for (Feature feat : this.getContigFeatures(contigId)) {
            boolean ok = this.deleteFeature(feat);
            if (! ok) retVal = false;
        }
        this.contigs.remove(contigId);
        return retVal;
    }

    /**
     * @return the taxonomy string of this genome.
     */
    public String getTaxString() {
        List<String> names = new ArrayList<String>(this.lineage.length);
        String prev = "cellular organisms";
        // We remove "cellular organisms" at the front, and also skip over blanks and duplicates.
        for (TaxItem item : this.lineage) {
            String name = item.getName();
            if (name != null && ! name.isEmpty() && ! name.contentEquals(prev))
                names.add(name);
            prev = name;
        }
        return StringUtils.join(names, "; ");
    }

    /**
     * @return the feature with the specified function and having the longest protein string
     *
     * @param funDesc	description string for the desired function
     */
    public Feature getByFunction(String funDesc) {
        int protLen = 0;
        Feature retVal = null;
        Function target = new Function(funDesc);
        for (Feature feat : this.getFeatures()) {
            if (target.matches(feat.getPegFunction())) {
                int len = feat.getProteinLength();
                if (len >= protLen) {
                    retVal = feat;
                    protLen = len;
                }
            }
        }
        return retVal;
    }

    /**
     * @return a location object for the specified RefSeq-style location
     *
     * @param Refseq-style location string (contigAccession:begin-end), or NULL if the location is invalid
     */
    public Location computeLocation(String refseqLocation) {
        Location retVal = null;
        // Make sure we have an accession map.
        if (this.accessionMap == null)
            this.buildAccessionMap();
        // Parse the incoming location.
        Matcher m = ACCESSION_LOCATION.matcher(refseqLocation);
        if (m.matches()) {
            String contigId = this.accessionMap.get(m.group(1));
            if (contigId != null) {
                int begin = Integer.valueOf(m.group(2));
                int end = Integer.valueOf(m.group(3));
                retVal = Location.create(contigId, begin, end);
            }
        }
        return retVal;
    }

    /**
     * Create a mapping from RefSeq accession IDs to PATRIC contig IDs
     */
    private void buildAccessionMap() {
        this.accessionMap = new HashMap<String, String>(this.contigs.size());
        // Map each accession number to a contig ID.
        this.contigs.values().stream().filter(x -> StringUtils.isNotEmpty(x.getAccession()))
                .forEach(x -> this.accessionMap.put(x.getAccession(), x.getId()));
    }

    /**
     * @return TRUE if there is quality information in this GTO, else FALSE
     */
    public boolean hasQuality() {
        return this.gto.containsKey("quality");
    }

    /**
     * @return the quality object for this GTO, creating one if none exists yet
     */
    public JsonObject getQuality() {
        return this.quality;
    }

    /**
     * Update the SSU rRNA nucleotide sequence.
     *
     * @param seq	sequence to store (empty string if there is none
     */
    public void setSsuRRna(String seq) {
        this.ssuRna = seq;
    }

    /**
     * @return TRUE if the SSU rRNA is known
     */
    public boolean checkSsuRRna() {
        return (this.ssuRna != null);
    }

    /**
     * @return the SSU rRNA nucleotide sequence, or an empty string if none is present
     */
    public String getSsuRRna() {
        String retVal = this.ssuRna;
        if (retVal == null) {
            // Here we have to search for the sequence.
            this.ssuRna = "";
            for (Feature feat : this.features.values()) {
                // If this is an RNA and it is longer than the current sequence AND it has an SSU rRNA
                // functional assignment, we save its DNA as the SSU rRNA for this genome.
                if (feat.getType().contentEquals("rna") && feat.getLocation().getLength() > this.ssuRna.length() &&
                        isSSURole(feat)) {
                    String ssu = this.getDna(feat.getLocation());
                    // Insure there is not a bad ambiguity run.
                    if (isValidSsuRRna(ssu))
                        this.ssuRna = ssu;
                }
            }
            retVal = this.ssuRna;
        }
        return retVal;
    }

    /**
     * @return TRUE if an SSU is acceptable, FALSE if it is corrupted
     *
     * @param ssu	SSU sequence to check
     */
    public static boolean isValidSsuRRna(String ssu) {
        return ! StringUtils.contains(ssu, BAD_AMBIGUITY_RUN);
    }

    /**
     * @return TRUE if the specified feature's role indicates it is an SSU, else FALSE
     *
     * @param feat		feature in question
     */
    public static boolean isSSURole(Feature feat) {
        return RoleUtilities.SSU_R_RNA.matcher(feat.getPegFunction()).find();
    }

    /**
     * @return a map from each alias to its feature IDs
     */
    public Map<String, Set<String>> getAliasMap() {
        var retVal = new HashMap<String, Set<String>>(this.features.size());
        // Loop through the features.
        for (Map.Entry<String, Feature> featEntry : this.features.entrySet()) {
            String fid = featEntry.getKey();
            var aliases = featEntry.getValue().getAliases();
            for (String alias : aliases) {
                Set<String> aliasFids = retVal.computeIfAbsent(alias, x -> new TreeSet<String>());
                aliasFids.add(fid);
            }
        }
        return retVal;
    }

    /**
     * @return the list of analysis events
     */
    public List<AnalysisEvent> getEvents() {
        return this.events;
    }

    /**
     * Add a new analysis event to this genome.
     *
     * @param event		new event to add
     */
    public void addEvent(AnalysisEvent event) {
        this.events.add(event);
    }

    /**
     * Delete all features, subsystems, and contigs from this genome.  This resets the
     * genome to a blank state, but keeps the principal metadata intact.  We do this
     * the slow way, by deleting the contigs individually.
     */
    public void clear() {
        Collection<Contig> contigList = new ArrayList<Contig>(this.contigs.values());
        for (var contig : contigList)
            this.deleteContig(contig);
        // Clear the SSU rRNA cache.
        this.ssuRna = null;
        // Finally, erase the quality data.
        this.quality = new JsonObject();
    }

    /**
     * This method indicates if the genome is more or less complete.  Currently,
     * we do not have a way to do this for general GTOs.
     *
     * @return TRUE if this is believed to be a complete genome, else FALSE
     */
    public boolean isComplete() {
        return false;
    }

}
