/**
 *
 */
package org.theseed.subsystems;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.io.LineReader;
import org.theseed.magic.MagicMap;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.proteins.RoleSet;

/**
 * This object contains the information necessary to project subsystems onto a genome.  In particular, it contains
 * all of the subsystem specifications and a map of role IDs to variant specifications.
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
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SubsystemProjector.class);
    /** subsystem specifications */
    private Map<String, SubsystemSpec> subsystems;
    /** variant specifications */
    private SortedSet<VariantSpec> variants;
    /** useful role map */
    private RoleMap roleMap;
    /** marker line for end of a group */
    public static final String END_MARKER = "//";
    /** marker line for start of variant section */
    private static final String VARIANT_SECTION_MARKER = "**";
    /** spacer for rule report indenting */
    private static final String SPACER = "    ";
    /** double spacer for rule report indenting */
    private static final String SPACER2 = SPACER + SPACER;

    /**
     * Create a new, blank subsystem projector.
     */
    public SubsystemProjector() {
        this.setup();
        this.roleMap = new RoleMap();
    }

    /**
     * Create a new, blank subsystem projector with preloaded role definitions.
     *
     * @param roleFile	role file to use (or NULL if none)
     */
    public SubsystemProjector(File roleFile) {
        this.setup();
        if (roleFile == null)
            this.roleMap = new RoleMap();
        else {
            this.roleMap = RoleMap.load(roleFile);
            log.info("{} role definitions pre-loaded from {}.", this.roleMap.size(), roleFile);
        }
    }

    /**
     * Initialize this subsystem projector.
     */
    private void setup() {
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
    public static SubsystemProjector load(File inFile) throws IOException {
        SubsystemProjector retVal = new SubsystemProjector();
        try (LineReader inStream = new LineReader(inFile)) {
            // Loop through the roles.
            for (String line = inStream.next(); ! line.contentEquals(VARIANT_SECTION_MARKER); line = inStream.next()) {
                String[] roleDefinition = StringUtils.split(line, "\t", 2);
                Role newRole = new Role(roleDefinition[0], roleDefinition[1]);
                retVal.roleMap.put(newRole);
            }
            // Loop through the subsystems.
            for (String line = inStream.next(); ! line.contentEquals(VARIANT_SECTION_MARKER); line = inStream.next()) {
                // Read a single subsystem here.  The current line is the name.
                SubsystemSpec subsystem = new SubsystemSpec(line);
                // Next we read the classifications. Again, we use token-preserving because of possible blank slots.
                line = inStream.next();
                String[] classes = StringUtils.splitPreserveAllTokens(line, '\t');
                subsystem.setClassifications(classes);
                // Now the list of roles.  We add each role to the role map so we can generate an ID for it.
                line = inStream.next();
                while (! line.contentEquals(END_MARKER)) {
                    subsystem.addRole(line);
                    retVal.roleMap.findOrInsert(line);
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
                // Read the protein role cells.  Note that empty cells are normal, so we preserve all tokens.
                String[] cells = StringUtils.splitPreserveAllTokens(inStream.next(), '\t');
                for (int i = 0; i < cells.length; i++) {
                    if (! cells[i].isEmpty())
                        variant.setCell(i, retVal);
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
        // We need to put all the roles in the role map.  This is slightly complicated
        // by the fact that the subsystem role may be a multi-role function.
        for (String function : subsystem.getRoles()) {
            String[] roles = Feature.rolesOfFunction(function);
            for (String role : roles)
                this.roleMap.findOrInsert(role);
        }
    }

    /**
     * Add a new variant specification.
     *
     * @param variant		new variant specification
     *
     * @return TRUE if we added the variant, FALSE if it is a duplicate
     */
    public boolean addVariant(VariantSpec variant) {
        return this.variants.add(variant);
    }

    /**
     * Save this projector to a file.
     *
     * @throws IOException
     */
    public void save(File outFile) throws IOException {
        try (PrintWriter outStream = new PrintWriter(outFile)) {
            // Write the roles.  These must come first, since otherwise the order of the subsystems
            // matters.
            for (MagicMap.Entry<String,String> entry : this.roleMap.entrySet())
                outStream.println(entry.getKey() + "\t" + entry.getValue());
            outStream.println(VARIANT_SECTION_MARKER);
            // Now we write the subsystems.
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
                // Write the role list.
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

    /**
     * @return a role map for the specified genome
     *
     * @param genome	genome whose role map is needed
     */
    public Map<String, Set<String>> computeRoleMap(Genome genome) {
        Map<String, Set<String>> retVal = new HashMap<String, Set<String>>(genome.getFeatureCount());
        for (Feature feat : genome.getFeatures()) {
            for (Role role : feat.getUsefulRoles(roleMap))
                storeFeature(retVal, feat, role.getId());
        }
        return retVal;
    }

    /**
     * Store a feature in a role map using the specified identifier.
     *
     * @param roleMap		target role map
     * @param feat			feature to store
     * @param identifier	identifier with which to classify it (role ID)
     */
    protected void storeFeature(Map<String, Set<String>> roleMap, Feature feat, String identifier) {
        Set<String> fids = roleMap.computeIfAbsent(identifier, k -> new HashSet<String>(5));
        fids.add(feat.getId());
    }

    /**
     * @return the IDs for the roles in the specified feature
     *
     * @param feat	feature to process
     */
    public RoleSet getRoleIds(Feature feat) {
        RoleSet retVal;
        if (feat == null)
            retVal = RoleSet.NO_ROLES;
        else
            retVal = this.getRoleIds(feat.getFunction());
        return retVal;
    }

    /**
     * @return the IDs for the roles represented in the specified function
     *
     * @param function	function to process
     */
    public RoleSet getRoleIds(String function) {
        return RoleSet.create(function, this.roleMap);
    }

    /**
     * @return the ID of the role with a given description, or NULL if the
     * 		   description does not match a useful role
     *
     * @param roleDesc	description string of the role of interest
     */
    public String getRoleId(String roleDesc) {
        Role buffer = this.roleMap.getByName(roleDesc);
        String retVal = (buffer == null ? null : buffer.getId());
        return retVal;

    }

    /**
     * Project subsystems into a genome.  Existing subsystems are all cleared before the genome is updated.
     *
     * @param genome	genome into which subsystems should be projected
     */
    public void project(Genome genome) {
        // Create a role map for this genome.
        log.info("Scanning features in {} for subsystem projection.", genome);
        Map<String, Set<String>> roleMap = this.computeRoleMap(genome);
        // This map will track the variants found.  It is keyed on subsystem name.  Only the
        // first variant found for a subsystem is kept, since the variant specs are ordered
        // from most to least preferable.
        Map<String, VariantSpec> variantMap = new HashMap<String, VariantSpec>();
        log.info("Scanning variants for subsystem matches.");
        for (VariantSpec variant : this.variants) {
            String name = variant.getName();
            // Only proceed if we don't already have a match for this subsystem.
            if (! variantMap.containsKey(name)) {
                if (variant.matches(roleMap)) {
                    // Here we have the subsystem.
                    variantMap.put(name, variant);
                }
            }
        }
        log.info("{} subsystems found.", variantMap.size());
        // Instantiate the subsystems.
        genome.clearSubsystems();
        for (VariantSpec variant : variantMap.values()) {
            variant.instantiate(genome, roleMap);
            log.debug("{} instantiated in {}.", variant.getName(), genome);
        }
    }

    /**
     * @return the subsystem role map
     */
    public RoleMap usefulRoles() {
        return this.roleMap;
    }

    /**
     * This method writes a summary of the variant rules for each subsystem.  For each subsystem, there is
     * a header line with the subsystem name, then for each variant within the subsystem there is a header
     * line for the variant followed by detail lines listing role requirements.  The variant is present if
     * at least one of the role sets is satisfied.
     *
     * @param ruleFile	output file for the report
     *
     * @throws IOException
     */
    public void ruleReport(File ruleFile) throws IOException {
        Map<String, List<String>> ruleMap = new TreeMap<String, List<String>>();
        try (PrintWriter writer = new PrintWriter(ruleFile)) {
            for (SubsystemSpec subsystem : this.subsystems.values()) {
                final String subName = subsystem.getName();
                writer.println(subName);
                // Loop through the variants, extracting the ones for this subsystem into
                // the rule map.
                ruleMap.clear();
                for (VariantSpec variant : this.variants) {
                    if (variant.getName().contentEquals(subName)) {
                        List<String> rules = ruleMap.computeIfAbsent(variant.getCode(), x -> new ArrayList<String>());
                        rules.add(variant.getRuleString());
                    }
                }
                // Now we print the rule map.
                int vCount = 0;
                int rCount = 0;
                for (var variantEntry : ruleMap.entrySet()) {
                    String variantCode = variantEntry.getKey();
                    vCount++;
                    List<String> rules = variantEntry.getValue();
                    writer.println(SPACER + "Variant " + variantCode);
                    for (String rule : rules) {
                        writer.println(SPACER2 + rule);
                        rCount++;
                    }
                }
                log.info("{} variants and {} rules found for {}.", vCount, rCount, subName);
            }
        }

    }

}
