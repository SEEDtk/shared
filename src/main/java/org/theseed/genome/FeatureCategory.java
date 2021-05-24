/**
 *
 */
package org.theseed.genome;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.theseed.utils.IDescribable;

/**
 * This enum describes several useful feature categories.  A feature can potentially be in more than
 * one category-- all that matters is the ability to determine whether the feature is in the category
 * or not.  The categories are generally related to matters of annotation rather than function.  So,
 * the feature type (functional) would be RNA, PEG, or CRISPR, but a category would be hypothetical,
 * know, in-subsystem, not-in-subsystem.
 *
 * @author Bruce Parrello
 *
 */
public enum FeatureCategory implements IDescribable {
    HYPOTHETICAL {
        @Override
        public String getDescription() {
            return "hypothetical";
        }

        @Override
        public boolean qualifies(Feature feat) {
            String function = StringUtils.substringBefore(feat.getPegFunction(), "#").trim().toLowerCase();
            return HYPO_PATTERN.matcher(function).find();
        }

    }, KNOWN {
        @Override
        public String getDescription() {
            return "non-hypothetical";
        }

        @Override
        public boolean qualifies(Feature feat) {
            return ! HYPOTHETICAL.qualifies(feat);
        }

    }, IN_SUBSYSTEM {
        @Override
        public String getDescription() {
            return "in a subsystem";
        }

        @Override
        public boolean qualifies(Feature feat) {
            return ! feat.getSubsystems().isEmpty();
        }
    }, OUT_SUBSYSTEM {
        @Override
        public String getDescription() {
            return "not in a subsystem";
        }

        @Override
        public boolean qualifies(Feature feat) {
            return ! IN_SUBSYSTEM.qualifies(feat);
        }
    };

    /**
     * @return TRUE if the specified feature is in this category, else FALSE
     *
     * @param feat		feature to test
     */
    public abstract boolean qualifies(Feature feat);

    /** pattern for hypothetical protein functions */
    public static Pattern HYPO_PATTERN = Pattern.compile("hypothetical|putative|uncharacterized", Pattern.CASE_INSENSITIVE);
}
