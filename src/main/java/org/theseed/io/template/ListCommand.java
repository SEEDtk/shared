/**
 *
 */
package org.theseed.io.template;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.utils.ParseFailureException;

/**
 * This command handles a field containing a list.  The list can bean actual list
 * or a delimited list.  The list itself is output as a comma-separated phrase with
 * a conjunction ("and" or "or").
 */
public class ListCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** index of the column containing the list */
    private int colIdx;
    /** conjunction to use at end of list */
    private String conjunction;
    /** separator string, or NULL if the field is a list */
    private String separator;
    /** expected size of list output */
    private static final int DEFAULT_LIST_OUTPUT = 40;

    /**
     * Construct a list command.
     *
     * @param template	controlling master template
     * @param inStream	source input stream
     * @param parms		parameters (colon-separated): field name, conjunction, optional separator string
     *
     * @throws ParseFailureException
     */
    public ListCommand(LineTemplate template, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(template);
        this.setEstimatedLength(DEFAULT_LIST_OUTPUT);
        // Parse the parameters.
        String[] pieces = StringUtils.split(parms, ":");
        if (pieces.length <= 0)
            throw new ParseFailureException("List command requires parameters.");
        else {
            // Get the input field index.
            this.colIdx = template.findField(pieces[0], inStream);
            if (pieces.length < 2) {
                // The conjunction defaults to "and".
                this.conjunction = "and";
                this.separator = null;
            } else {
                // Save the conjunction.
                this.conjunction = pieces[1];
                if (pieces.length < 3) {
                    // Here we retrieve the column as a list.
                    this.separator = null;
                } else if (pieces.length == 3)
                    this.separator = pieces[2];
                else
                    throw new ParseFailureException("Too many parameters on list command.");
            }
        }
    }

    @Override
    protected String translate(Record line) {
        // Get the column data.
        List<String> pieces;
        if (this.separator == null)
            pieces = line.getList(this.colIdx);
        else
            pieces = Arrays.asList(StringUtils.splitByWholeSeparator(line.get(this.colIdx), this.separator));
        return LineTemplate.conjunct(this.conjunction, pieces);
    }

    @Override
    protected String getName() {
        return "list";
    }

}
