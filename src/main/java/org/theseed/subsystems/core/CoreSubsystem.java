/**
 *
 */
package org.theseed.subsystems.core;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.io.LineReader;
import org.theseed.io.MarkerFile;
import org.theseed.io.TabbedLineReader;
import org.theseed.subsystems.StrictRole;
import org.theseed.subsystems.StrictRoleMap;
import org.theseed.subsystems.VariantId;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * A core subsystem contains various bits of information about a subsystem found in the CoreSEED.
 * This includes the name, the three classifications, the roles, the role IDs, the abbreviations,
 * the auxiliary roles, the spreadsheet rows, and the variant rules.
 *
 * @author Bruce Parrello
 *
 */
public class CoreSubsystem {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(CoreSubsystem.class);
    /** subsystem name */
    private String name;
    /** rule namespace */
    private LinkedHashMap<String, SubsystemRule> ruleMap;
    /** map of variant rules, in priority order */
    private LinkedHashMap<String, SubsystemRule> variantRules;
    /** version number */
    private int version;
    /** TRUE if this is a good subsystem (exchangeable and non-experimental) */
    private boolean good;
    /** auxiliary role list */
    private Set<String> auxRoles;
    /** auxiliary role bit map */
    private BitSet auxMap;
    /** associated role ID computation map */
    private StrictRoleMap roleMap;
    /** list of roles, in order */
    private List<StrictRole> roles;
    /** role abbreviations, in order */
    private List<String> roleAbbrs;
    /** subsystem spreadsheet */
    private Map<String, Row> spreadsheet;
    /** classifications */
    private List<String> classes;
    /** set of invalid identifiers */
    private Set<String> badIds;
    /** number of invalid roles */
    private int badRoles;
    /** roles not found during current rule */
    private Set<String> notFound;
    /** roles found during current rule */
    private Set<String> found;
    /** description note */
    private String description;
    /** set of pubmed IDs from the notes */
    private Set<Integer> pubmed;
    /** basic note */
    private String note;
    /** variant description map */
    private Map<String, String> variantNotes;
    /** original directory */
    private File subDir;
    /** list of feature types used in subsystems */
    public static final String[] FID_TYPES = new String[] { "opr", "aSDomain", "pbs", "rna", "rsw", "sRNA", "peg" };
    /** common representation of an empty cell */
    private static final Set<String> EMPTY_CELL = Collections.emptySet();
    /** marker to separate file sections */
    public static final String SECTION_MARKER = "//";
    /** main rule parser, for separating the rule name and the text to compile */
    private static final Pattern RULE_PATTERN = Pattern.compile("\\s*(\\S+)\\s+(?:(?:means|if|is)\\s+)?(.+)");
    /** pattern for matching a marker line */
    private static final Pattern NOTES_MARKER = Pattern.compile("####+");
    /** pattern for finding pubmed IDs */
    private static final Pattern PUBMED_REFERENCE = Pattern.compile("PMID:\\s+(\\d+)");
    /** subsystem directory filter */
    private static final FileFilter DIR_SS_FILTER = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            // We accept the file if it is a directory and has a spreadsheet file in it.
            boolean retVal = pathname.isDirectory();
            if (retVal) {
                File ssFile = new File(pathname, "spreadsheet");
                retVal = ssFile.exists();
            }
            return retVal;
        }

    };

    /**
     * This class describes a spreadsheet row.  It contains the the variant code and the list of peg sets in column order.
     * An empty cell will point to an empty set of peg IDs.
     */
    public class Row {

        /** ID of the target genome */
        private String genomeId;
        /** variant code */
        private String variantCode;
        /** list of peg sets */
        private List<Set<String>> columns;

        /**
         * Construct a subsystem row from a spreadsheet line.
         *
         * @param cols		columns from the input spreadsheet line
         */
        protected Row(String[] cols) {
            // Save the genome ID and variant code.
            this.genomeId = cols[0];
            this.variantCode = cols[1];
            // Create a basic feature ID prefix.
            String prefix = "fig|" + this.genomeId + ".";
            // Create the column list.
            final int n = CoreSubsystem.this.roles.size();
            this.columns = new ArrayList<Set<String>>(CoreSubsystem.this.roles.size());
            while (this.columns.size() < CoreSubsystem.this.roles.size())
                this.columns.add(EMPTY_CELL);
            for (int i = 0; i < n; i++) {
                // Get the role column.
                final int i2 = i + 2;
                String column = (i2 >= cols.length ? "" : cols[i2]);
                if (! column.isBlank()) {
                    // Here we have to parse the pegs.  Split up the peg specifiers (there is usually only 1).
                    String[] pegSpecs = StringUtils.split(column, ',');
                    Set<String> roleSet = new TreeSet<String>();
                    for (String pegSpec : pegSpecs) {
                        if (pegSpec.contains(".")) {
                            // A dot indicates it's not a peg, and has the type included.
                            roleSet.add(prefix + pegSpec);
                        } else {
                            // Otherwise it's a real peg.
                            roleSet.add(prefix + "peg." + pegSpec);
                        }
                    }
                    // Store the role set in the column cell.
                    this.columns.set(i, roleSet);
                }
            }
        }

        /**
         * @return the genomeId
         */
        public String getGenomeId() {
            return this.genomeId;
        }

        /**
         * @return the variantCode
         */
        public String getVariantCode() {
            return this.variantCode;
        }

        /**
         * @return TRUE if this is an inactive variant, else FALSE
         */
        public boolean isInactive() {
            return VariantId.computeActiveLevel(this.getVariantCode()).contentEquals("inactive");
        }

        /**
         * @return the list of columns
         */
        public List<Set<String>> getColumns() {
            return this.columns;
        }

        /**
         * @return the parent subsystem
         */
        public CoreSubsystem getParent() {
            return CoreSubsystem.this;
        }

        /**
         * @return the set of roles in this row
         */
        public Set<String> getRoles() {
            Set<String> retVal = new TreeSet<String>();
            for (int i = 0; i < this.columns.size(); i++) {
                if (! this.columns.get(i).isEmpty()) {
                    String roleId = CoreSubsystem.this.getRoleId(CoreSubsystem.this.getRole(i));
                    if (roleId != null)
                        retVal.add(roleId);
                }
            }
            return retVal;
        }

    }

    /**
     * Construct a core subsystem descriptor from a subsystem directory.
     *
     * @param inDir		subsystem directory to use
     * @param roleDefs	role definitions to use
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public CoreSubsystem(File inDir, StrictRoleMap roleDefs) throws IOException, ParseFailureException {
        this.subDir = inDir;
        // Clear the error counters.
        this.badRoles = 0;
        this.badIds = new TreeSet<String>();
        // Compute the real subsystem name.
        this.name = dirToName(inDir);
        log.info("Reading subsystem {}.", this.name);
        // Save the role definitions.
        this.roleMap = new StrictRoleMap();
        // Now get the classification and version.
        this.setClassification(inDir);
        // Initialize the rule namespace and the role list.
        this.ruleMap = new LinkedHashMap<String, SubsystemRule>();
        this.roles = new ArrayList<StrictRole>();
        this.roleAbbrs = new ArrayList<String>();
        // Initialize the tracking sets.
        this.found = new TreeSet<String>();
        this.notFound = new TreeSet<String>();
        // Read in the subsystem spreadsheet.  This will initialize the name space, collect the
        // rule list and auxiliary rules, and store the rows.
        this.spreadsheet = new HashMap<String, Row>();
        this.readSpreadsheet(roleDefs, inDir);
        // Now read in the notes.
        this.readNotes(inDir);
        // Compile the variant rules.
        this.variantRules = new LinkedHashMap<String, SubsystemRule>();
        this.readRules(inDir, "checkvariant_definitions", this.ruleMap);
        this.readRules(inDir, "checkvariant_rules", this.variantRules);
        log.info("Subsystem {} v{} has {} roles, {} variant rules, {} namespace rules, and {} spreadsheet rows.",
                this.name, this.version, this.roles.size(), this.variantRules.size(), this.ruleMap.size(),
                this.spreadsheet.size());
    }

    /**
     * This is a dummy core-subsystem to use as a placeholder.
     */
    public CoreSubsystem() {
        this.name = "(none)";
        this.good = false;
        this.auxRoles = Collections.emptySet();
        this.auxMap = new BitSet();
        this.badIds = Collections.emptySet();
        this.classes = Arrays.asList("", "", "");
        this.found = Collections.emptySet();
        this.notFound = Collections.emptySet();
        this.roleMap = new StrictRoleMap();
        this.roles = Collections.emptyList();
        this.roleAbbrs = Collections.emptyList();
        this.ruleMap = new LinkedHashMap<String, SubsystemRule>();
        this.variantRules = new LinkedHashMap<String, SubsystemRule>();
        this.spreadsheet = Collections.emptyMap();
        this.version = 1;
        this.initNoteData();
    }

    /**
     * Initialize the data normally taken from the notes file.
     */
    private void initNoteData() {
        this.note = "";
        this.description = "";
        this.variantNotes = new TreeMap<String, String>();
        this.pubmed = new TreeSet<Integer>();
    }

    /**
     * Now we read the subsystem spreadsheet, which contains the bulk of the data.  This includes the role list,
     * the auxiliary role set, and the genome rows.
     *
     * @param roleDefs	role definition map
     * @param inDir		subsystem directory
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    private void readSpreadsheet(StrictRoleMap roleDefs, File inDir) throws IOException, ParseFailureException {
        // The file has three sections, separated by "//" markers.
        File inFile = new File(inDir, "spreadsheet");
        try (LineReader reader = new LineReader(inFile)) {
            // Loop through the first section.  This has the roles.
            int ruleIdx = 1;
            for (String[] line : reader.new Section(SECTION_MARKER)) {
                // The line contains an abbreviation and a role name.  We convert the role name to an ID
                // and create a rule for the abbreviation.
                String roleName = line[1];
                StrictRole role = roleDefs.getByName(roleName);
                if (role == null) {
                    // The role is not in the map, which is a role error.
                    log.warn("Invalid role name \"{}\" found in {}.", roleName, inFile);
                    this.badRoles++;
                    role = new StrictRole("invalid", roleName);
                }
                // Create the rule for this role.
                var roleRule = new SubsystemPrimitiveRule(role.getId());
                // Connect it to the abbreviation.
                this.ruleMap.put(line[0], roleRule);
                roleRule.setTracking(this, line[0]);
                // Establish the role in the current column position.
                this.ruleMap.put(Integer.toString(ruleIdx), roleRule);
                this.roles.add(role);
                this.roleAbbrs.add(line[0]);
                ruleIdx++;
                // Add the role to the internal role map.
                var roles = roleDefs.getAllById(role.getId());
                this.roleMap.putAll(roles);
            }
            // The second section has the auxiliary roles.  These should be 1-based index numbers.
            this.auxRoles = new TreeSet<String>();
            this.auxMap = new BitSet(roles.size());
            for (String[] line : reader.new Section(SECTION_MARKER)) {
                if (line.length > 1 && line[0].toLowerCase().equals("aux")) {
                    // Here we have found the auxiliary roles definition.
                    // Loop through the role specifiers.
                    for (int i = 1; i < line.length; i++) {
                        String spec = line[i];
                        if (StringUtils.isNumeric(spec)) {
                            // Here we have a column index.
                            int idx = Integer.parseInt(spec);
                            if (idx <= 0 || idx > this.roles.size())
                                log.error("Invalid role index in aux role list.");
                            else {
                                this.auxRoles.add(this.roles.get(idx - 1).getId());
                                this.auxMap.set(idx - 1);
                            }
                        } else
                            throw new ParseFailureException("Non-numeric auxiliary role spec \"" + spec + "\" in subsystem "
                                    + this.name + ".");
                    }
                }
            }
            // The final section has the spreadsheet rows themselves.
            for (String[] line : reader.new Section(SECTION_MARKER)) {
                Row newRow = this.new Row(line);
                this.spreadsheet.put(newRow.getGenomeId(), newRow);
            }
        }
        log.info("Spreadsheet for {} contained {} roles ({} auxiliary) and {} rows.", this.name,
                this.roles.size(), this.auxRoles.size(), this.spreadsheet.size());
    }

    /**
     * Read in the notes file.  We have variant notes, description notes, and just plain notes.  All of these
     * are separated by a line of pound signs, with a label underneath in all caps indicating the content.
     *
     * @param inDir		subsystem input directory
     *
     * @throws IOException
     */
    private void readNotes(File inDir) throws IOException {
        // Clear all the notes data.
        this.initNoteData();
        // Insure we have a notes file in the first place.
        File noteFile = new File(inDir, "notes");
        if (noteFile.exists()) {
            try (LineReader noteStream = new LineReader(noteFile)) {
                // Here we have the note file.  Each section of the file begins with a line of pound signs.
                String line = noteStream.next();
                while (noteStream.hasNext()) {
                    // Here we are getting a section name.
                    line = noteStream.next();
                    // Process the section according to the name.
                    switch (line) {
                    case "DESCRIPTION" :
                        this.description = this.processNotesText(noteStream);
                        break;
                    case "NOTES" :
                        this.note = this.processNotesText(noteStream);
                        break;
                    case "VARIANTS" :
                        this.processNotesVariants(noteStream);
                        break;
                    default :
                        this.skipNotesText(noteStream);
                    }
                }
            }
        }
    }

    /**
     * Read a text section of the notes file.  We pass through the text unmodified, joining the
     * lines with new-line characters, only pausing to extract embedded pubmed IDs.
     *
     * @param noteStream	line reader for the notes file, positioned after a label line
     *
     * @return a single string with the note text
     */
    private String processNotesText(LineReader noteStream) {
        // We read in the lines until we hit a marker.  Each line is added to the line list
        // for the final join, but it is also parsed for pubmed IDs.
        boolean done = false;
        List<String> retVal = new ArrayList<String>();
        while (noteStream.hasNext() && ! done) {
            String line = noteStream.next();
            if (NOTES_MARKER.matcher(line).matches())
                done = true;
            else {
                retVal.add(line);
                // Here we parse for the pubmed references.  The "find" method will always get
                // the next one and fail when there are no more.
                Matcher m = PUBMED_REFERENCE.matcher(line);
                while (m.find()) {
                    // The pubmed ID is in group 1.
                    int pubmedId = Integer.valueOf(m.group(1));
                    this.pubmed.add(pubmedId);
                }
            }
        }
        // Join all the strings together and return the result.
        return StringUtils.join(retVal, '\n');
    }

    /**
     * Skip over an invalid or unused section of the notes file.
     *
     * @param noteStream	line reader for the notes file
     */
    private void skipNotesText(LineReader noteStream) {
        // The section ends when we hit end-of-file or read a marker line.
        boolean done = false;
        while (noteStream.hasNext() && ! done)
            done = NOTES_MARKER.matcher(noteStream.next()).matches();
    }

    /**
     * @return the role name for the specified spreadsheet column
     *
     * @param idx	column index
     */
    public String getRole(int idx) {
        return this.roles.get(idx).getName();
    }

    /**
     * @return the role abbreviation for the specified spreadsheet column
     * @param idx	column index
     */
    public String getRoleAbbr(int idx) {
        return this.roleAbbrs.get(idx);
    }

    /**
     * @return the bit map for the auxiliary roles
     */
    public BitSet getAuxMap() {
        return this.auxMap;
    }

    /**
     * This processes the variant notes.  Unlike a normal notes text section, the variants are a two-column
     * table consisting of variant codes and descriptions.
     *
     * @param noteStream	line reader for the notes file
     */
    private void processNotesVariants(LineReader noteStream) {
        // The section ends when we hit end-of-file or read a marker line.
        boolean done = false;
        while (noteStream.hasNext() && ! done) {
            String line = noteStream.next();
            if (NOTES_MARKER.matcher(line).matches())
                done = true;
            else {
                // Split on the first tab.  Note we ignore any line that is badly-formed.
                String[] pieces = StringUtils.split(line, "\t", 2);
                if (pieces.length == 2)
                    this.variantNotes.put(pieces[0], pieces[1]);
            }
        }
    }

    /**
     * Read a file of variant rules or definitions, and store them in the specified map.
     *
     * @param inDir			subsystem directory
     * @param fileName		name of the source file
     * @param targetMap		rule map to receive the parsed rules
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    private void readRules(File inDir, String fileName, Map<String, SubsystemRule> targetMap) throws IOException, ParseFailureException {
        // Compute the source file name.
        File inFile = new File(inDir, fileName);
        if (inFile.exists()) {
            log.info("Parsing rules in {}.", inFile);
            // Open the file for line-by-line reading.
            try (LineReader reader = new LineReader(inFile)) {
                // Loop through the rules.  Each is on a single input line.
                for (String line : reader) {
                    // Skip blank lines and comments.
                    if (! line.isBlank() && ! line.startsWith("#")) {
                        Matcher m = RULE_PATTERN.matcher(line);
                        if (! m.matches())
                            throw new ParseFailureException("Invalid rule line in \"" + inFile + "\".");
                        else {
                            // Now we parse the rule.  Note that the name space is the same regardless of the target
                            // map.
                            String key = m.group(1);
                            String rule = m.group(2);
                            RuleCompiler compiler = new RuleCompiler(rule, this.ruleMap);
                            SubsystemRule newRule = compiler.compiledRule();
                            targetMap.put(key, newRule);
                            this.badIds.addAll(compiler.getBadIds());
                        }
                    }
                }
            }
        }
    }

    /**
     * Read in the classifications for the subsystem.
     *
     * @param inDir		subsystem input directory
     */
    private void setClassification(File inDir) {
        String classData = MarkerFile.readSafe(new File(inDir, "CLASSIFICATION"));
        this.classes = new ArrayList<String>(3);
        String[] pieces = StringUtils.splitPreserveAllTokens(classData, '\t');
        // Copy the classification pieces to the classification list.  There are always
        // supposed to be 3.  If we find an experimental class, we mark the subsystem as bad.
        this.good = true;
        for (int i = 0; i < pieces.length; i++) {
            String piece = pieces[i];
            if (piece.isBlank())
                this.classes.add("");
            else {
                this.classes.add(piece);
                if (piece.toLowerCase().startsWith("experimental") ||
                        piece.toLowerCase().startsWith("clustering"))
                    this.good = false;
            }
        }
        for (int i = pieces.length; i < 3; i++)
            this.classes.add("");
        if (this.good) {
            // If it's still good, check for exchangable.
            File exchangeMarker = new File(inDir, "EXCHANGABLE");
            if (! exchangeMarker.exists())
                this.good = false;
        }
        // Finally, get the version.
        File versionMarker = new File(inDir, "VERSION");
        this.version = MarkerFile.readInt(versionMarker);
    }

    /**
     * @return the name of the subsystem in the specified directory
     *
     * @param subDir	subsystem directory of interest
     */
    public static String dirToName(File subDir) {
        String name = subDir.getName();
        final int n = name.length();
        StringBuilder retVal = new StringBuilder(n);
        // Loop through the name, converting the translated characters.
        int i = 0;
        while (i < n) {
            char chr = name.charAt(i);
            switch (chr) {
            case '_' :
                // Underscores are encoded from spaces.
                retVal.append(' ');
                i++;
                break;
            case '%' :
                // Percent signs are used for hex encodings.
                String hex = name.substring(i + 1, i + 3);
                retVal.append((char) Integer.parseInt(hex, 16));
                i += 3;
                break;
            default :
                retVal.append(chr);
                i++;
            }
        }
        // Check for the pathological space trick.
        int last = retVal.length() - 1;
        while (last > 0 && retVal.charAt(last) == ' ') {
            retVal.setCharAt(last, '_');
            last--;
        }
        return retVal.toString();
    }
    /**
     * @return the name of the subsystem
     */
    public String getName() {
        return this.name;
    }
    /**
     * @return the subsystem version number
     */
    public int getVersion() {
        return this.version;
    }
    /**
     * @return TRUE if this is a good subsystem
     */
    public boolean isGood() {
        return this.good;
    }

    /**
     * @return the subsystem's superclass
     */
    public String getSuperClass() {
        return this.classes.get(0);
    }

    /**
     * @return the subsystem's class
     */
    public String getMiddleClass() {
        return this.classes.get(1);
    }

    /**
     * @return the subsystem's subclass
     */
    public String getSubClass() {
        return this.classes.get(2);
    }

    /**
     * @return a displayable classification string for this subsystem
     */
    public String getClassification() {
        String retVal = this.classes.stream().filter(x -> ! x.isBlank()).collect(Collectors.joining(", "));
        return retVal;
    }

    /**
     * @return TRUE if the specified role ID is an auxiliary role in this subsystem
     *
     * @param roleId	ID of the role of interest
     */
    public boolean isAuxRole(String roleId) {
        return this.auxRoles.contains(roleId);
    }

    /**
     * @return the variant code of the specified genome in the spreadsheet, or NULL if it is not in the spreadsheet
     *
     * @param genomeId	ID of the genome of interest
     */
    public String variantOf(String genomeId) {
        String retVal = null;
        Row row = this.spreadsheet.get(genomeId);
        if (row != null)
            retVal = row.getVariantCode();
        return retVal;
    }

    /**
     * @return the set of feature IDs assigned to this subsystem for the specified genome row
     *
     * @param genomeId	ID of the genome of interest
     */
    public Set<String> fidSetOf(String genomeId) {
        Set<String> retVal = EMPTY_CELL;
        Row row = this.spreadsheet.get(genomeId);
        if (row != null)
            retVal = row.columns.stream().flatMap(x -> x.stream()).collect(Collectors.toSet());
        return retVal;
    }

    /**
     * @return the number of genome rows in the spreadsheet
     */
    public int getRowCount() {
        return this.spreadsheet.size();
    }

    /**
     * @return an iterator through the spreadsheet rows
     */
    public Iterator<Row> rowIterator() {
        return this.spreadsheet.values().iterator();
    }

    /**
     * @return the set of variant codes for this subsystem
     */
    public Set<String> getVariantCodes() {
        Set<String> retVal = new TreeSet<String>();
        // Add all the variant codes we used.
        for (Row row : this.spreadsheet.values())
            retVal.add(row.variantCode);
        // Add any unused variant codes that have rules.
        retVal.addAll(this.variantRules.keySet());
        // Return the full set.
        return retVal;
    }

    /**
     * Create the rule files for this subsystem and compile the rules into this object.
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public void createRules() throws IOException, ParseFailureException {
        if (this.hasRules())
            throw new IOException("Cannot create rules for a subsystem with existing rules.");
        // Create the rule generator.
        log.info("Generating rules for {}.", this.name);
        RuleGenerator ruleGen = new RuleGenerator(this);
        List<String> variantRules = ruleGen.getVariantRules();
        List<String> definitions = ruleGen.getGroupDefinitions();
        log.info("Writing rules to {}.", this.subDir);
        final File defFile = new File(this.subDir, "checkvariant_definitions");
        try (PrintWriter defStream = new PrintWriter(defFile)) {
            for (String definition : definitions)
                defStream.println(definition);
        }
        final File ruleFile = new File(this.subDir, "checkvariant_rules");
        try (PrintWriter ruleStream = new PrintWriter(ruleFile)) {
            for (String rule : variantRules)
                ruleStream.println(rule);
        }
        log.info("Compiling rules.");
        this.readRules(this.subDir, "checkvariant_definitions", this.ruleMap);
        this.readRules(this.subDir, "checkvariant_rules", this.variantRules);
    }

    /**
     * Compute the variant code for a specified role set in this subsystem.
     *
     * @param roleSet	set of roles in the genome
     *
     * @return the optimal variant code, or NULL if no variant applies
     */
    public String applyRules(Set<String> roleSet) {
        // Get an iterator through the variant rules.  Because it is a linked hash map, we will get
        // them in order.
        Iterator<Map.Entry<String, SubsystemRule>> iter = this.variantRules.entrySet().iterator();
        String retVal = null;
        // Loop and stop on the first matching rule.
        while (iter.hasNext() && retVal == null) {
            var ruleEntry = iter.next();
            SubsystemRule rule = ruleEntry.getValue();
            if (rule.check(roleSet))
                retVal = ruleEntry.getKey();
        }
        // Clear the tracking sets.  These are only available after an analyzeRule.
        this.found.clear();
        this.notFound.clear();
        // Return the results.
        return retVal;
    }

    /**
     * @return the variant rule map
     */
    protected LinkedHashMap<String, SubsystemRule> getVariantRuleMap() {
        return this.variantRules;
    }

    /**
     * Create the role set for a genome.
     *
     * @param genome	genome of interest
     * @param roleMap2	role definition structure
     *
     * @return a set of the role IDs for the genome's roles
     */
    public static Set<String> getRoleSet(Genome genome, StrictRoleMap roleMap2) {
        Set<String> retVal = new HashSet<String>(genome.getFeatureCount());
        for (Feature feat : genome.getFeatures())
            roleMap2.usefulRoles(feat.getFunction()).stream().forEach(x -> retVal.add(x.getId()));
        return retVal;
    }

    /**
     * @return the list of subsystem directories for a CoreSEED instance
     *
     * @param coreDir	CoreSEED data directory name
     *
     * @throws IOException
     */
    public static List<File> getSubsystemDirectories(File coreDir) throws IOException {
        File subMaster = new File(coreDir, "Subsystems");
        if (! subMaster.isDirectory())
            throw new FileNotFoundException(coreDir + " does not appear to be a CoreSEED data directory.");
        File[] subFiles = subMaster.listFiles(DIR_SS_FILTER);
        return Arrays.asList(subFiles);
    }

    /**
     * @return the number of bad rule identifiers
     */
    public int getBadIdCount() {
        return this.badIds.size();
    }

    /**
     * @return the set of bad rule identifiers
     */
    public Set<String> getBadIds() {
        return this.badIds;
    }

    /**
     * @return the number of bad role names
     */
    public int getBadRoleCount() {
        return this.badRoles;
    }

    /**
     * @return the set of genomes in the spreadsheet
     */
    public Set<String> getRowGenomes() {
        return this.spreadsheet.keySet();
    }

    /**
     * @return the number of roles in this subsystem
     */
    public int getRoleCount() {
        return this.roles.size();
    }

    /**
     * @return TRUE if there are variant rules for this subsystem
     */
    public boolean hasRules() {
        return this.variantRules.size() > 0;
    }

    /**
     * @return TRUE if the specified variant code has a rule
     *
     * @param vCode		variant code to check
     */
    public boolean isRuleVariant(String vCode) {
        return this.variantRules.containsKey(vCode);
    }

    /**
     * Record the results of a primitive-rule match.
     *
     * @param abbr		role abbreviation
     * @param retVal	TRUE if the role was found, else FALSE
     */
    public void record(String abbr, boolean retVal) {
        if (retVal)
            this.found.add(abbr);
        else
            this.notFound.add(abbr);
    }

    /**
     * Analyze the specified rule to determine what was found and not found.
     *
     * @param ruleName		name of the rule to analyze
     * @param roleSet		set of roles to use
     *
     * @return a string containing the abbreviations of the found roles, a slash, and the abbreviations of the missing roles
     *
     * @throws ParseFailureException
     */
    public String analyzeRule(String ruleName, Set<String> roleSet) throws ParseFailureException {
        String retVal;
        // Insure the tracking sets are empty.
        this.notFound.clear();
        this.found.clear();
        // Test the rule.
        SubsystemRule rule = this.variantRules.get(ruleName);
        if (rule == null)
            retVal = "<no match>";
        else {
            rule.check(roleSet);
            retVal = StringUtils.join(this.found, ",") + "/" + StringUtils.join(this.notFound, ",");
        }
        return retVal;
    }

    /**
     * @return the rule with the given name, or NULL if there is none
     *
     * @param name 		name of the desired rule
     */
    public SubsystemRule getRule(String name) {
        SubsystemRule retVal = this.variantRules.get(name);
        if (retVal == null)
            retVal = this.ruleMap.get(name);
        return retVal;
    }

    /**
     * This can be used immediately after an "analyzeRule" to get the roles not found.
     *
     * @return the roles not-found
     */
    public Set<String> getNotFound() {
        return this.notFound;
    }

    /**
     * This can be used immediately after an "analyzeRule" to get the roles found.
     *
     * @return the roles found
     */
    public Set<String> getFound() {
        return this.found;
    }

    /**
     * @return the main rule name space for this subsystem
     */
    protected Map<String, SubsystemRule> getNameSpace() {
        return this.ruleMap;
    }

    /**
     * @return a map from role IDs to names for this subsystem
     */
    public Map<String, String> getOriginalNameMap() {
        final int n = this.roles.size();
        Map<String, String> retVal = new HashMap<String, String>(n * 4 / 3 + 1);
        for (int i = 0; i < n; i++) {
            StrictRole role = this.roles.get(i);
            String roleId = role.getId();
            String roleName = role.getName();
            retVal.put(roleId, roleName);
        }
        return retVal;
    }

    /**
     * This method will look at a role and return TRUE if it exactly matches one of the subsystem roles, else FALSE.
     *
     * @param roleId		ID of the role to check
     * @param roleString	role string to check
     *
     * @return TRUE if the role string exactly matches a subsystem role, else FALSE
     */
    public boolean isExactRole(String roleId, String roleString) {
        StrictRole role = this.roleMap.getItem(roleId);
        boolean retVal;
        if (role == null)
            retVal = false;
        else
            retVal = StringUtils.equals(roleString, role.getName());
        return retVal;
    }

    /**
     * This method will look at a role string and return the role ID if the role is in this subsystem, else NULL.
     *
     * @param roleString	role string to check
     *
     * @return the ID of the role if it is in the subsystem, else NULL
     */
    public String getRoleId(String roleString) {
        String retVal = null;
        StrictRole role = this.roleMap.getByName(roleString);
        if (role != null)
            retVal = role.getId();
        return retVal;
    }

    /**
     * @return the expected role string for the role with the specified ID
     *
     * @param roleId	ID of the desired role
     *
     * @return the actual name of the role in this subsystem, or NULL if the role ID is not found
     */
    public String getExpectedRole(String roleId) {
        String retVal = null;
        // Find the role ID in the role list.
        int i = 0;
        final int n = this.roles.size();
        while (i < n && ! this.roles.get(i).getId().contentEquals(roleId))
            i++;
        if (i < n)
            retVal = this.roles.get(i).getName();
        return retVal;
    }

    /**
     * @return the description notes
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return the pubmed ID set
     */
    public Set<Integer> getPubmed() {
        return this.pubmed;
    }

    /**
     * @return the note text
     */
    public String getNote() {
        return this.note;
    }

    /**
     * @return the map of variant IDs to variant notes (for variants with notes)
     */
    public Map<String, String> getVariantNotes() {
        return this.variantNotes;
    }

    /**
     * @return a JSON object containing the high-level subsystem data
     */
    public JsonObject getSubsystemJson() {
        JsonObject retVal = new JsonObject();
        retVal.put("subsystem_name", this.name);
        retVal.put("version", this.version);
        retVal.put("superclass", this.getSuperClass());
        retVal.put("class", this.getMiddleClass());
        retVal.put("subclass", this.getSubClass());
        retVal.put("description", this.description);
        retVal.put("notes", this.note);
        // If there are pubmed IDs, put them in as an array.
        if (! this.pubmed.isEmpty()) {
            JsonArray pubmeds = new JsonArray();
            pubmeds.addAll(this.pubmed);
            retVal.put("pmid", pubmeds);
        }
        // If there are rules, add them here.
        if (! this.ruleMap.isEmpty()) {
            JsonArray ruleDefs = new JsonArray();
            for (var ruleEntry : this.ruleMap.entrySet()) {
                String ruleKey = ruleEntry.getKey();
                String ruleText = ruleEntry.getValue().toString();
                // Here is our clumsy way to get rid of the internal rules.
                if (! ruleText.equals(ruleKey) && ! StringUtils.isNumeric(ruleKey))
                    ruleDefs.add(ruleKey + " means " + ruleText);
            }
            retVal.put("rule_defs", ruleDefs);
        }
        // Finally, list the roles and role IDs in order.
        JsonArray roleList = new JsonArray();
        JsonArray idList = new JsonArray();
        for (int i = 0; i < this.roles.size(); i++) {
            StrictRole role = this.roles.get(i);
            final String roleId = role.getId();
            String realName = this.roleMap.get(roleId);
            roleList.add(realName);
            idList.add(this.roleAbbrs.get(i));
        }
        retVal.put("role_abbrs", idList);
        retVal.put("role_names", roleList);
        return retVal;
    }

    /**
     * Compute the set of genomes that use a particular variant code.
     *
     * @param variantCode	variant code of interest
     *
     * @return the set of genomes in the spreadsheet that use the specified variant
     */
    public Set<String> getVariantGenomes(String variantCode) {
        Set<String> retVal = new TreeSet<String>();
        for (Row row : this.spreadsheet.values()) {
            if (row.variantCode.contentEquals(variantCode))
                retVal.add(row.genomeId);
        }
        return retVal;
    }

    /**
     * Compute a list of the subsystems in a CoreSEED directory, with an optional filter file.
     *
     * @param coreDir		CoreSEED data directory
     * @param filterFile	name of a tab-delimited file with headerscontaining subsystem names in the first
     * 						column, or NULL to use all the subsystems
     *
     * @return a list of the subsystem directories to use
     *
     * @throws IOException
     */
    public static List<File> getFilteredSubsystemDirectories(File coreDir, File filterFile) throws IOException {
        // Get the subsystem directory list.  This will also validate the coreSEED input directory.
        log.info("Scanning for subsystems in {}.", coreDir);
        List<File> retVal = getSubsystemDirectories(coreDir);
        // Check for a filter file.
        if (filterFile == null)
            log.info("No subsystem filtering specified.");
        else {
            log.info("Filter file {} specified.", filterFile);
            if (! filterFile.canRead())
                throw new FileNotFoundException("Filter file " + filterFile + " is not found or unreadable.");
            Set<String> ssNames = TabbedLineReader.readSet(filterFile, "1");
            // Now we must validate the filter subsystems.
            Set<String> realSubs = retVal.stream().map(x -> CoreSubsystem.dirToName(x))
                    .collect(Collectors.toSet());
            for (String filterSub : ssNames) {
                if (! realSubs.contains(filterSub)) {
                    log.error("Subsystem \"{}\" not found in subsystem directory.", filterSub);
                }
            }
            // Now create a version of the subsystem directory list that only contains the filtered
            // ones.
            List<File> allDirs = retVal;
            retVal = allDirs.stream().filter(x -> ssNames.contains(CoreSubsystem.dirToName(x)))
                    .collect(Collectors.toList());
        }
        return retVal;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CoreSubsystem)) {
            return false;
        }
        CoreSubsystem other = (CoreSubsystem) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Find the row for a specified genome.
     *
     * @param genomeId	ID of genome whose row is desired
     *
     * @return the row for that genome, or NULL if none
     */
    public Row getRowOf(String genomeId) {
        return this.spreadsheet.get(genomeId);
    }
}
