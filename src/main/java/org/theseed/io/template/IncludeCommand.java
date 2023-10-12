/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.utils.ParseFailureException;

/**
 * This command extracts the expanded template text based on a link-field value and emits it.  The
 * global cache may return multiple results, in which case they are strung together with a
 * conjunction in the manner used by ListCommand.
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
    /** conjunction for multiple results */
    private String conjunction;

    /**
     * Create an include command.
     *
     * @param lineTemplate		source template
     * @param inStream			input file stream
     * @param fileName			global data file name
     * @param linkFieldName		link field name in the input file
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public IncludeCommand(LineTemplate lineTemplate, FieldInputStream inStream, String parms)
            throws IOException, ParseFailureException {
        super(lineTemplate);
        String[] pieces = StringUtils.splitByWholeSeparator(parms, ":");
        if (pieces.length < 2)
            throw new ParseFailureException("Invalid include command-- missing file name or key field.");
        this.linkIdx = inStream.findField(pieces[1]);
        this.fileName = pieces[0];
        if (pieces.length < 3)
            this.conjunction = "and";
        else
            this.conjunction = pieces[2];
    }

    @Override
    protected String translate(Record line) {
        String linkValue = line.get(this.linkIdx);
        List<String> strings = this.getMasterTemplate().getGlobal(this.fileName, linkValue);
        String retVal = LineTemplate.conjunct(this.conjunction, strings);
        return retVal;
    }

    @Override
    protected String getName() {
        return "include";
    }

}
