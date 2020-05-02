/**
 *
 */
package org.theseed.proteins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * This object is used to translate DNA into proteins.  The constructor specifies a genetic code, and the
 * resulting object converts DNA regions to protein strings.
 *
 * @author Bruce Parrello
 *
 */
public class DnaTranslator {

    /** genetic code 11 translation table */
    @SuppressWarnings("serial")
    private static final HashMap<String, String> GENETIC_CODE_11 = new HashMap<String, String>() {{
        put("aaa","K"); put("aac","N"); put("aag","K"); put("aat","N"); put("aca","T");
        put("acc","T"); put("acg","T"); put("act","T"); put("aga","R"); put("agc","S");
        put("agg","R"); put("agt","S"); put("ata","I"); put("atc","I"); put("atg","M");
        put("att","I"); put("caa","Q"); put("cac","H"); put("cag","Q"); put("cat","H");
        put("cca","P"); put("ccc","P"); put("ccg","P"); put("cct","P"); put("cga","R");
        put("cgc","R"); put("cgg","R"); put("cgt","R"); put("cta","L"); put("ctc","L");
        put("ctg","L"); put("ctt","L"); put("gaa","E"); put("gac","D"); put("gag","E");
        put("gat","D"); put("gca","A"); put("gcc","A"); put("gcg","A"); put("gct","A");
        put("gga","G"); put("ggc","G"); put("ggg","G"); put("ggt","G"); put("gta","V");
        put("gtc","V"); put("gtg","V"); put("gtt","V"); put("taa","*"); put("tac","Y");
        put("tag","*"); put("tat","Y"); put("tca","S"); put("tcc","S"); put("tcg","S");
        put("tct","S"); put("tga","*"); put("tgc","C"); put("tgg","W"); put("tgt","C");
        put("tta","L"); put("ttc","F"); put("ttg","L"); put("ttt","F");
    }};

    /** array of start codon sets by genetic code */
    public static final CodonSet[] STARTS = new CodonSet[] { null,
            /*  1 */ new CodonSet("ttg", "ctg", "atg"),
            /*  2 */ new CodonSet("att", "atc", "ata", "atg", "gtg"),
            /*  3 */ new CodonSet("ata", "atg", "gtg"),
            /*  4 */ new CodonSet("ttg", "ctg", "atg"),
            null, null, null, null, null, null,
            /* 11 */ new CodonSet("ttg", "gtg", "atg")
            };

    /** array of stop codon sets by genetic code */
    public static final CodonSet[] STOPS = new CodonSet[] { null,
            /*  1 */ new CodonSet("taa", "tag", "tga"),
            /*  2 */ new CodonSet("taa", "tag", "aga", "agg"),
            /*  3 */ new CodonSet("taa", "tag"),
            /*  4 */ new CodonSet("taa", "tag"),
            null, null, null, null, null, null,
            /* 11 */ new CodonSet("taa", "tag", "tga")
            };


    // FIELDS
    private Map<String, String> translationMap;
    private int geneticCode;
    private CodonSet starts;
    private CodonSet stops;


    /**
     * Construct the genetic code mapping for a specified translation code.
     *
     * @param gc	genetic code to use
     */
    public DnaTranslator(int gc) {
        this.geneticCode = gc;
        if (gc == 1 || gc == 11) {
            // Use the default map.
            this.translationMap = GENETIC_CODE_11;
        } else {
            // Here we make a small modification to the standard map.  First, we need
            // a safety copy.
            this.translationMap = new HashMap<String, String>(GENETIC_CODE_11);
            switch (gc) {
            case 2:
                this.translationMap.put("aga", "*");
                this.translationMap.put("agg", "*");
                this.translationMap.put("ata", "M");
                this.translationMap.put("tga", "W");
                break;
            case 3:
                this.translationMap.put("ata", "M");
                this.translationMap.put("ctt", "T");
                this.translationMap.put("ctc", "T");
                this.translationMap.put("ctg", "T");
                this.translationMap.put("tga", "W");
                break;
            case 4:
                this.translationMap.put("tga", "W");
                break;
            default:
                throw new IllegalArgumentException("Unsupported genetic code " + gc + ".");
            }
        }
        this.starts = STARTS[gc];
        this.stops = STOPS[gc];
    }

    /**
     * Translate the specified region from the specified position (1-based) and the specified length
     * in the specified DNA.
     *
     * @param dna		dna sequence to translate (must be lower case)
     * @param start		position to start translation (1-based)
     * @param len		number of characters to translate
     *
     * @return the translated protein string
     */
    public String translate(String dna, int start, int len) {
        // Create a buffer in which to build the result.
        StringBuilder retVal = new StringBuilder(len / 3);
        // Figure out how far we can go.
        int end = start + len;
        // Insure we don't pass the end of the string.
        if (end > dna.length()) end = dna.length();
        // Subtract 2 to leave space for a full triplet.
        end -= 2;
        // Now loop through the DNA.
        for (int i = start; i <= end; i += 3) {
            retVal.append(this.aa(dna, i));
        }
        return retVal.toString();
    }

    /**
     * Translate the specified coding region from the specified position (1-based) and
     * the specified length in the specified DNA.  Since this is a coding region, the
     * start will be modified.
     *
     * @param dna		dna sequence to translate (must be lower case)
     * @param start		position to start translation (1-based)
     * @param len		number of characters to translate
     *
     * @return the translated protein string
     */
    public String pegTranslate(String dna, int start, int len) {
        String first;
        String codon = StringUtils.substring(dna, start - 1, start + 2);
        switch (codon) {
        case "gtg" :
        case "ttg" :
            first = "M";
            break;
        default :
            first = this.aa(codon);
        }
        String retVal = first + this.translate(dna, start + 3, len - 3);
        return retVal;
    }

    /**
     * Translate the specified coding region string.  Since this is a coding region, the
     * start will be modified.
     *
     * @param dna		dna sequence to translate (must be lower case)
     *
     * @return the translated protein string
     */
    public String pegTranslate(String dna) {
        return this.pegTranslate(dna, 1, dna.length());
    }

    /**
     * @return the amino acid for a single codon
     *
     * @param codon		triplet to translate
     */
    public String aa(String codon) {
        String retVal = this.translationMap.get(codon);
        if (retVal == null) retVal = "X";
        return retVal;
    }

    /**
     * @return the amino acid at a specified dna position
     *
     * @param dna	dna string (must be lower-case)
     * @param pos	position (1-based) in the string
     */
    public String aa(String dna, int pos) {
        String codon = StringUtils.substring(dna, pos - 1, pos + 2);
        return aa(codon);
    }

    /**
     * Translate an entire DNA string.
     *
     * @param dna	DNA string to translate
     *
     * @return the protein translation of the whole DNA string
     */
    public String translate(String dna) {
        return this.translate(dna, 1, dna.length());
    }

    /**
     * @return the genetic code for this translator
     */
    public int getGeneticCode() {
        return geneticCode;
    }

    /**
     * @return TRUE if the codon at the specified position in the DNA string is a start
     *
     * @param dna	dna string (must be lower-case)
     * @param pos	position (1-based) in the string
     */
    public boolean isStart(String dna, int pos) {
        return this.starts.contains(dna, pos);
    }

    /**
     * @return TRUE if the codon at the specified position in the DNA string is a stop
     *
     * @param dna	dna string (must be lower-case)
     * @param pos	position (1-based) in the string
     */
    public boolean isStop(String dna, int pos) {
        return this.stops.contains(dna, pos);
    }


    /**
     * Build a protein operon from a DNA sequence.  We will begin from the last stop and
     * work our way backward.
     *
     * @param dnaString	DNA string to separate into operons
     *
     * @return a list of protein strings representing the operon
     */
    public List<String> operonFrom(String dnaString) {
        // Estimate the number of proteins and create the output list.
        int size = dnaString.length() / 1000 + 1;
        List<String> retVal = new ArrayList<String>(size);
        // Start at the last codon.
        int p = dnaString.length() - 3;
        while (p >= 0) {
            // Find the last stop from this position.
            while (p >= 0 && ! this.isStop(dnaString, p)) p--;
            // If we found the stop, search for the first start in the ORF.
            if (p >= 0) {
                int stop = p;
                p -= 3;
                int start = 0;
                while (p >= 0 && ! this.isStop(dnaString, p)) {
                    if (this.isStart(dnaString, p)) start = p;
                    p -= 3;
                }
                if (start == 0) {
                    // This is not a coding ORF.  Keep searching for a stop.
                    p = stop - 1;
                } else {
                    // This is a coding ORF.  compute the protein and then
                    // search for the next ORF.
                    String protein = this.pegTranslate(dnaString, start, stop - start);
                    retVal.add(protein);
                    p = start - 3;
                }
            }
        }
        return retVal;
    }

}
