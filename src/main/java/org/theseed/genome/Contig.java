package org.theseed.genome;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.theseed.locations.Region;
import org.theseed.sequence.Sequence;

import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This class implements a contig.  It contains the ID, sequence, and genetic code, and is
 * constructible from a JsonObject.
 *
 * @author Bruce Parrello
 *
 */
public class Contig implements Comparable<Contig> {

    // FIELDS
    /** contig ID */
    private String id;
    /** DNA sequence (not always available) */
    private String sequence;
    /** genetic code for protein translation */
    private int geneticCode;
    /** length of sequence (always available) */
    private int length;
    /** accession key of contig */
    private String accession;
    /** description of contig */
    private String description;
    /** cached copy of reverse complement (not always available) */
    private String rSequence;
    /** string containing nucleic acids */
    private static final String NUCLEOTIDES = "acgtu";
    /** pattern for coverage keywords in the comment */
    private static final Pattern COMMENT_COVG_PATTERN = Pattern.compile("\\b(?:covg|cov|multi|coverage)[= ](\\d+(?:\\.\\d+)?)\\b");
    /** pattern for coverage keywords in the contig ID */
    private static final Pattern LABEL_COVG_PATTERN = Pattern.compile("_(?:coverage|covg|cov)_(\\d+(?:\\.\\d+)?)(?:_|\\b)");
    /** default coverage to use if none can be computed */
    private static final double DEFAULT_COVERAGE = 50.0;


    /** This enum defines the keys used and their default values.
     */
    public static enum ContigKeys implements JsonKey {
        // GTO FIELDS
        ID("con.001"),
        DNA(""),
        GENETIC_CODE(11),
        GENBANK_LOCUS(null),
        // SHARED FIELDS
        LENGTH(0),
        ACCESSION(""),
        // PATRIC FIELDS
        SEQUENCE_ID("con.001"),
        SEQUENCE(""),
        DESCRIPTION(""),
        // GENBANK_LOCUS FIELDS
        COMMENT("")
        ;

        private final Object m_value;

        ContigKeys(final Object value) {
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
     * Create the contig from the incoming JsonObject.  Note the DNA is converted to lower case,
     * and the length is computed if it is not present.
     *
     * @param contigObj	JsonObject read into memory during genome input
     */
    public Contig(JsonObject contigObj) {
        this.id = contigObj.getStringOrDefault(ContigKeys.ID);
        this.sequence = contigObj.getStringOrDefault(ContigKeys.DNA);
        // Sometimes we get an explicit NULL and we have to convert to an empty string.
        if (this.sequence != null)
            this.sequence = this.sequence.toLowerCase();
        else
            this.sequence = "";
        this.geneticCode = contigObj.getIntegerOrDefault(ContigKeys.GENETIC_CODE);
        this.length = contigObj.getIntegerOrDefault(ContigKeys.LENGTH);
        if (this.length == 0)
            this.length = this.sequence.length();
        JsonObject locusObj = contigObj.getMapOrDefault(ContigKeys.GENBANK_LOCUS);
        if (locusObj != null) {
            this.accession = locusObj.getStringOrDefault(ContigKeys.ACCESSION);
            this.description = locusObj.getStringOrDefault(ContigKeys.COMMENT);
        } else {
            this.accession = "";
            this.description = "";
        }
    }

    /**
     * Create the contig from a JsonObject retrieved from the PATRIC data API.
     *
     * @param contigObj		a JsonObject containing contig data from PATRIC
     * @param gcode	the genetic code for this contig
     */
    public Contig(JsonObject contigObj, int code) {
        this.id = contigObj.getStringOrDefault(ContigKeys.SEQUENCE_ID);
        this.sequence = contigObj.getStringOrDefault(ContigKeys.SEQUENCE).toLowerCase();
        if (this.sequence.length() > 0) {
            this.length = this.sequence.length();
        } else {
            this.length = contigObj.getIntegerOrDefault(ContigKeys.LENGTH);
        }
        this.geneticCode = code;
        this.accession = contigObj.getString(ContigKeys.ACCESSION);
        this.description = contigObj.getString(ContigKeys.DESCRIPTION);
    }

    /**
     * Create a simple contig from its basic elements.
     *
     * @param contigId	the ID of the contig
     * @param sequence	the sequence of the contig
     * @param code		the genetic code of the contig
     */
    public Contig(String contigId, String sequence, int code) {
        this.id = contigId;
        this.sequence = sequence.toLowerCase();
        this.geneticCode = code;
        this.length = sequence.length();
        this.accession = "";
        this.description = "";
    }

    /**
     * Create a simple contig from and ID and length.
     *
     * @param contigId	the ID of the contig
     * @param len		the length of the contig
     * @param code		the genetic code of the contig
     */
    public Contig(String contigId, int len, int code) {
        this.id = contigId;
        this.geneticCode = code;
        this.length = len;
        this.accession = "";
        this.description = "";
        this.sequence = "";
    }

    /**
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return the sequence
     */
    public String getSequence() {
        return this.sequence;
    }

    /**
     * @return the reverse compliment of the sequence
     */
    public String getRSequence() {
        // Note we cache the reverse complement in case we need it again.
        if (this.rSequence == null)
            this.rSequence = reverse(this.sequence);
        return this.rSequence;
    }

    /**
     * @return the geneticCode
     */
    public int getGeneticCode() {
        return this.geneticCode;
    }

    /**
     * @return the length of the contig
     */
    public int length() {
        return this.length;
    }

    /**
     * @return the DNA string for the specified region on this contig
     *
     * @param region	the region whose DNA is desired
     */
    public String getDna(Region region) {
        int left = region.getLeft() - 1;
        if (left < 0) {
            left = 0;
        }
        int right = region.getRight();
        if (right < 0 || right > this.sequence.length()) {
            right = this.sequence.length();
        }
        return this.sequence.substring(left, right);
    }

    /**
     * @return the reverse complement of a DNA string
     *
     * @param dna	the DNA string to reverse (must be lower case)
     */
    public static String reverse(String dna) {
        int n = dna.length();
        StringBuilder retVal = new StringBuilder(n);
        for (int i = n - 1; i >= 0; i--) {
            char rev;
            char norm = dna.charAt(i);
            rev = switch (norm) {
                case 'a' -> 't';
                case 'c' -> 'g';
                case 'g' -> 'c';
                case 't', 'u' -> 'a';
                default -> 'n';
            };
            retVal.append(rev);
        }
        return retVal.toString();
    }

    /**
     * @return a json object for this contig
     */
    public JsonObject toJson() {
        JsonObject retVal = new JsonObject();
        retVal.put(ContigKeys.ID.getKey(), this.id);
        retVal.put(ContigKeys.GENETIC_CODE.getKey(), this.geneticCode);
        retVal.put(ContigKeys.DNA.getKey(), this.sequence);
        retVal.put(ContigKeys.LENGTH.getKey(), this.length);
        if (! this.accession.isEmpty() || ! this.description.isEmpty()) {
            JsonObject genbank_locus = new JsonObject();
            genbank_locus.put(ContigKeys.ACCESSION.getKey(), this.accession);
            genbank_locus.put(ContigKeys.COMMENT.getKey(), this.description);
            retVal.put(ContigKeys.GENBANK_LOCUS.getKey(), genbank_locus);
        }
        return retVal;
    }

    /**
     * Update the ID of this contig.
     *
     * @param contig2Id		new ID to store.
     */
    protected void setId(String contig2Id) {
        this.id = contig2Id;
    }

    /**
     * Update the genetic code stored in this contig.
     *
     * @param code	new genetic code
     */
    public void setGeneticCode(int code) {
        this.geneticCode = code;
    }

    /**
     * @return the accession ID
     */
    public String getAccession() {
        return accession;
    }

    /**
     * @param accession 	new accession ID
     */
    public void setAccession(String accession) {
        this.accession = accession;
    }

    /**
     * @return the description of the contig
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description 	new contig description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param sequence 	new DNA sequence to store
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
        this.length = sequence.length();
        this.rSequence = null;
    }

    /**
     * Contigs are sorted by ID.
     */
    @Override
    public int compareTo(Contig o) {
        return this.id.compareTo(o.id);
    }

    /**
     * Split a DNA sequence into pieces, removing ambiguity characters.
     *
     * @param seq	input DNA sequence
     */
    public static List<String> cleanParts(String seq) {
        List<String> retVal;
        String normalized = seq.toLowerCase();
        int iBreak = StringUtils.indexOfAnyBut(normalized, "acgt");
        // We special-case no ambiguity characters because that is the most common case.
        if (iBreak < 0)
            retVal = List.of(normalized);
        else {
            // Now we have to break up the sequence.
            retVal = new ArrayList<>(5);
            if (iBreak > 0)
                retVal.add(normalized.substring(0, iBreak));
            final int n = normalized.length();
            int start = iBreak + 1;
            // Now we are positioned on a bad character.
            while (start < n) {
                // Loop through the bad characters here.
                while (start < n && NUCLEOTIDES.indexOf(normalized.charAt(start)) < 0)
                    start++;
                // Loop through the good characters here.
                iBreak = start;
                while (iBreak < n && NUCLEOTIDES.indexOf(normalized.charAt(iBreak)) >= 0)
                    iBreak++;
                // Output the substring here, then start over.
                if (start < iBreak)
                    retVal.add(normalized.substring(start, iBreak));
                start = iBreak + 1;
            }
        }
        return retVal;
    }

    /**
     * Compute coverage for an assembled contig.  We look in the label and then the comment.  If nothing works,
     * we use a default.
     *
     * @param	sequence representing the assembled contig
     *
     * @return the estimated coverage
     */
    public static double computeCoverage(Sequence contig) {
        double coverage = DEFAULT_COVERAGE;
        final String contigId = contig.getLabel();
        Matcher m = LABEL_COVG_PATTERN.matcher(contigId);
        if (m.find())
            coverage = Double.parseDouble(m.group(1));
        else {
            m = COMMENT_COVG_PATTERN.matcher(contig.getComment());
            if (m.find())
                coverage = Double.parseDouble(m.group(1));
        }
        return coverage;
    }

    /**
     * Delete the sequence from the contig to save memory.
     */
    public void clearSequence() {
        this.sequence = "";
        this.rSequence = "";
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Contig other = (Contig) obj;
        return Objects.equals(this.accession, other.accession) && Objects.equals(this.description, other.description)
                && this.geneticCode == other.geneticCode && Objects.equals(this.id, other.id)
                && this.length == other.length && Objects.equals(this.sequence, other.sequence);
    }

}
