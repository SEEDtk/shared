/**
 *
 */
package org.theseed.genomes;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.theseed.locations.Location;
import org.theseed.locations.Region;

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


    /** This is an empty list to use as a default intermediate value for cases where the contigs or
     * features are missing.
     */
    private static final Collection<JsonObject> noEntries = new ArrayList<JsonObject>();


    /** This enum defines the keys used and their default values.
     */
    private enum GenomeKeys implements JsonKey {
        ID("0"),
        SCIENTIFIC_NAME("unknown organism"),
        NCBI_TAXONOMY_ID(2),
        GENETIC_CODE(11),
        DOMAIN("Bacteria"),
        CONTIGS(noEntries),
        FEATURES(noEntries);

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
     * @throws IOException
     * @throws NumberFormatException
     */
    public Genome(String fileName) throws NumberFormatException, IOException {
        // Get a reader for the named file.
        FileReader reader = new FileReader(fileName);
        // Read the genome from the file.
        JsonObject gto;
        try {
            gto = (JsonObject) Jsoner.deserialize(reader);
        } catch (JsonException e) {
            throw new IOException("Error reading JSON data.", e);
        }
        id = gto.getStringOrDefault(GenomeKeys.ID);
        name = gto.getStringOrDefault(GenomeKeys.SCIENTIFIC_NAME);
        taxonomyId = gto.getIntegerOrDefault(GenomeKeys.NCBI_TAXONOMY_ID);
        geneticCode = gto.getIntegerOrDefault(GenomeKeys.GENETIC_CODE);
        domain = gto.getStringOrDefault(GenomeKeys.DOMAIN);

        // Now we need to process the features and contigs.
        Collection<JsonObject> featureList = gto.getCollectionOrDefault(GenomeKeys.FEATURES);
        features = new HashMap<String, Feature>();
        for (JsonObject feat : featureList) {
            Feature feature = new Feature(feat);
            features.put(feature.getId(), feature);
        }
        Collection<JsonObject> contigList = gto.getCollectionOrDefault(GenomeKeys.CONTIGS);
        contigs = new HashMap<String, Contig>();
        for (JsonObject contigObj : contigList) {
            Contig contig = new Contig(contigObj);
            contigs.put(contig.getId(), contig);
        }

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
                Contig contig = this.getContig(loc.getContigId());
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

    @Override
    public String toString() {
        return this.id + " (" + this.name + ")";
    }

    public Collection<Feature> getContigFeatures(String contigId) {
        ArrayList<Feature> retVal = new ArrayList<Feature>(this.getFeatureCount());
        for (Feature feat : this.features.values()) {
            if (feat.getLocation().getContigId().equals(contigId)) {
                retVal.add(feat);
            }
        }
        retVal.sort(new Feature.LocationComparator());
        return retVal;
    }

    /**
     * @return the number of features in this genome
     */
    public int getFeatureCount() {
        return this.features.size();
    }


}
