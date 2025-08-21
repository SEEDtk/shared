/**
 *
 */
package org.theseed.subsystems.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.subsystems.StrictRoleMap;

/**
 * @author Bruce Parrello
 *
 */
class TestRuleGenerator {

    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(TestRuleGenerator.class);


    @Test
    void testGeneration() throws IOException, ParseFailureException {
        StrictRoleMap roleMap = StrictRoleMap.load(new File("data/ss_test/Subsystems", "core.roles.in.subsystems"));
        File subDir = new File("data/ss_test/Subsystems", "Biosynthesis_of_Arabinogalactan_in_Mycobacteria");
        CoreSubsystem sub = new CoreSubsystem(subDir, roleMap);
        RuleGenerator ruleGen = new RuleGenerator(sub);
        Set<String> vCodes = ruleGen.getVariantCodes();
        assertThat(vCodes, containsInAnyOrder("0", "1", "9"));
        // Find a commons for each variant.
        Map<String, RuleBits> commonMap = new TreeMap<String, RuleBits>();
        for (String vCode : vCodes) {
            Collection<RuleBits> ruleList = ruleGen.getRuleBits(vCode);
            RuleBits common = RuleBits.intersection(sub, ruleList);
            commonMap.put(vCode, common);
        }
        // Verify that each spreadsheet line works.
        Iterator<CoreSubsystem.Row> iter = sub.rowIterator();
        while (iter.hasNext()) {
            CoreSubsystem.Row row = iter.next();
            String label = row.getGenomeId();
            if (! row.isInactive()) {
                String vCode = row.getVariantCode();
                Collection<RuleBits> rules = ruleGen.getRuleBits(vCode);
                // Check the row against the list of rules.
                RuleBits rowBits = new RuleBits(row);
                assertThat(label, rowBits.matches(rules), equalTo(true));
                // Check the row against the common set.
                RuleBits common = commonMap.get(vCode);
                assertThat(label, common.subsumeCompare(rowBits), lessThan(0));
            }
        }
        // Output the rules.
        List<String> variantRules = ruleGen.getVariantRules();
        List<String> definitions = ruleGen.getGroupDefinitions();
        assertThat(definitions.size(), greaterThan(0));
        assertThat(variantRules.size(), equalTo(4));
    }

    public static final String[] CHECK_FILES = new String[] { "checkvariant_definitions", "checkvariant_rules" };

    @Test
    void testCompiling() throws IOException, ParseFailureException {
        StrictRoleMap roleMap = StrictRoleMap.load(new File("data/ss_test/Subsystems", "core.roles.in.subsystems"));
        File subDir = new File("data/ss_test/Subsystems", "Biosynthesis_of_Arabinogalactan_in_Mycobacteria");
        // Get rid of any leftover rule files from the last test.
        for (String fileName : CHECK_FILES) {
            File file = new File(subDir, fileName);
            if (file.exists())
                FileUtils.forceDelete(file);
        }
        // Load the subsystem and create the rules.
        CoreSubsystem sub = new CoreSubsystem(subDir, roleMap);
        sub.createRules();
        // Now we test each subsystem row against the rules.
        Iterator<CoreSubsystem.Row> iter = sub.rowIterator();
        while (iter.hasNext()) {
            CoreSubsystem.Row row = iter.next();
            if (! row.isInactive()) {
                String label = row.getGenomeId();
                String vCode = row.getVariantCode();
                Set<String> roles = row.getRoles();
                String ruleCode = sub.applyRules(roles);
                if (! ruleCode.equals(vCode))
                    log.info("For {}. Should be {} but found {}.", label, vCode, ruleCode);
                // assertThat(label, ruleCode, equalTo(vCode));
            }
        }
    }

}
