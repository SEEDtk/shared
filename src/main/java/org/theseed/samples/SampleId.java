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
    /** number of fragments in the strain ID */
    public static final int STRAIN_SIZE = 7;
    /** array index of pseudo-numeric component */
    public static final int TIME_COL = 8;
    /** array index of the medium ID */
    public static final int MEDIA_COL = 9;
    /** number of guaranteed fields */
    public static final int NORMAL_SIZE = 10;
    /** index of the insertion column */
    public static final int INSERT_COL = 5;
    /** index of the deletion column */
    public static final int DELETE_COL = 6;
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
            new AbstractMap.SimpleEntry<>("pwt2-1-1", StringUtils.split("D_Tasd_P_asdD", '_')),
            new AbstractMap.SimpleEntry<>("pfb6-4-3", StringUtils.split("D_TasdA_P_asdD", '_'))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    /** map of mis-spelled deletion protein names */
    private static final Map<String, String> PROTEIN_ERRORS = Stream.of(
            new AbstractMap.SimpleEntry<>("rthA", "rhtA"),
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
            new AbstractMap.SimpleEntry<>("318", "30318")
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    /** map of strain IDs for non-plasmid strains */
    private static final Map<String, String[]> CHROMO_MAP = Stream.of(
            new AbstractMap.SimpleEntry<>("277-14", StringUtils.split("7_0_TA1_C_asdO", '_')),
            new AbstractMap.SimpleEntry<>("277wt1", StringUtils.split("7_0_T_C_asdO", '_')),
            new AbstractMap.SimpleEntry<>("926-44", StringUtils.split("M_0_T_C_asdO", '_')),
            new AbstractMap.SimpleEntry<>("926fb1", StringUtils.split("M_0_TA1_C_asdO", '_'))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    /** pattern for parsing an old strain name */
    protected static final Pattern OLD_STRAIN_NAME = Pattern.compile("(\\d+)([Dd]\\S+)?((?:\\s+\\S+)+)?");
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
    private static final Pattern PLASMID_PATTERN = Pattern.compile("_(?:pfb)?\\d[_.]\\d[_.]\\d_");
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
        Matcher m = OLD_STRAIN_NAME.matcher(strain);
        if (! m.matches())
            retVal = null;
        else {
            // We have a valid strain.  The first group is the host strain.
            String host = m.group(1);
            retVal.fragments[0] = HOST_MAP.getOrDefault(host, host);
            // Group 3 is a bunch of inserts, space-delimited.  A "+" is removed.
            // There can be up to two inserts-- a plasmid spec and a protein.  Note that the
            // term "plasmid" is obsolete, as sometimes we put these things directly on the
            // chromosome.
            boolean plasmidFound = false;
            String[] plasmidSpecs = PLASMID_DEFAULT;
            String insert = "000";
            if (m.group(3) != null) {
                String[] insertParts = StringUtils.split(m.group(3));
                for (String insertPart : insertParts) {
                    if (insertPart.startsWith("+"))
                        insertPart = insertPart.substring(1);
                    // Convert to lower case and fix periods.  A plasmid can be specified with
                    // either periods or hyphens and we prefer hyphens.
                    insertPart = StringUtils.replaceChars(insertPart.toLowerCase(), '.', '-');
                    // Check for a plasmid.
                    if (PLASMID_MAP.containsKey(insertPart)) {
                        plasmidSpecs = PLASMID_MAP.get(insertPart);
                        plasmidFound = true;
                    } else
                        insert = insertPart;
                }
            }
            // Copy the plasmid specs to the output and make the 277 adjustment.
            System.arraycopy(plasmidSpecs, 0, retVal.fragments, 1, 4);
            if (host.contentEquals("277") && ! plasmidFound)
                retVal.fragments[3] = "A";
            // The second group is deletions.
            String deletions = m.group(2);
            if (deletions == null)
                retVal.fragments[DELETE_COL] = "D000";
            else {
                // Here we have real deletes to process.
                String fixedDeleteSpec = fixDeletes(deletions);
                retVal.fragments[DELETE_COL] = fixedDeleteSpec;
            }
            // Next we process the protein insert.  Here, we also need to repair the lower-casing.
            retVal.fragments[INSERT_COL] = StringUtils.substring(insert, 0, 3) +
                    StringUtils.substring(insert, 3).toUpperCase();
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
        }
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
            String[] parts = rnaMatch(rnaFile.getName());
            if (parts != null) {
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
                String host = HOST_MAP.get(strainId);
                if (host == null) {
                    // If the host is not found, check for an artificial host, which contains operon adjustments built in.
                    String[] plasmidInfo = CHROMO_MAP.get(strainId);
                    if (plasmidInfo == null)
                        throw new IllegalArgumentException("Invalid host name \"" + strainId + "\".");
                    else {
                        // Copy the full host/operon info to the sample ID.
                        System.arraycopy(plasmidInfo, 0, retVal.fragments, 0, plasmidInfo.length);
                    }
                } else {
                    // Here we have a normal host.
                    retVal.fragments[0] = host;
                    String[] plasmidInfo = PLASMID_DEFAULT;
                    System.arraycopy(plasmidInfo, 0, retVal.fragments, 1, plasmidInfo.length);
                }
                // Denote that so far there is no insert.
                String insert = "000";
                // Run through the modifiers.
                for (int i = 4; i < parts.length; i++) {
                    String modifier = parts[i];
                    String[] plasmidInfo = PLASMID_MAP.get(modifier);
                    if (plasmidInfo != null)
                        System.arraycopy(plasmidInfo, 0, retVal.fragments, 1, plasmidInfo.length);
                    else switch (modifier) {
                    case "ptac-thrABC" :
                        retVal.fragments[2] = "TA1";
                        retVal.fragments[3] = "C";
                        break;
                    case "ptac-asd" :
                        retVal.fragments[4] = "asdT";
                        break;
                    default :
                        // Here we have an insert.
                        insert = modifier;
                    }
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
            }
        }
        return retVal;
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
        String reducedName = StringUtils.replaceOnce(fileName, "pta-", "ptac-");
        reducedName = StringUtils.replaceOnce(reducedName, "D_lysC", "DlysC");
        reducedName = StringUtils.replaceOnce(reducedName, "926_lysC", "926DlysC");
        reducedName = StringUtils.replace(reducedName, "Dtd_h", "Dtdh");
        // Fix the delete glitch.  This is caused by the weird untranslatable unicode character.
        Matcher m = DELETE_GLITCH.matcher(reducedName);
        if (m.matches())
            reducedName = m.group(1) + m.group(2);
        // Fix the PTAC glitch.  Sometimes the leading space is missing.
        m = PTAC_GLITCH.matcher(reducedName);
        if (m.matches())
            reducedName = m.group(1) + "_" + m.group(2);
        // Fix the numbered plasmids.  We need to replace the underscores/periods with hyphens.
        m = PLASMID_PATTERN.matcher(reducedName);
        if (m.find()) {
            String plasmid = StringUtils.substring(reducedName, m.start() + 1, m.end() - 1);
            reducedName = reducedName.substring(0, m.start() + 1) +
                    StringUtils.replaceChars(plasmid, "._", "--") +
                    reducedName.substring(m.end() - 1);
        }
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

    @Override
    public int compareTo(SampleId o) {
        int retVal = 0;
        for (int i = 0; retVal == 0 && i < NORMAL_SIZE; i++) {
            if (i == TIME_COL)
                retVal = Double.compare(this.timePoint, o.timePoint);
            else
                retVal = this.fragments[i].compareTo(o.fragments[i]);
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
        Set<String> retVal = new TreeSet<String>();
        String deletes = this.fragments[DELETE_COL];
        if (! deletes.contentEquals("D000")) {
            String[] parts = StringUtils.split(deletes, 'D');
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
        String deletes = this.fragments[DELETE_COL];
        deletes = StringUtils.remove(deletes, "D" + protein);
        if (deletes.isEmpty())
            deletes = "D000";
        String retVal = this.replaceFragment(DELETE_COL, deletes);
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
     * @return an array of the basic fragments in the strain name (everything but the deletes)
     */
    public String[] getBaseFragments() {
        String[] retVal = Arrays.copyOfRange(this.fragments, 0, DELETE_COL);
        return retVal;
    }

    /**
     * @return an array of all the fragments in the strain name (everything but the deletes)
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
        return DELETE_COL;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(this.fragments);
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
        if (!Arrays.equals(this.fragments, other.fragments)) {
            return false;
        }
        return true;
    }

}
