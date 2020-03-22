/**
 *
 */
package org.theseed.genome;

import java.util.Iterator;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This class represents a single taxonomic level for a genome.
 *
 * @author Bruce Parrello
 *
 */
public class TaxItem {

    // FIELDS
    /** taxonomic ID */
    private int id;
    /** rank level */
    private String rank;
    /** taxonomy name */
    private String name;

    public static enum TaxItemKeys implements JsonKey {
        TAXON_ID(2),
        TAXON_RANK("none"),
        TAXON_NAME("hidden");

        private final Object m_value;

        TaxItemKeys(final Object value) {
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
     * Iterator through a taxonomy array, from smallest group to largest
     */
    public static class TaxIterator implements Iterator<TaxItem> {

        /** lineage from the genome */
        private TaxItem[] lineage;
        /** last item returned */
        private int pos;

        public TaxIterator(TaxItem[] lineage) {
            this.lineage = lineage;
            this.pos = lineage.length;
        }

        @Override
        public boolean hasNext() {
            return (this.pos > 0);
        }

        @Override
        public TaxItem next() {
            this.pos--;
            return this.lineage[this.pos];
        }

    }

    /**
     * Create the taxon from a JSON array.
     *
     * @param json		array of taxonomic elements
     */
    public TaxItem(JsonArray json) {
        this.name = json.getString(0);
        this.id = json.getInteger(1);
        this.rank = json.getString(2);
    }

    /**
     * Create the taxon from a JSON object.
     *
     * @param json		json object for a taxonomy item (from PATRIC)
     */
    public TaxItem(JsonObject json) {
        this.name = json.getStringOrDefault(TaxItemKeys.TAXON_NAME);
        this.id = json.getIntegerOrDefault(TaxItemKeys.TAXON_ID);
        this.rank = json.getStringOrDefault(TaxItemKeys.TAXON_RANK);
    }

    /**
     * Create the taxon from the name, ID, and rank.
     *
     * @param id		taxonomic group ID
     * @param name		taxonomic group name
     * @param rank		taxonomic group rank
     */
    public TaxItem(int id, String name, String rank) {
        this.name = name;
        this.id = id;
        this.rank = rank;
    }

    /**
     * @return a JSON array for this taxonomy item
     */
    public JsonArray toJson() {
        return new JsonArray().addChain(this.name).addChain(this.id).addChain(this.rank);
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the rank
     */
    public String getRank() {
        return rank;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
}
