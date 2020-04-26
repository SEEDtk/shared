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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.theseed.locations.Location;
import org.theseed.locations.Region;
import org.theseed.reports.LinkObject;
import org.theseed.sequence.FastaOutputStream;
import org.theseed.sequence.Sequence;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import j2html.tags.DomContent;

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

    private String id;
    private String name;
    private int geneticCode;
    private String domain;
    private int taxonomyId;
    private Map<String, Feature> features;
    private Map<String, Contig> contigs;
    private JsonObject gto;
    private TaxItem[] lineage;
    private SortedSet<CloseGenome> closeGenomes;
    private String home;
    private LinkObject linker;
    private String source;
    private String sourceId;



    /** This is an empty list to use as a default intermediate value for cases where the contigs or
     * features are missing.
     */
    private static final Collection<JsonObject> noEntries = new ArrayList<JsonObject>();


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
        // PATRIC fields
        GENOME_ID("0"),
        GENOME_NAME("unknown organism"),
        TAXON_LINEAGE_IDS(noEntries),
        TAXON_ID(2),
        KINGDOM("Bacteria"),
        SOURCE(null),
        SOURCE_ID(null),
        HOME("none")
        ;

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
        id = this.gto.getStringOrDefault(GenomeKeys.ID);
        name = this.gto.getStringOrDefault(GenomeKeys.SCIENTIFIC_NAME);
        taxonomyId = this.gto.getIntegerOrDefault(GenomeKeys.NCBI_TAXONOMY_ID);
        geneticCode = this.gto.getIntegerOrDefault(GenomeKeys.GENETIC_CODE);
        domain = this.gto.getStringOrDefault(GenomeKeys.DOMAIN);
        source = this.gto.getStringOrDefault(GenomeKeys.SOURCE);
        sourceId = this.gto.getStringOrDefault(GenomeKeys.SOURCE_ID);
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
        contigs = new HashMap<String, Contig>();
        for (JsonObject contigObj : contigList) {
            Contig contig = new Contig(contigObj);
            contigs.put(contig.getId(), contig);
        }
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
        // Create empty maps for features, close genomes, and contigs.
        this.features = new HashMap<String, Feature>();
        this.contigs = new HashMap<String, Contig>();
        this.closeGenomes = new TreeSet<CloseGenome>();
        // Create a blank original GTO.
        this.gto = new JsonObject();
        // Denote the genome has no home.
        this.setHome("none");
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
     * @return a collection of the features in this genome
     */
    public Collection<Feature> getFeatures() {
        return this.features.values();
    }

    /**
     * @return a collection of the protein features in this genome
     */
    public Collection<Feature> getPegs() {
        Collection<Feature> featList = this.getFeatures();
        ArrayList<Feature> retVal = new ArrayList<Feature>(featList.size());
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
        Contig contig = this.getContig(loc.getContigId());
        for (Region region : loc.getRegions()) {
            retVal += contig.getDna(region);
        }
        if (loc.getDir() == '-') {
            retVal = Contig.reverse(retVal);
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
        // json object we read it.  If there is none, we create it.
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
    public void update(File outFile) throws IOException {
        try (PrintWriter gtoStream = new PrintWriter(outFile)) {
            updateToStream(gtoStream);
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
    public void update(OutputStream outStream) throws IOException {
        try (PrintWriter gtoStream = new PrintWriter(outStream)) {
            updateToStream(gtoStream);
        }
    }

    /**
     * Write the GTO to a stream in json format.
     *
     * @param gtoStream		output writer for the GTO
     *
     * @throws IOException
     */
    private void updateToStream(Writer gtoStream) throws IOException {
        String jsonString = Jsoner.serialize(this.toJson());
        try {
            Jsoner.prettyPrint(new StringReader(jsonString), gtoStream, "    ", "\n");
        } catch (JsonException e) {
            throw new RuntimeException("Error updating GTO: " + e.getMessage());
        }
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
        this.domain = genomeData.getStringOrDefault(GenomeKeys.KINGDOM);
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
     * @return a link to a feature in this genome
     *
     * @param fid	feature to link to
     */
    public DomContent featureLink(String fid) {
        return this.linker.featureLink(fid);
    }

    /**
     * @return a link to a list of features from this genome
     *
     * @param fidList	list of the feature IDs to link to
     */
    public DomContent featureListLink(Collection<String> fidList) {
        return this.linker.featureListLink(fidList);
    }

    /**
     * @return a link to the context view of a feature in this genome
     *
     * @param fid	feature to link to
     */
    public DomContent featureRegionLink(String fid) {
        return this.linker.featureRegionLink(fid);
    }

    /**
     * @return a link to this genome's overview page
     */
    public DomContent genomeLink() {
        return this.linker.genomeLink(this.id);
    }

    /**
     * @return the home
     */
    public String getHome() {
        return home;
    }

    /**
     * @return the ID of the reference genome used to create this one from a metagenome, or NULL
     * 		   if there is none
     */
    public String getBinRefGenomeId() {
        String retVal = null;
        for (CloseGenome closeSpec : this.closeGenomes) {
            if (closeSpec.getMethod().contentEquals("bins_generate"))
                retVal = closeSpec.genomeId;
        }
        return retVal;
    }

    /**
     * @return the weighted coverage of this genome, if it was generated from a metagenome, else 0
     */
    public double getBinCoverage() {
        double retVal = 0.0;
        for (CloseGenome closeSpec : this.closeGenomes) {
            if (closeSpec.getMethod().contentEquals("bins_generate"))
                retVal = closeSpec.getCloseness();
        }
        return retVal;
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
     * Save this genome's proteins to the specified file.
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
     * @return this genome's link object
     */
    public LinkObject getLinker() {
        return this.linker;
    }

}
