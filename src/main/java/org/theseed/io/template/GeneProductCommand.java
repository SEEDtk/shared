/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.theseed.genome.Genome;
import org.theseed.io.TabbedLineReader;
import org.theseed.io.TabbedLineReader.Line;
import org.theseed.proteins.Function;
import org.theseed.utils.ParseFailureException;

/**
 * This command takes a column name as input and presumes the column contains a gene product string.
 * The string is parsed into descriptive text and output.
 *
 * @author Bruce Parrello
 *
 */
public class GeneProductCommand implements ITemplateCommand {

    // FIELDS
    /** index of the column containing the gene product string */
    private int prodColIdx;
    /** index of the column containing the feature type code */
    private int typeColIdx;
    /** map of protein abbreviations to names for tRNA */
    private static final Map<String, String> AMINO_ACIDS = Map.ofEntries(
            new AbstractMap.SimpleEntry<String, String>("Ala", "Alanine"),
            new AbstractMap.SimpleEntry<String, String>("Arg", "Arginine"),
            new AbstractMap.SimpleEntry<String, String>("Asn", "Asparagine"),
            new AbstractMap.SimpleEntry<String, String>("Asp", "Aspartic acid"),
            new AbstractMap.SimpleEntry<String, String>("Cys", "Cysteine"),
            new AbstractMap.SimpleEntry<String, String>("Glu", "Glutamic acid"),
            new AbstractMap.SimpleEntry<String, String>("Gln", "Glutamine"),
            new AbstractMap.SimpleEntry<String, String>("Gly", "Glycine"),
            new AbstractMap.SimpleEntry<String, String>("His", "Histidine"),
            new AbstractMap.SimpleEntry<String, String>("Ile", "Isoleucine"),
            new AbstractMap.SimpleEntry<String, String>("Leu", "Leucine"),
            new AbstractMap.SimpleEntry<String, String>("Lys", "Lysine"),
            new AbstractMap.SimpleEntry<String, String>("Met", "Methionine"),
            new AbstractMap.SimpleEntry<String, String>("Phe", "Phenylalanine"),
            new AbstractMap.SimpleEntry<String, String>("Pro", "Proline"),
            new AbstractMap.SimpleEntry<String, String>("Ser", "Serine"),
            new AbstractMap.SimpleEntry<String, String>("Thr", "Threonine"),
            new AbstractMap.SimpleEntry<String, String>("Trp", "Tryptophan"),
            new AbstractMap.SimpleEntry<String, String>("Tyr", "Tyrosine"),
            new AbstractMap.SimpleEntry<String, String>("Val", "Valine"));
    /** split pattern for rRNAs */
    private static final Pattern RNA_SPLITTER = Pattern.compile("(?:\\s+##|;)\\s+");
    /** RNA string finder for rRNAs */
    private static final Pattern RNA_NAMER = Pattern.compile("\\b(?:ribosomal\\s+)?r?RNA\\b", Pattern.CASE_INSENSITIVE);
    /** tRNA product parser */
    private static final Pattern T_RNA_PARSER = Pattern.compile("tRNA-(\\w{3,3})(?:-([A-Z]{3,3}))?");


    /**
     * Construct a gene-product translation command.
     *
     * @param columnName	index (1-based) or name of source column
     * @param inStream		main input stream
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public GeneProductCommand(String columnName, TabbedLineReader inStream) throws IOException, ParseFailureException {
        String[] tokens = StringUtils.split(columnName, ':');
        if (tokens.length != 2)
            throw new ParseFailureException("$product command requires exactly two column names-- the product column and the feature type column.");
        this.prodColIdx = inStream.findField(tokens[0]);
        this.typeColIdx = inStream.findField(tokens[1]);
    }

    @Override
    public int getIfEffect() {
        return 0;
    }

    @Override
    public boolean requiresIf() {
        return false;
    }

    @Override
    public String translate(LineTemplate template, Line line) {
        String product = line.get(this.prodColIdx);
        String type = line.get(this.typeColIdx);
        String retVal;
        // Each type has a different approach.
        switch (type) {
        case "tRNA" :
            retVal = this.processTRna(product);
            break;
        case "rRNA" :
            retVal = this.processRRna(product);
            break;
        case "misc_RNA" :
            retVal = this.processMiscRna(product);
            break;
        case "CDS" :
            retVal = this.processProtein(product);
            break;
        default :
            retVal = this.processOther(type);
            break;
        }
        return retVal;
    }

    /**
     * @return a string describing this miscellaneous feature type
     *
     * @param type	type of feature to describe
     */
    private String processOther(String type) {
        // Convert underscores to spaces.
        String typeDesc = StringUtils.replaceChars(type, '_', ' ');
        // Return the description.
        return "This feature is " + prefixArticle(typeDesc) + ".";
    }

    /**
     * @return the specified phrase with an indefinite article in front of it.
     *
     * @param phrase	input phrase
     */
    public static String prefixArticle(String phrase) {
        // Compute the article.
        String article;
        switch (Character.toLowerCase(phrase.charAt(0))) {
        case 'a' :
        case 'e' :
        case 'i' :
        case 'o' :
        case 'u' :
        case '8' :
            article = "an";
            break;
        default :
            article = "a";
            break;
        }
        return article + " " + phrase;
    }

    /**
     * @return a description of a miscellaneous RNA feature
     *
     * @param product	supplied product string
     */
    private String processMiscRna(String product) {
        String retVal;
        if (StringUtils.isBlank(product)) {
            // Here we have no product string.
            retVal = "This feature's product is a miscellaneous RNA.";
        } else {
            // Here we have a product string to use.
            retVal = "This featwure's product is a miscellaneous RNA believed to be " + product + ".";
        }
        return retVal;
    }

    /**
     * This method produces the description of a ribosomal RNA.
     *
     * @param product	supplied product string
     *
     * @return a sentence describing the ribosomal RNA
     */
    private String processRRna(String product) {
        // Use the product to determine the type of ribosomal RNA.
        String retVal;
        if (StringUtils.isBlank(product)) {
            retVal = "This feature's product is an unknown ribosomal RNA.";
        } else if (Genome.LSU_R_RNA.matcher(product).find()) {
            // Here we have an LSU.
            retVal = "This feature's product is a large subunit ribosomal RNA.";
        } else if (Genome.SSU_R_RNA.matcher(product).find()) {
            // Here we have the venerable SSU.
            retVal = "This feature's product is a 16S small subunit ribosomal RNA.";
        } else {
            // Break up the product and return the longest portion.
            String[] pieces = RNA_SPLITTER.split(product);
            String longestPiece = pieces[0];
            for (int i = 1; i < pieces.length; i++) {
                if (pieces[i].length() > longestPiece.length())
                    longestPiece = pieces[i];
            }
            // Look for a substring that names the product as an RNA.
            var m = RNA_NAMER.matcher(longestPiece);
            if (! m.find()) {
                // Here there is no such substring.
                retVal = "This feature's product is a ribosomal RNA of type " + longestPiece + ".";
            } else {
                // Here we must replace the substring with the full version of the name.
                String tail = (m.end() + 1 < longestPiece.length() ?
                        longestPiece.substring(m.end() + 1) : "");
                retVal = "This feature's product is " +
                        prefixArticle(longestPiece.substring(0, m.start()) + "ribosomal RNA" + tail);
            }
        }
        return retVal;
    }

    /**
     * This method produces the description of a transfer RNA.
     *
     * @param product	relevant product string
     *
     * @return a text description of the transfer RNA
     */
    private String processTRna(String product) {
        String retVal;
        if (StringUtils.isBlank(product)) {
            retVal = "This feature's product is an unknown type of transfer RNA.";
        } else {
            // Parse the product.  If we have a match, group 1 is the amino acid abbreviation, and if group 2 exists, it
            // is the codon.
            var m = T_RNA_PARSER.matcher(product);
            if (! m.matches()) {
                // No match: we have an unparseable tRNA.
                retVal = "This feature's product is a type of tranfser RNA described as " + product + ".";
            } else {
                // Here we have the standard format.  Translate the amino acid name.
                String aa = AMINO_ACIDS.get(m.group(1));
                if (aa == null)
                    aa = "an unknown amino acid " + m.group(1);
                String codon = "";
                if (m.group(2) != null)
                    codon = " from codon " + m.group(2);
                retVal = "This feature's product is a transfer RNA for " + aa + codon + ".";
            }
        }
        return retVal;
    }

    /**
     * This is the most complicated product translator-- protein-coding regions.  Protein-coding regions can have comments,
     * multi-functional roles, and EC and TC numbers.
     *
     * @param product	protein product string
     *
     * @return a text description of the product
     */
    private String processProtein(String product) {
        // First, we strip off the comment.
        String productBody = Function.commentFree(product);
        // Now, we split into ambiguous possibilities.
        // TODO code for processProtein
        return productBody;
    }


}
