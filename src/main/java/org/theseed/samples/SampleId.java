/**
 *
 */
package org.theseed.samples;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class represents a sample ID.  A sample ID consists of 10 to 11 identification fields separated by underscores.
 * they are sorted field by field, with one of the fields being interpreted numerically.
 *
 * @author Bruce Parrello
 *
 */
public class SampleId implements Comparable<SampleId> {

    // FIELDS
    /** array of segments */
    private String[] fragments;
    /** time point */
    private double timePoint;
    /** index of the strain column */
    public static final int STRAIN_COL = 0;
    /** index of the operon column */
    public static final int OPERON_COL = 2;
    /** index of the ASD mode column */
    public static final int ASD_COL = 4;
    /** index of the insertion column */
    public static final int INSERT_COL = 5;
    /** index of the deletion column */
    public static final int DELETE_COL = 6;
    /** number of fragments in the strain ID */
    public static final int STRAIN_SIZE = 7;
    /** array index of pseudo-numeric component */
    public static final int TIME_COL = 8;
    /** array index of the medium ID */
    public static final int MEDIA_COL = 9;
    /** number of guaranteed fields */
    public static final int NORMAL_SIZE = 10;
    /** index of the induction column */
    public static final int INDUCE_COL = 7;
    /** index of column containing replicate flag */
    public static final int REP_COL = NORMAL_SIZE;
    /** map of sample fragments for each plasmid code */
    private static final Map<String, String[]> PLASMID_MAP = Stream.of(
            new AbstractMap.SimpleEntry<>("6-4-2", StringUtils.split("D_TasdA1_P_asdD", '_')),
            new AbstractMap.SimpleEntry<>("2-1-1", StringUtils.split("D_Tasd_P_asdD", '_')),
            new AbstractMap.SimpleEntry<>("6-4-3", StringUtils.split("D_TasdA_P_asdD", '_')),
            new AbstractMap.SimpleEntry<>("pfb6-4-2", StringUtils.split("D_TasdA1_P_asdD", '_')),
            new AbstractMap.SimpleEntry<>("pfb6-4-2c", StringUtils.split("D_TasdA1_P_asdD", '_')),
            new AbstractMap.SimpleEntry<>("pwt2-1-1", StringUtils.split("D_Tasd_P_asdD", '_')),
            new AbstractMap.SimpleEntry<>("pfb6-4-3", StringUtils.split("D_TasdA_P_asdD", '_')),
            new AbstractMap.SimpleEntry<>("277-14", StringUtils.split("0_TA1_C_asdO", '_'))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    /** map of mis-spelled protein names */
    private static final Map<String, String> PROTEIN_ERRORS = Stream.of(
            new AbstractMap.SimpleEntry<>("rthA", "rhtA"),
            new AbstractMap.SimpleEntry<>("ppyc", "pyc"),
            new AbstractMap.SimpleEntry<>("lysCC", "lysC"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    /** default plasmid coding */
    private static final String[] PLASMID_DEFAULT = new String[] { "0", "0", "0", "asdO" };
    /** map of strain numbers to strain IDs */
    private static final Map<String, String> HOST_MAP = Stream.of(
            new AbstractMap.SimpleEntry<>("277", "7"), new AbstractMap.SimpleEntry<>("926", "M"),
            new AbstractMap.SimpleEntry<>("278", "21278"), new AbstractMap.SimpleEntry<>("823", "30823"),
            new AbstractMap.SimpleEntry<>("319", "30319"), new AbstractMap.SimpleEntry<>("593", "21593"),
            new AbstractMap.SimpleEntry<>("316", "30316"), new AbstractMap.SimpleEntry<>("317", "30317"),
            new AbstractMap.SimpleEntry<>("318", "30318"), new AbstractMap.SimpleEntry<>("30318", "30318"),
            new AbstractMap.SimpleEntry<>("21278", "21278"), new AbstractMap.SimpleEntry<>("30823", "30823"),
            new AbstractMap.SimpleEntry<>("30319", "30319"), new AbstractMap.SimpleEntry<>("21593", "21593"),
            new AbstractMap.SimpleEntry<>("30316", "30316"), new AbstractMap.SimpleEntry<>("30317", "30317")
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    /** map of strain IDs for non-plasmid strains */
    private static final Map<String, String[]> CHROMO_MAP = Stream.of(
            new AbstractMap.SimpleEntry<>("277-14", StringUtils.split("7_0_TA1_C_asdO", '_')),
            new AbstractMap.SimpleEntry<>("277wt1", StringUtils.split("7_0_T_C_asdO", '_')),
            new AbstractMap.SimpleEntry<>("277w1", StringUtils.split("7_0_T_C_asdO", '_')),
            new AbstractMap.SimpleEntry<>("926-44", StringUtils.split("M_0_T_C_asdO", '_')),
            new AbstractMap.SimpleEntry<>("926-41", StringUtils.split("M_0_T_C_asdO", '_')),
            new AbstractMap.SimpleEntry<>("926fb1", StringUtils.split("M_0_TA1_C_asdO", '_'))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    /** set of invalid deletion proteins */
    protected static final Set<String> BAD_DELETES = new TreeSet<String>(Arrays.asList("asd", "thrABC"));
    /** fragment descriptions */
    public static final String[] FRAGMENT_DESCRIPTIONS = new String[] { "host", "original thrABC", "core thr operon",
            "insert method", "asd status", "insertions", "deletions", "IPTG", "time", "medium" };
    /** RNA filename pattern */
    protected static final Pattern RNA_SUFFIX = Pattern.compile("(?:rep\\d+_)?(S\\d+)_+(?:L\\d+_)?R[12]_+001\\.fastq");
    /** delete glitch pattern */
    private static final Pattern DELETE_GLITCH = Pattern.compile("([^_]+)_(D[a-z]{3,}.+)");
    /** PTAC glitch pattern */
    private static final Pattern PTAC_GLITCH = Pattern.compile("(^[^_]+)(ptac-.+)");
    /** time specification pattern */
    private static final Pattern TIME_PATTERN = Pattern.compile("_(\\d+)([p_\\-\\.]_?5)?_?hrs?");
    /** plasmid specification patter */
    private static final Pattern PLASMID_PATTERN = Pattern.compile("_(?:pfb)?\\d[_.]\\d[_.]\\d");
    /** obsolete plasmid prefixes */
    private static final Pattern PLASMID_PREFIX = Pattern.compile("^p?ph");
    /** list of strange protein names (length != 4) */
    private static final String[] STRANGE_PROTEINS = new String[] { "ppc", "pyc", "zwf", "pntab", "aceba", "tdh" };
    /** default time point */
    public static String DEFAULT_TIME = "9";

    /**
     * Construct a sample ID from an ID string.
     *
     * @param sampleData 	ID string
     */
    public SampleId(String sampleData) {
        this.fragments = StringUtils.split(sampleData, '_');
        parseTimeString();
    }

    /**
     * Sort the inserts and deletes to normalize the sample ID.  To facilitate
     * use in streams and construction, this operation returns the object itself.
     */
    public SampleId normalizeSets() {
        Set<String> deletes = this.getDeletes();
        if (deletes.size() > 1)
            this.fragments[DELETE_COL] =
                    deletes.stream().sorted().collect(Collectors.joining("D", "D", ""));
        Set<String> inserts = this.getInserts();
        if (inserts.size() > 1)
            this.fragments[INSERT_COL] =
                    inserts.stream().sorted().collect(Collectors.joining("-"));
        return this;
    }

    /**
     * Compute the time point from the time fragment of the sample ID.
     */
    private void parseTimeString() {
        String timeString = StringUtils.replaceChars(fragments[TIME_COL], 'p', '.');
        if (timeString.contentEquals("ML") || timeString.contentEquals("X"))
            this.timePoint = Double.NaN;
        else
            this.timePoint = Double.valueOf(timeString);
    }

    /**
     * Construct a blank sample ID.
     */
    private SampleId() {
        this.fragments = new String[NORMAL_SIZE];
        this.timePoint = Double.NaN;
    }

    /**
     * Construct a new sample ID copied from an old one.
     *
     * @param oldId		sample ID to copy
     */
    public SampleId(SampleId oldId) {
        this.fragments = oldId.fragments.clone();
        this.timePoint = oldId.timePoint;
    }

    /**
     * Construct a sample ID from old-style sample information.
     *
     * @param strain	old-style strain name
     * @param time		time point
     * @param iptg		TRUE if IPTG induction was used, else FALSE
     * @param medium	ID of the medium
     *
     * @return the new Sample ID, or NULL if it is invalid
     */
    public static SampleId translate(String strain, double time, boolean iptg, String medium) {
        SampleId retVal = new SampleId();
        retVal.fragments = new String[NORMAL_SIZE];
        // Erase the leading NRRL (if any).
        strain = StringUtils.removeStartIgnoreCase(strain, "nrrl ");
        // Break the strain name into pieces.
        String[] pieces = StringUtils.split(strain);
        // Remove deletes from the host name and add them to the master delete string.
        StringBuffer deleteString = new StringBuffer(40);
        String parts[] = StringUtils.split(pieces[0], "dD", 2);
        if (parts.length > 1) {
            pieces[0] = parts[0];
            deleteString.append("D" + parts[1]);
        }
        // The first piece is the host.
        retVal.parseHost(pieces[0]);
        // Now we loop through the modifiers.  A modifier can be a plasmid specification, a deletion,
        // or an insert.  A leading plus sign is removed.  Note that the term "plasmid" is obsolete,
        // as some plasmids are actually direct modification to the chromosome.  There is, alas, too much
        // use of the word to let go now.
        String insert = "000";
        for (int i = 1; i < pieces.length; i++) {
            String piece = pieces[i].toLowerCase();
            if (piece.charAt(0) == 'd') {
                // Here we have a deletion.  Add it to the delete string.
                deleteString.append(piece);
            } else if (piece.contentEquals("plasmid")) {
                // The word "plasmid" is just a comment.
            } else {
                // Remove any leading plus sign.
                if (piece.charAt(0) == '+')
                    piece = piece.substring(1);
                // Fix periods.  A plasmid can be specified with periods or hyphens, and we
                // prefer hyphens.
                piece = StringUtils.replaceChars(piece, '.', '-');
                insert = retVal.parseModifier(insert, piece);
            }
        }
        // Make the 277 adjustment.
        if (pieces[0].contentEquals("277") && retVal.fragments[3].contentEquals("0"))
            retVal.fragments[3] = "A";
        // Now process the deletions.
        String deletions = deleteString.toString();
        if (deletions.isEmpty())
            retVal.fragments[DELETE_COL] = "D000";
        else {
            // Here we have real deletes to process.
            String fixedDeleteSpec = fixDeletes(deletions);
            retVal.fragments[DELETE_COL] = fixedDeleteSpec;
        }
        // Next we process the protein insert.
        retVal.fragments[INSERT_COL] = insert;
        // Now we save the time.
        retVal.timePoint = time;
        String timeString;
        if (Double.isNaN(time))
            timeString = "ML";
        else {
            timeString = String.format("%1.1f", retVal.timePoint);
            timeString = StringUtils.removeEnd(timeString, ".0");
            timeString = StringUtils.replaceChars(timeString, '.', 'p');
        }
        retVal.fragments[TIME_COL] = timeString;
        // Next the IPTG flag.
        retVal.fragments[INDUCE_COL] = (iptg ? "I" : "0");
        // Finally the medium.
        retVal.fragments[MEDIA_COL] = medium;
        return retVal;
    }

    /**
     * Translate a new-style strain name to a sample ID.
     *
     * @param strain		new-style strain name
     * @param timePoint		time string
     *
     * @return the sample ID to use
     */
    public static SampleId translate(String strain, String timePoint) {
        String[] parts = sampleMatch(strain, timePoint);
        SampleId retVal = parseParts(parts);
        return retVal;
    }

    /**
     * This method fixed up deletions.  These are hell to process. The basic strategy is to eat
     * a "d", skip three, push to the next "d", and repeat.  "asd" and "thrabc" are
     * automatically removed.  All characters after the first three are uppercased.  Everything
     * else is lower case.
     *
     * @param deletions		incoming deletion specifier
     *
     * @return a corrected deletion specifier
     */
    private static String fixDeletes(String deletions) {
        List<String> deletes = new ArrayList<String>(11);
        deletions = deletions.toLowerCase();
        int pos = 1;
        while (pos < deletions.length()) {
            int end = pos + 3;
            while (end < deletions.length() && deletions.charAt(end) != 'd') end++;
            String delete = StringUtils.substring(deletions, pos, pos+3) +
                    StringUtils.substring(deletions, pos+3, end).toUpperCase();
            // We have to deal with some messy stuff.  One of the deletion proteins is occasionally
            // mis-spelled, and two are redundant, because they are expressed elswhere in the ID.
            if (! BAD_DELETES.contains(delete)) {
                if (PROTEIN_ERRORS.containsKey(delete))
                    delete = PROTEIN_ERRORS.get(delete);
                deletes.add(delete);
            }
            pos = end + 1;
        }
        String retVal = "D000";
        if (deletes.size() > 0)
            retVal = "D" + StringUtils.join(deletes, 'D');
        return retVal;
    }

    /**
     * Convert an RNA sequence file name to a sample ID.  Note that we lose a lot of information here, of which the read type (1 or 2)
     * is the most important.  The final sample ID may also need to have a "rep" identifier added.
     *
     * @param rnaFile	file containing the RNA sequence data
     *
     * @return the sample identified by the RNA sequence file name, or NULL if the file name is invalid
     */
    public static SampleId translate(File rnaFile) {
        SampleId retVal = null;
        String name = rnaFile.getName();
        if (! name.startsWith("Blank")) {
            String[] parts = rnaMatch(name);
            if (parts != null) {
                retVal = parseParts(parts);
            }
        }
        return retVal;
    }

    /**
     * Parse the parts computed by one of the match methods to create a sample ID
     *
     * @param parts		components of an old-style sample name-- [strain, time point, IPTG, sample number, modifiers...]
     *
     * @return the appropriate sample ID
     */
    private static SampleId parseParts(String[] parts) {
        SampleId retVal;
        // Here we have a valid name.  First, separate out the deletes.
        String strainId = parts[0];
        String deletes = "D000";
        int deleteLoc = strainId.indexOf('D');
        if (deleteLoc >= 0) {
            deletes = fixDeletes(StringUtils.substring(strainId, deleteLoc));
            strainId = StringUtils.substring(strainId, 0, deleteLoc);
        }
        // Parse the host.
        retVal = new SampleId();
        retVal.parseHost(strainId);
        // Denote that so far there is no insert.
        String insert = "000";
        // Run through the modifiers.
        for (int i = 4; i < parts.length; i++) {
            String modifier = parts[i];
            insert = retVal.parseModifier(insert, modifier);
        }
        // If no plasmid on a 277, we add an A in the location slot.
        if (retVal.fragments[0].equals("7") && retVal.fragments[3].equals("0"))
            retVal.fragments[3] = "A";
        // Store the IPTG flag.
        retVal.fragments[INDUCE_COL] = parts[2];
        // Store the time point.
        retVal.fragments[TIME_COL] = parts[1];
        // Store the constant columns.
        retVal.fragments[MEDIA_COL] = "M1";
        retVal.fragments[INSERT_COL] = insert;
        retVal.fragments[DELETE_COL] = deletes;
        // Compute the numeric time point.
        retVal.parseTimeString();
        return retVal;
    }

    /**
     * Parse a modifier segment.  Here the modifier must be an insert or a plasmid.
     *
     * @param insert	current insertion protein string
     * @param modifier	modifier to parse
     *
     * @return new insertion protein string
     */
    private String parseModifier(String insert, String modifier) {
        // Remove the old pPH prefix.
        modifier = RegExUtils.removeFirst(modifier, PLASMID_PREFIX);
        String[] plasmidInfo = PLASMID_MAP.get(modifier);
        if (plasmidInfo != null)
            System.arraycopy(plasmidInfo, 0, this.fragments, 1, plasmidInfo.length);
        else switch (modifier) {
        case "ptac-thrABC" :
        case "ptac-thrabc" :
        case "ptacthrABC" :
        case "ptacthrabc" :
            this.fragments[2] = "TA1";
            this.fragments[3] = "C";
            break;
        case "ptac-asd" :
        case "ptacasd" :
            this.fragments[4] = "asdT";
            break;
        default :
            // Here we have an insert.  Split out the pieces.  This is brutal parsing since there are no delimiters.
            String[] prots = splitProteins(modifier);
            for (String prot : prots) {
                if (insert.contentEquals("000"))
                    insert = prot;
                else
                    insert = insert + "-" + prot;
            }
        }
        return insert;
    }

    /**
     * Split an undelimited protein list into the individual proteins.  Protein names are always length
     * 4, except for the ones in STRANGE_PROTEINS.
     *
     * @param modifier		modifier to be split into proteins
     *
     * @return a list of the protein IDs found
     */
    private static String[] splitProteins(String modifier) {
        List<String> list = new ArrayList<String>(4);
        String lcModifier = modifier.toLowerCase();
        int p = 0;
        while (p < lcModifier.length()) {
            // Check for a strange protein.
            int i = 0;
            while (i < STRANGE_PROTEINS.length && ! lcModifier.substring(p).startsWith(STRANGE_PROTEINS[i])) i++;
            // Compute the length of the current protein.
            int end = p + (i < STRANGE_PROTEINS.length ? STRANGE_PROTEINS[i].length() : 4);
            // Add it to the output list.
            list.add(StringUtils.substring(lcModifier, p, end));
            p = end;
        }
        // Fix the casing and the spelling errors.
        String[] retVal = list.stream().map(x -> PROTEIN_ERRORS.getOrDefault(x, x))
                .map(x -> x.substring(0, 3) + StringUtils.substring(x, 3).toUpperCase()).toArray(String[]::new);
        return retVal;
    }

    /**
     * Parse a host specification.
     *
     * @param strainId	strain identifier to parse (with deletes removed)
     */
    private void parseHost(String strainId) {
        String host = HOST_MAP.get(strainId);

        if (host == null) {
            // If the host is not found, check for an artificial host, which contains operon adjustments built in.
            String[] plasmidInfo = CHROMO_MAP.get(strainId);
            if (plasmidInfo == null)
                throw new IllegalArgumentException("Invalid host name \"" + strainId + "\".");
            else {
                // Copy the full host/operon info to the sample ID.
                System.arraycopy(plasmidInfo, 0, this.fragments, 0, plasmidInfo.length);
            }
        } else {
            // Here we have a normal host.
            this.fragments[0] = host;
            String[] plasmidInfo = PLASMID_DEFAULT;
            System.arraycopy(plasmidInfo, 0, this.fragments, 1, plasmidInfo.length);
        }
    }

    /**
     * Extract the sample number from an RNA file name.
     *
     * @param rnaFile	RNA file name
     *
     * @return the sample number ("S" followed by one or more digits)
     */
    public static String getSampleNumber(File rnaFile) {
        String name = rnaFile.getName();
        String[] parts = rnaMatch(name);
        String retVal = null;
        if (parts != null)
            retVal = parts[3];
        return retVal;
    }

    /**
     * Increment this sample ID.  This involves increasing the replicate number.
     */
    public void increment() {
        if (this.fragments.length == NORMAL_SIZE) {
            String[] newFragments = new String[NORMAL_SIZE + 1];
            System.arraycopy(this.fragments, 0, newFragments, 0, NORMAL_SIZE);
            newFragments[REP_COL] = "rep1";
            this.fragments = newFragments;
        } else {
            int repNum = Integer.valueOf(this.fragments[REP_COL].substring(3));
            this.fragments[REP_COL] = String.format("rep%d", repNum + 1);
        }
    }

    /**
     * Parse an RNA file name.
     *
     * @param fileName		base part of the file name to parse
     *
     * @return the components of the file name-- [strain, time point, IPTG, sample number, modifiers...]--
     * 		   or NULL if the file name is invalid
     */
    protected static String[] rnaMatch(String fileName) {
        // Fix some common spelling errors.
        Matcher m;
        String reducedName = fixRnaName(fileName);
        // Remove the IPTG flag (if any).
        String iptgFlag;
        if (StringUtils.contains(reducedName, "_IPTG")) {
            iptgFlag = "I";
            reducedName = StringUtils.remove(reducedName, "_IPTG");
        } else if (StringUtils.contains(reducedName, "_plus")) {
            iptgFlag = "I";
            reducedName = StringUtils.remove(reducedName, "_plus");
        } else
            iptgFlag = "0";
        // Remove the time specification.
        String timePoint = DEFAULT_TIME;
        m = TIME_PATTERN.matcher(reducedName);
        if (m.find()) {
            timePoint = m.group(1);
            if (m.group(2) != null)
                timePoint += "p5";
            reducedName = reducedName.substring(0, m.start()) + reducedName.substring(m.end());
        }
        // Strip off the RNA suffix.  If this fails, we fail the whole match.
        String[] retVal = null;
        m = RNA_SUFFIX.matcher(reducedName);
        if (m.find()) {
            reducedName = reducedName.substring(0, m.start());
            String sampleNum = m.group(1);
            // Now split the file name into pieces.
            String[] parts = reducedName.split("_+");
            retVal = new String[parts.length + 3];
            retVal[0] = parts[0];
            retVal[1] = timePoint;
            retVal[2] = iptgFlag;
            retVal[3] = sampleNum;
            for (int i = 1; i < parts.length; i++)
                retVal[i+3] = parts[i];
        }
        return retVal;
    }

    /**
     * Parse an RNA sample name string.
     *
     * @param sampleName	name of the sample
     * @param timePoint		time string
     *
     * @return the components of the sample name-- [strain, time point, IPTG, sample number, modifiers...]
     */
    protected static String[] sampleMatch(String sampleName, String timePoint) {
        // Convert spaces to underscores.
        String reducedName = StringUtils.replaceChars(sampleName, ' ', '_');
        // Remove bad periods.
        reducedName = StringUtils.replace(reducedName, "._", "_");
        // Perform standard fixups.
        reducedName = fixRnaName(reducedName);
        // Remove the IPTG flag (if any).
        String iptgFlag;
        if (StringUtils.contains(reducedName, "_+")) {
            iptgFlag = "I";
            reducedName = StringUtils.remove(reducedName, "_+");
        } else
            iptgFlag = "0";
        // Search for a time point override.
        Matcher m = TIME_PATTERN.matcher(reducedName);
        if (m.find()) {
            timePoint = m.group(1);
            if (m.group(2) != null)
                timePoint += "p5";
            reducedName = reducedName.substring(0, m.start()) + reducedName.substring(m.end());
        }
        // Now split the name into pieces.
        String[] parts = reducedName.split("_+");
        String[] retVal = new String[parts.length + 3];
        retVal[0] = parts[0];
        retVal[1] = timePoint;
        retVal[2] = iptgFlag;
        retVal[3] = "XX";
        for (int i = 1; i < parts.length; i++)
            retVal[i+3] = parts[i];
        return retVal;
    }

    /**
     * @return a fixed-up version of an RNA file name.
     *
     * @param fileName	RNA file name or strain string
     */
    private static String fixRnaName(String fileName) {
        String retVal = StringUtils.replaceOnce(fileName, "pta-", "ptac-");
        retVal = StringUtils.replaceOnce(retVal, "D_lysC", "DlysC");
        retVal = StringUtils.replaceOnce(retVal, "926_lysC", "926DlysC");
        retVal = StringUtils.replace(retVal, "Dtd_h", "Dtdh");
        // Fix the delete glitch.  This is caused by the weird untranslatable unicode character.
        Matcher m = DELETE_GLITCH.matcher(retVal);
        if (m.matches())
            retVal = m.group(1) + m.group(2);
        // If there are two S-numbers, only keep the first one.
        retVal = retVal.replaceAll("_S(\\d+)_S\\d+_", "_S$1_");
        // Fix the PTAC glitch.  Sometimes the leading space is missing.
        m = PTAC_GLITCH.matcher(retVal);
        if (m.matches())
            retVal = m.group(1) + "_" + m.group(2);
        // Fix the numbered plasmids.  We need to replace the underscores/periods with hyphens.
        m = PLASMID_PATTERN.matcher(retVal);
        if (m.find()) {
            String plasmid = StringUtils.substring(retVal, m.start() + 1, m.end());
            retVal = retVal.substring(0, m.start() + 1) +
                    StringUtils.replaceChars(plasmid, "._", "--") +
                    retVal.substring(m.end());
        }
        return retVal;
    }

    @Override
    public int compareTo(SampleId o) {
        int retVal = 0;
        for (int i = 0; retVal == 0 && i < NORMAL_SIZE; i++) {
            switch (i) {
            case TIME_COL:
                retVal = Double.compare(this.timePoint, o.timePoint);
                break;
            case DELETE_COL:
                retVal = SampleId.setCompare(this.getDeletes(), o.getDeletes());
                break;
            case INSERT_COL:
                retVal = SampleId.setCompare(this.getInserts(), o.getInserts());
                break;
            default:
                retVal = this.fragments[i].compareTo(o.fragments[i]);
            }
        }
        // Handle the optional 11th slot.
        if (retVal == 0) {
            String thisRep = (this.fragments.length > NORMAL_SIZE ? this.fragments[NORMAL_SIZE] : "");
            String oRep = (o.fragments.length > NORMAL_SIZE ? o.fragments[NORMAL_SIZE] : "");
            retVal = thisRep.compareTo(oRep);
        }
        return retVal;
    }

    /**
     * Compare two protein sets.
     *
     * @param set1 		first set to compare
     * @param set2 		second set compare
     *
     * @return a negative value if the first set is less, positive if it is more, 0 if they
     * 		   are the same
     */
    private static int setCompare(Set<String> set1, Set<String> set2) {
        // Smaller sets compare less.
        int retVal = set1.size() - set2.size();
        if (retVal == 0) {
            List<String> set1S = listifySet(set1);
            List<String> set2S = listifySet(set2);
            for (int i = 0; retVal == 0 && i < set1.size(); i++)
                retVal = set1S.get(i).compareTo(set2S.get(i));
        }
        return retVal;
    }

    /**
     * @return a sorted list based on the input set
     *
     * @param set1		input set to convert to a list
     */
    private static List<String> listifySet(Set<String> set1) {
        return set1.stream().sorted().collect(Collectors.toList());
    }

    /**
     * @return the strain portion of the sample ID
     */
    public String toStrain() {
        return StringUtils.join(this.fragments, '_', 0, STRAIN_SIZE);
    }

    /**
     * @return the sample ID without the time point
     */
    public String toTimeless() {
        return this.toStrain() + "_" + this.fragments[INDUCE_COL];
    }

    /**
     * @return the string representative of the sample ID
     */
    public String toString() {
        return StringUtils.join(this.fragments, '_');
    }

    /**
     * @return the sample ID without the replicate number
     */
    public String repBaseId() {
        return StringUtils.join(this.fragments, '_', 0, REP_COL);
    }

    /**
     * @return the deletion set for this sample
     */
    public Set<String> getDeletes() {
        String deletes = this.fragments[DELETE_COL];
        Set<String> retVal = parseDeletes(deletes);
        return retVal;
    }

    /**
     * @return the set of deleted proteins in a delete string
     *
     * @param deletes	delete string to parse
     */
    public static Set<String> parseDeletes(String deletes) {
        Set<String> retVal = new TreeSet<String>();
        if (! deletes.contentEquals("D000")) {
            String[] parts = StringUtils.split(deletes, 'D');
            for (String part : parts)
                retVal.add(part);
        }
        return retVal;
    }

    /**
     * @return the insertion set for this sample
     */
    public Set<String> getInserts() {
        String inserts = this.fragments[INSERT_COL];
        Set<String> retVal = parseInserts(inserts);
        return retVal;
    }

    /**
     * @return a new sample ID with the additional protein inserted
     *
     * @param newProtein		new protein to insert
     */
    public SampleId addInsert(String newProtein) {
        SampleId retVal = new SampleId(this);
        Set<String> inserts = retVal.getInserts();
        inserts.add(newProtein);
        retVal.fragments[INSERT_COL] = StringUtils.join(inserts, '-');
        return retVal;
    }

    /**
     * @return a new sample ID with the additional protein deleted
     *
     * @param newProtein		new protein to delete
     */
    public SampleId addDelete(String newProtein) {
        SampleId retVal = new SampleId(this);
        Set<String> deletes = retVal.getDeletes();
        deletes.add(newProtein);
        retVal.fragments[DELETE_COL] = "D" + StringUtils.join(deletes, 'D');
        return retVal;
    }
    /**
     * @return the set of inserted proteins in an insert string
     *
     * @param inserts	insert string to parse
     */
    public static Set<String> parseInserts(String inserts) {
        Set<String> retVal = new TreeSet<String>();
        if (! inserts.contentEquals("000")) {
            String[] parts = StringUtils.split(inserts, '-');
            for (String part : parts)
                retVal.add(part);
        }
        return retVal;
    }

    /**
     * @return the name of this sample with a deletion removed
     *
     * @param protein	name of the protein being undeleted
     */
    public String unDelete(String protein) {
        Set<String> deletes = this.getDeletes();
        String retVal;
        if (! deletes.remove(protein)) {
            // The protein is not being deleted, so return the sample ID unchanged.
            retVal = this.toString();
        } else {
            // Return D000 if we are empty, else join up the new delete set.
            String newDeletes = "D000";
            if (! deletes.isEmpty())
                newDeletes = "D" + StringUtils.join(deletes, "D");
            retVal = this.replaceFragment(DELETE_COL, newDeletes);
        }
        return retVal;
    }

    /**
     * @return the name of this sample with an insertion removed
     *
     * @param protein	name of the protein being uninserted
     */
    public String unInsert(String protein) {
        Set<String> inserts = this.getInserts();
        String retVal;
        if (! inserts.remove(protein)) {
            // Removing the insert changed nothing, so return the sample ID unchanged.
            retVal = this.toString();
        } else {
            // Return "000" if we are empty, otherwise join the inserts.
            String newInserts = "000";
            if (! inserts.isEmpty())
                newInserts = StringUtils.join(inserts, "-");
            retVal = this.replaceFragment(INSERT_COL, newInserts);
        }
        return retVal;
    }

    /**
     * @return a string formed by replacing the fragment in the specified column with the specified value.
     *
     * @param col		column to replace
     * @param newVal	new value
     */
    public String replaceFragment(int col, String newVal) {
        String retVal = IntStream.range(0, NORMAL_SIZE).mapToObj(i -> (i == col ? newVal : this.fragments[i])).collect(Collectors.joining("_"));
        return retVal;
    }

    /**
     * @return an operon-generic sample ID string for this sample
     */
    public String genericOperon() {
        StringBuilder retVal = new StringBuilder(40);
        retVal.append(this.fragments[0]);
        retVal.append("_X_X_X");
        for (int i = 4; i < NORMAL_SIZE; i++)
            retVal.append("_").append(this.fragments[i]);
        return retVal.toString();
    }

    /**
     * @return an array of the basic fragments in the strain name (everything but the inserts and deletes)
     */
    public String[] getBaseFragments() {
        String[] retVal = Arrays.copyOfRange(this.fragments, 0, INSERT_COL);
        return retVal;
    }

    /**
     * @return an array of all the fragments in the strain name (everything but the time, media, and IPTG indicator)
     */
    public String[] getStrainFragments() {
        String[] retVal = Arrays.copyOfRange(this.fragments, 0, STRAIN_SIZE);
        return retVal;
    }

    /**
     * @return the fragment at the specified index
     *
     * @param idx	index of desired fragment
     */
    public String getFragment(int idx) {
        return this.fragments[idx];
    }

    /**
     * @return the time point for this sample
     */
    public double getTimePoint() {
        return this.timePoint;
    }

    /**
     * @return TRUE if this sample is induced, else FALSE
     */
    public boolean isIPTG() {
        return this.fragments[INDUCE_COL].contentEquals("I");
    }

    /**
     * @return the number of base fragments
     */
    public static int numBaseFragments() {
        return INSERT_COL;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (int i = 0; i < this.fragments.length; i++) {
            switch (i) {
            case INSERT_COL :
                result = 31 * result + this.getInserts().hashCode();
                break;
            case DELETE_COL :
                result = 31 * result + this.getDeletes().hashCode();
                break;
            default :
                result = 31 * result + this.getFragment(i).hashCode();
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SampleId)) {
            return false;
        }
        SampleId other = (SampleId) obj;
        boolean retVal = true;
        for (int i = 0; i < this.fragments.length && retVal; i++) {
            switch (i) {
            case DELETE_COL :
                // For deletes, the deletion order does not matter, so we have a special compare.
                Set<String> thisDels = this.getDeletes();
                Set<String> otherDels = other.getDeletes();
                retVal = (thisDels.size() == otherDels.size() && thisDels.containsAll(otherDels));
                break;
            case INSERT_COL :
                // Inserts work like deletes.
                Set<String> thisInserts = this.getInserts();
                Set<String> otherInserts = other.getInserts();
                retVal = (thisInserts.size() == otherInserts.size() && thisInserts.containsAll(otherInserts));
                break;
            default :
                // For normal fragments it's just a string compare.
                retVal = this.fragments[i].contentEquals(other.fragments[i]);
            }
        }
        return retVal;
    }

    /**
     * Match two sample IDs.  Either one may contain "X" for wildcard in any position.
     *
     * @param inSample	sample to match to this one
     *
     * @return TRUE for a match, else FALSE
     */
    public boolean matches(SampleId inSample) {
        boolean retVal = true;
        for (int i = 0; i < NORMAL_SIZE && retVal; i++) {
            if (! this.fragments[i].contentEquals("X") && ! inSample.fragments[i].contentEquals("X") &&
                    ! this.fragments[i].contentEquals(inSample.fragments[i])) {
                // Here we have two constants that don't match.  If we are an insert or delete, we do a set-based check.
                switch (i) {
                case DELETE_COL :
                    // For deletes, the deletion order does not matter, so we have a special compare.
                    Set<String> thisDels = this.getDeletes();
                    Set<String> otherDels = inSample.getDeletes();
                    retVal = (thisDels.size() == otherDels.size() && thisDels.containsAll(otherDels));
                    break;
                case INSERT_COL :
                    // Inserts work like deletes.
                    Set<String> thisInserts = this.getInserts();
                    Set<String> otherInserts = inSample.getInserts();
                    retVal = (thisInserts.size() == otherInserts.size() && thisInserts.containsAll(otherInserts));
                    break;
                default:
                    retVal = false;
                }
            }
        }
        return retVal;
    }

    /**
     * Convert this sample ID to a strain ID and return it.
     */
    public SampleId asStrain() {
        for (int i = STRAIN_SIZE; i < this.fragments.length; i++) {
            this.fragments[i] = "0";
        }
        return this;
    }

    /**
     * @return TRUE if this is a constructed strain
     */
    public boolean isConstructed() {
        return (! this.fragments[OPERON_COL].contentEquals("0") ||
                ! this.fragments[INSERT_COL].contentEquals("000") ||
                ! this.fragments[DELETE_COL].contentEquals("D000"));
    }

}
