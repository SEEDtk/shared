/**
 *
 */
package org.theseed.rna;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.theseed.io.TabbedLineReader;
import org.theseed.rna.RnaData.Row;

/**
 * This baseline computer uses a file to read in the baseline values.  The file should be tab-delimited with
 * headers, and contain the feature IDs in the column "fid" and the baseline values in the column "baseline".
 *
 * @author Bruce Parrello
 *
 */
public class FileBaselineComputer extends BaselineComputer {

    // FIELDS
    /** map of feature IDs to baselines */
    private Map<String, Double> baselineMap;

    /**
     * Construct a baseline value map from a file.
     *
     * @param baselineFile	file containing the baseline for each feature
     */
    public FileBaselineComputer(File baselineFile) {
        // Read the baseline map from the file.
        this.baselineMap = new HashMap<String, Double>(4000);
        try (TabbedLineReader reader = new TabbedLineReader(baselineFile)) {
            int fidIdx = reader.findField("fid");
            int valIdx = reader.findField("baseline");
            for (TabbedLineReader.Line line : reader)
                this.baselineMap.put(line.get(fidIdx), line.getDouble(valIdx));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public double getBaseline(Row row) {
        String fid = row.getFeat().getId();
        return this.baselineMap.getOrDefault(fid, 0.0);
    }

}
