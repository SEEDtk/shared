/**
 *
 */
package org.theseed.genome;

import com.github.cliftonlabs.json_simple.JsonKey;

/**
 * Enumeration for shared JSON keys in the Genome quality member.
 *
 * @author Bruce Parrello
 *
 */
public enum QualityKeys implements JsonKey {
    GENOME_LENGTH(0),
    CONTIGS(0),
    PLFAM_CDS_RATIO(0.0),
    COARSE_CONSISTENCY(0.0),
    CONTAMINATION(100.0),
    HYPOTHETICAL_CDS_RATIO(100.0),
    COMPLETENESS(0.0),
    CDS_RATIO(0.0),
    FINE_CONSISTENCY(0.0),
    COMPLETENESS_GROUP(""),
    HAS_SSU_RNA(false),
    SCORE(0.0),
    HAS_SEED(false),
    MOSTLY_GOOD(false),
    BIN_COVERAGE(0.0),
    EVAL_GOOD(false),
    BIN_REF_GENOME(null);

    /** default value */
    private Object value;

    private QualityKeys(Object value) {
        this.value = value;
    }

    @Override
    public String getKey() {
        return this.name().toLowerCase();
    }

    @Override
    public Object getValue() {
        return this.value;
    }

}
