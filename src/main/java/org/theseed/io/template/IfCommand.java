/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.theseed.io.TabbedLineReader;
import org.theseed.io.TabbedLineReader.Line;

/**
 * This processes an if-command.  Currently, the if-command pushes a context of TRUE if the
 * column content is non-blank.
 *
 * @author Bruce Parrello
 *
 */
public class IfCommand implements ITemplateCommand {

    // FIELDS
    /** index of the column relevant to the if-command */
    private int colIdx;

    /**
     * Create an if-command based on the specified column.
     *
     * @param columnName	name of the conditional column
     *
     * @throws IOException
     */
    public IfCommand(String columnName, TabbedLineReader inStream) throws IOException {
        // Compute the column index for the target column.
        this.colIdx = inStream.findField(columnName);
    }

    @Override
    public int getIfEffect() {
        return 1;
    }

    @Override
    public boolean requiresIf() {
        return false;
    }

    @Override
    public String translate(LineTemplate template, Line line) {
        // Get the value of the conditional column.
        String colValue = line.get(this.colIdx);
        // Push TRUE if it is nonblank, else FALSE.
        template.push(! StringUtils.isBlank(colValue));
        return "";
    }

}
