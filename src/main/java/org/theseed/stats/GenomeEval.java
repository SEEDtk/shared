/**
 *
 */
package org.theseed.stats;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.theseed.counters.CountMap;
import org.theseed.io.TabbedLineReader;

/**
 * This class that contains the methods that determine the various quality cutoffs for evaluation,
 * as well as structural information about evaluation summary files.  It is used to analyze the
 * contents of such files for statistical purposes.
 *
 * @author Bruce Parrello
 *
 */
public class GenomeEval {

    // FIELDS
    /** genome counts */
    private final CountMap<String> statMap;
    /** list of stat types */
    public static final Set<String> STAT_TYPES = Arrays.asList("HasRNA", "HasPheS", "Clean", "Complete",
            "Consistent", "Understood").stream().collect(Collectors.toSet());
    /** list of column headers (NOTE that the columns relating to completeness are at the end so they can be easily removed.) */
    public static final String[] DEFAULT_HEADERS = new String[] { "Genome", "Name", "Score", "Good", "Taxonomy", "Good Seed",
            "Ssu_rRNA", "Contigs", "Hypothetical", "Coarse", "Fine", "Completeness", "Contamination", "Group" };
    /** index of the good-seed column */
    public static final int SEED_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Good Seed");
    /** index of the contamination-percent column */
    public static final int CONTAM_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Contamination");
    /** index of the completeness-percent column */
    public static final int COMPLETE_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Completeness");
    /** index of the hypothetical-percent column */
    public static final int HYPO_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Hypothetical");
    /** index of the fine-consistency column */
    public static final int FINE_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Fine");
    /** index of the SSU-present column */
    public static final int SSU_RNA_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Ssu_rRNA");
    /** index of the good/bad column */
    public static final int GOOD_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Good");
    /** index of the score column */
    public static final int SCORE_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Score");
    /** index of the genome ID column */
    public static final int GENOME_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Genome");
    /** index of the genome name column */
    public static final int NAME_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Name");
    /** index of the lineage column */
    public static final int LINEAGE_COL = ArrayUtils.indexOf(DEFAULT_HEADERS, "Taxonomy");
    /** minimum fine consistency */
    public static final double MIN_CONSISTENCY = 85.0;
    /** maximumum hypothetical percent  */
    public static final double MAX_HYPOTHETICAL = 70.0;
    /** minimum completeness */
    public static final double MIN_COMPLETENESS = 80.0;
    /** maximum contamination */
    public static final double MAX_CONTAMINATION = 10.0;

    /**
     * @return TRUE if the specified contamination percent indicates a clean genome
     *
     * @param contam	contamination percent
     */
    public static boolean indicatesClean(double contam) {
        return (contam < MAX_CONTAMINATION);
    }

    /**
     * @return TRUE if this fine consistency indicates a genome is consistently annotated, else FALSE
     *
     * @param fine	fine consistency percent
     */
    public static boolean indicatesConsistent(double fine) {
        return (fine >= MIN_CONSISTENCY);
    }

    /**
     * @return TRUE if the completeness percent indicates a complete genome
     *
     * @param comp	completeness percent
     */
    public static boolean indicatesComplete(double comp) {
        return (comp >= MIN_COMPLETENESS);
    }

    /**
     * @return TRUE if the percent hypothetical protein percent indicates the genome is understood
     *
     * @param hypo	percent of proteins that are hypothetical
     */
    public static boolean indicatesUnderstood(double hypo) {
        return (hypo <= MAX_HYPOTHETICAL);
    }

    public static String getHeader(boolean haveCompleteness) {
        String retVal;
        if (haveCompleteness)
            retVal = StringUtils.join(DEFAULT_HEADERS, '\t');
        else
            retVal = StringUtils.join(DEFAULT_HEADERS, '\t', 0, COMPLETE_COL);
        return retVal;
    }

    /**
     * Create a genome evaluation object for analyzing the content of an evaluation summary
     * file.
     */
    public GenomeEval() {
        // We will count the various genome attributes in here.
        this.statMap = new CountMap<>();
    }

    /**
     * Analyze a data line from a summary file.
     *
     * @param line	data line to analyze
     */
    public void analyze(TabbedLineReader.Line line) {
        Set<String> stats = new TreeSet<>();
        if (line.getFancyFlag(GenomeEval.GOOD_COL))
            this.statMap.count("Good");
        else
            this.statMap.count("Bad");
        if (line.getFancyFlag(GenomeEval.SSU_RNA_COL))
            stats.add("HasRNA");
        else
            this.statMap.count("MissingRNA");
        if (line.getFancyFlag(GenomeEval.SEED_COL))
            stats.add("HasPheS");
        else
            this.statMap.count("MissingPheS");
        if (indicatesClean(line.getDouble(GenomeEval.CONTAM_COL)))
            stats.add("Clean");
        else
            this.statMap.count("Contaminated");
        if (indicatesComplete(line.getDouble(GenomeEval.COMPLETE_COL)))
            stats.add("Complete");
        else
            this.statMap.count("Incomplete");
        if (indicatesConsistent(line.getDouble(GenomeEval.FINE_COL)))
            stats.add("Consistent");
        else
            this.statMap.count("Inconsistent");
        if (indicatesUnderstood(line.getDouble(GenomeEval.HYPO_COL)))
            stats.add("Understood");
        else
            this.statMap.count("Misunderstood");
        // If there is a single failure point, note that.
        if (GenomeEval.STAT_TYPES.size() - stats.size() == 1) {
            for (String reason : GenomeEval.STAT_TYPES) {
                if (! stats.contains(reason))
                    this.statMap.count("Not " + reason);
            }
        }
    }

    /**
     * Add a count for a new statistic.
     *
     * @param name		name of the statistic
     * @param count		value to add
     */
    public void count(String name, int count) {
        this.statMap.count(name, count);
    }

    /**
     * Write the statistics to the specified file.
     *
     * @param statFile	output file for statistics
     *
     * @throws IOException
     */
    public void write(File statFile) throws IOException {
        try (PrintWriter statWriter = new PrintWriter(statFile)) {
            statWriter.println("status\tcount");
            for (CountMap<String>.Count counter : this.statMap.sortedCounts())
                statWriter.format("%s\t%d%n", counter.getKey(), counter.getCount());
        }
    }

}
