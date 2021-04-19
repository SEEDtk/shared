package org.theseed.subsystems;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.genome.SubsystemRow;
import org.theseed.magic.MagicMap;
import org.theseed.magic.MagicObject;

/**
 * This object describes a subsystem as it is implemented in a genome.  It contains the subsystem name,
 * an ID for the subsystem, the three classifiers, and a list of the features in the subsystem.  It is
 * constructed from the SubsystemRow object in the Genome object.  The row descriptor is used to parse
 * the subsystems of a genome and produce an output file that can be read back later and used to associate
 * features with subsystems.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemRowDescriptor extends MagicObject {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SubsystemRowDescriptor.class);

    /** classification list */
    private List<String> classes;
    /** list of features in the subsystem */
    private Set<String> fidList;
    /** ID for serialization */
    private static final long serialVersionUID = 6584400701063296597L;
    /** delimiter for feature IDs */
    public static String FID_DELIM = ",";
    /** delimiter for classifications */
    public static String CLASS_DELIM = " | ";

    /**
     * Construct a blank subsystem row descriptor.
     */
    public SubsystemRowDescriptor() {
        super();
    }

    /**
     * Construct a subsystem row descriptor.
     *
     * @param row			subsystem row from a genome
     * @param magicTable	master table for creating IDs
     */
    public SubsystemRowDescriptor(SubsystemRow row, MagicMap<SubsystemRowDescriptor> magicTable) {
        // Store the subsystem name.
        this.setName(row.getName());
        // Store the classifiers.
        this.classes = row.getClassifications();
        // Loop through the roles, creating the feature ID list.
        this.fidList = row.getRoles().stream().flatMap(r -> r.getFeatures().stream()).map(f -> f.getId()).collect(Collectors.toSet());
        // Add this object to the table.  This creates the ID.
        magicTable.put(this);
    }

    @Override
    protected String normalize() {
        // A tiny difference in a subsystem name is always significant, so there is no normalizing.
        // Seriously, curators will add extra spaces or capitalize differently to indicate a new
        // version of an existing system.
        return this.getName();
    }

    /**
     * Write this object to an output file.
     *
     * @param writer	output writer to contain the object
     */
    public void write(PrintWriter writer) {
        // Write the id, name, classification, and feature IDs.
        writer.format("%s\t%s\t%s\t%s%n", this.getId(), this.getName(), StringUtils.join(this.classes, CLASS_DELIM),
                StringUtils.join(this.fidList, FID_DELIM));
    }

    /**
     * Create a file containing a genome's subsystem information.
     *
     * @param genome		genome to process
     * @param outFile		output file to contain descriptor records
     *
     * @throws IOException
     */
    public static void createFile(Genome genome, File outFile) throws IOException {
        SubsystemRowDescriptor blank = new SubsystemRowDescriptor();
        try (PrintWriter writer = new PrintWriter(outFile)) {
            // Create a magic map to generate compact subsystem IDs.
            MagicMap<SubsystemRowDescriptor> subMap = new MagicMap<SubsystemRowDescriptor>(blank);
            // Loop through the subsystem rows in the genome, writing descriptors.
            int count = 0;
            for (SubsystemRow row : genome.getSubsystems()) {
                SubsystemRowDescriptor desc = new SubsystemRowDescriptor(row, subMap);
                desc.write(writer);
                count++;
            }
            log.info("{} subsystems written for {}.", count, genome);
        }
    }

}
