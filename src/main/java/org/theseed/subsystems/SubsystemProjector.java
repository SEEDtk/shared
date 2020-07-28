/**
 *
 */
package org.theseed.subsystems;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.theseed.io.LineReader;

/**
 * This object contains the information necessary to project subsystems onto a genome.  In particular, it contains
 * all of the subsystem specifications and a map of protein family IDs to variant specifications.
 *
 * The variant specifications are stored in a single list using the natural ordering of variant specifications.
 * This ordering insures that the first variant found for a particular subsystem is the one that should be
 * kept.
 *
 * The subsystems are mapped by name, for easy access.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemProjector {

    // FIELDS
    /** marker line for end of a group */
    private static final String END_MARKER = "//";
    /** marker line for start of variant section */
    private static final String VARIANT_SECTION_MARKER = "**";
    /** subsystem specifications */
    private Map<String, SubsystemSpec> subsystems;
    /** variant specifications */
    private SortedSet<VariantSpec> variants;

    /**
     * Create a new, blank subsystem projector.
     */
    public SubsystemProjector() {
        this.subsystems = new HashMap<String, SubsystemSpec>(1000);
        this.variants = new TreeSet<VariantSpec>();
    }

    /**
     * Create a subsystem projector from a saved file.
     *
     * @param inFile	file containing the projector
     *
     * @return the subsystem projector created
     *
     * @throws IOException
     */
    public static SubsystemProjector Load(File inFile) throws IOException {
        SubsystemProjector retVal = new SubsystemProjector();
        try (LineReader inStream = new LineReader(inFile)) {
            // Loop through the subsystems.
            for (String line = inStream.next(); ! line.contentEquals(VARIANT_SECTION_MARKER); line = inStream.next()) {
                // Read a single subsystem here.  The current line is the name.
                SubsystemSpec subsystem = new SubsystemSpec(line);
                // Next we read the classifications. Again, we use token-preserving because of possible blank slots.
                line = inStream.next();
                String[] classes = StringUtils.splitPreserveAllTokens(line, '\t');
                subsystem.setClassifications(classes);
                // Now the list of roles.
                line = inStream.next();
                while (! line.contentEquals(END_MARKER)) {
                    subsystem.addRole(line);
                    line = inStream.next();
                }
                // Add the subsystem to the projector.
                retVal.addSubsystem(subsystem);
            }
            // Loop through the variants.  Each variant is three lines of text.
            while (inStream.hasNext()) {
                // Read the subsystem name.
                String name = inStream.next();
                // Read the variant code.
                String code = inStream.next();
                // Find the subsystem and create the variant spec.
                SubsystemSpec subsystem = retVal.subsystems.get(name);
                if (subsystem == null)
                    throw new IOException("Variant in " + inFile + " specifies undefined subsystem " + name + ".");
                VariantSpec variant = new VariantSpec(subsystem, code);
                // Read the protein family cells.  Note that empty cells are normal, so we preserve all tokens.
                String[] cells = StringUtils.splitPreserveAllTokens(inStream.next(), '\t');
                for (int i = 0; i < cells.length; i++) {
                    if (! cells[i].isEmpty())
                        variant.setCell(i, cells[i]);
                }
                // Add the variant to the projector.
                retVal.addVariant(variant);
            }
        }
        return retVal;
    }

    /**
     * Add a new subsystem.
     *
     * @param subsystem		new subsystem specification
     */
    public void addSubsystem(SubsystemSpec subsystem) {
        this.subsystems.put(subsystem.getName(), subsystem);
    }

    /**
     * Add a new variant specification.
     *
     * @param variant		new variant specification
     */
    public void addVariant(VariantSpec variant) {
        this.variants.add(variant);
    }

    /**
     * Save this projector to a file.
     *
     * @throws IOException
     */
    public void save(File outFile) throws IOException {
        try (PrintWriter outStream = new PrintWriter(outFile)) {
            for (SubsystemSpec subsystem : this.subsystems.values()) {
                // Write the name.
                outStream.println(subsystem.getName());
                // Write the classifications.
                outStream.println(StringUtils.join(subsystem.getClassifications(), '\t'));
                // Write the roles.
                for (String role : subsystem.getRoles()) {
                    outStream.println(role);
                }
                // Terminate the subsystem specification.
                outStream.println(END_MARKER);
            }
            // Denote we are starting the variants.
            outStream.println(VARIANT_SECTION_MARKER);
            for (VariantSpec variant : this.variants) {
                // Write the subsystem name.
                outStream.println(variant.getName());
                // Write the variant code.
                outStream.println(variant.getCode());
                // Write the family list.
                outStream.println(StringUtils.join(variant.getCells(), '\t'));
            }
            // Insure the whole file is written.
            outStream.flush();
        }
    }

    /**
     * @return the specification for the subsystem with the given name, or NULL if the name is not found
     *
     * @param subsystemName		name of desired subsystem
     */
    public SubsystemSpec getSubsystem(String subsystemName) {
        return this.subsystems.get(subsystemName);
    }

    /**
     * @return the set of variants in this projector
     */
    public SortedSet<VariantSpec> getVariants() {
        return this.variants;
    }

}
