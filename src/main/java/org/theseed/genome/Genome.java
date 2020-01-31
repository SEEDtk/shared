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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.theseed.locations.Location;
import org.theseed.locations.Region;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

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
    private File inFile;
    private JsonObject gto;
    private String[] lineage;
    private TreeSet<CloseGenome> closeGenomes;


    /** This is an empty list to use as a default intermediate value for cases where the contigs or
     * features are missing.
     */
    private static final Collection<JsonObject> noEntries = new ArrayList<JsonObject>();


    /** This enum defines the keys used and their default values.
     */
    public enum GenomeKeys implements JsonKey {
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
        KINGDOM("Bacteria")
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
        // Save the input file name.
        this.inFile = inFile;
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
        // Extract the lineage IDs.
        Collection<JsonArray> lineageArray = this.gto.getCollectionOrDefault(GenomeKeys.NCBI_LINEAGE);
        this.lineage = lineageArray.stream().map(x -> x.getString(1)).toArray(n -> new String[n]);
        // Pull in the close genomes.
        this.closeGenomes = new TreeSet<CloseGenome>();
        Collection<JsonObject> closeList = this.gto.getCollectionOrDefault(GenomeKeys.CLOSE_GENOMES);
        for (JsonObject close : closeList)
            this.closeGenomes.add(new CloseGenome(close));
        // Now we need to process the features and contigs.
        Collection<JsonObject> featureList = this.gto.getCollectionOrDefault(GenomeKeys.FEATURES);
        features = new HashMap<String, Feature>();
        for (JsonObject feat : featureList) {
            Feature feature = new Feature(feat);
            features.put(feature.getId(), feature);
        }
        Collection<JsonObject> contigList = this.gto.getCollectionOrDefault(GenomeKeys.CONTIGS);
        contigs = new HashMap<String, Contig>();
        for (JsonObject contigObj : contigList) {
            Contig contig = new Contig(contigObj);
            contigs.put(contig.getId(), contig);
        }
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
        // Create empty maps for features and contigs.
        this.features = new HashMap<String, Feature>();
        this.contigs = new HashMap<String, Contig>();
        // Denote there is no input file.
        this.inFile = null;
    }

    /**
     * Initialize a bare-bones genome (used by super-classes only).
     *
     * @param genomeId	ID of this genome
     */
    protected Genome(String genomeId) {
        this.id = genomeId;
        // Create empty maps for features and contigs.
        this.features = new HashMap<String, Feature>();
        this.contigs = new HashMap<String, Contig>();
        // Denote there is no input file.
        this.inFile = null;
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
            if (feat.getType().equals("CDS")) {
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
     * @return the name of the file from which the genome was read
     */
    public File getFile() {
        return this.inFile;
    }

    /**
     * @return the original json object containing the full gto
     */
    public JsonObject getJson() {
        return this.gto;
    }

    /**
     * @return the taxonomic lineage ids for this genome
     */
    public String[] getLineage() {
        return this.lineage;
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
        String jsonString = Jsoner.serialize(this.gto);
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
    protected void p3Store(JsonObject genomeData) {
        this.id = genomeData.getStringOrDefault(GenomeKeys.GENOME_ID);
        this.name = genomeData.getStringOrDefault(GenomeKeys.GENOME_NAME);
        this.taxonomyId = genomeData.getIntegerOrDefault(GenomeKeys.TAXON_ID);
        // Store the lineage.
        JsonArray taxonomy = genomeData.getCollectionOrDefault(GenomeKeys.TAXON_LINEAGE_IDS);
        this.lineage = new String[taxonomy.size()];
        for (int i = 0; i < this.lineage.length; i++) {
            this.lineage[i] = taxonomy.getString(i);
        }
        // Compute the domain.
        this.domain = genomeData.getStringOrDefault(GenomeKeys.KINGDOM);
    }

    /**
     * Update the genetic code.
     *
     * @param code	code to store
     */
    protected void setGeneticCode(int code) {
        this.geneticCode = code;
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

}
