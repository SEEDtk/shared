/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.utils.ParseFailureException;

/**
 * This is the most basic and common command.  It outputs the content of a named column
 * on the current line.
 *
 * @author Bruce Parrello
 *
 */
public class ColumnCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** index of column to output */
    private int colIdx;
    /** name of column to output */
    private String name;
    /** default expected output size */
    private static int DEFAULT_COLUMN_SIZE = 20;

    /**
     * Construct the column command.
     *
     * @param template	master template
     * @param colName	name of column to output
     * @param inStream	source input stream
     *
     * @throws ParseFailureException
     */
    public ColumnCommand(LineTemplate template, String colName, FieldInputStream inStream) throws ParseFailureException {
        super(template);
        this.setEstimatedLength(DEFAULT_COLUMN_SIZE);
        // Find the column index for the input stream.
        this.colIdx = template.findField(colName, inStream);
        // Save the column name.
        this.name = colName;
    }

    @Override
    protected String translate(Record line) {
        return line.get(this.colIdx);
    }

    @Override
    protected String getName() {
        return this.name;
    }

}
