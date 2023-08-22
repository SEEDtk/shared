/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.theseed.io.TabbedLineReader;

/**
 * This template command outputs the content of the named column in the current input line.
 *
 * @author Bruce Parrello
 *
 */
public class ColumnCommand implements ITemplateCommand {

    // FIELDS
    /** index of target column */
    private int colIdx;

    /**
     * Construct a column-content template command.
     *
     * @param columnName	index (1-based) or name of the desired column
     * @param inStream		input file stream
     *
     * @throws IOException
     */
    public ColumnCommand(String columnName, TabbedLineReader inStream) throws IOException {
        this.colIdx = inStream.findField(columnName);
    }

    @Override
    public String translate(LineTemplate template, TabbedLineReader.Line line) {
        String retVal;
        if (template.peek())
            retVal = StringUtils.strip(line.get(this.colIdx));
        else
            retVal = "";
        return retVal;
    }

    @Override
    public int getIfEffect() {
        return 0;
    }

    @Override
    public boolean requiresIf() {
        return false;
    }

}
