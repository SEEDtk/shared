/**
 *
 */
package org.theseed.proteins;

import java.util.HashMap;
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

    // FIELDS
    private Map<String, String> translationMap;


    /**
     * Construct the genetic code mapping for a specified translation code.
     *
     * @param gc	genetic code to use
     */
    public DnaTranslator(int gc) {
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
        String codon = StringUtils.substring(dna, 0, 3);
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
    public Object translate(String dna) {
        return this.translate(dna, 1, dna.length());
    }

}
