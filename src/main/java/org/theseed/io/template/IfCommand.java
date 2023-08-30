/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.utils.ParseFailureException;

/**
 * This is the basic IF command.  The command has as its sole parameter a column name.
 * It executes its THEN-block when the column evaluates to TRUE and executes the ELSE
 * block when the column evaluates to FALSE.
 */
public class IfCommand extends TemplateCommand {

    // FIELDS
    /** index of the condition column */
    private int colIdx;
    /** then-clause */
    private TemplateCommand thenClause;
    /** else-clause */
    private TemplateCommand elseClause;

    /**
     * Construct the IF-block.
     *
     * @param template	controlling master template
     * @param inStream	source input stream
     * @param parms		parameter, consisting of a column name
     *
     * @throws ParseFailureException
     */
    public IfCommand(LineTemplate template, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(template);
        this.colIdx = template.findField(parms, inStream);
        // Both clauses are null.  The first subcommand is THEN, the second is ELSE, and
        // any others are an error.
        this.thenClause = null;
        this.elseClause = null;
    }

    @Override
    protected void addCommand(TemplateCommand command) throws ParseFailureException {
        // Note that the estimated length will be the larger of the two estimates.
        if (this.thenClause == null) {
            this.thenClause = command;
            this.setEstimatedLength(command.getEstimatedLength());
        } else if (this.elseClause == null) {
            this.elseClause = command;
            this.mergeEstimatedLength(command);
        } else
            throw new ParseFailureException("Too many clauses for IF.");
    }

    @Override
    protected String translate(Record line) {
        String retVal = "";
        // Get the condition column.
        boolean flag = line.getFlag(this.colIdx);
        // Execute the appropriate clause if it exists.
        if (flag && this.thenClause != null)
            retVal = this.thenClause.translate(line);
        else if (! flag && this.elseClause != null)
            retVal = this.elseClause.translate(line);
        return retVal;
    }

    @Override
    protected String getName() {
        return "if";
    }

}
