/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;

import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;

/**
 * This command extracts the expanded template text based on a link-field value and emits it.
 *
 * @author Bruce Parrello
 *
 */
public class IncludeCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** index of the link field in the input line */
    private int linkIdx;
    /** file name to use to get the global data */
    private String fileName;

    /**
     * Create an include command.
     *
     * @param lineTemplate		source template
     * @param inStream			input file stream
     * @param fileName			global data file name
     * @param linkFieldName		link field name in the input file
     *
     * @throws IOException
     */
    public IncludeCommand(LineTemplate lineTemplate, FieldInputStream inStream, String fileName, String linkFieldName)
            throws IOException {
        super(lineTemplate);
        this.linkIdx = inStream.findField(linkFieldName);
        this.fileName = fileName;
    }

    @Override
    protected String translate(Record line) {
        String linkValue = line.get(this.linkIdx);
        return this.getMasterTemplate().getGlobal(this.fileName, linkValue);
    }

    @Override
    protected String getName() {
        return "include";
    }

}
