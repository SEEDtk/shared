/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.FieldInputStream;

/**
 * This template command inserts a literal string into the output.
 *
 * @author Bruce Parrello
 *
 */
public class LiteralCommand implements ITemplateCommand {

    // FIELDS
    /** literal string value */
    private String value;

    /**
     * Construct a literal command.
     *
     * @param literal	literal string to output
     */
    public LiteralCommand(String literal) {
        this.value = literal;
    }

    @Override
    public String translate(LineTemplate template, FieldInputStream.Record line) {
        String retVal;
        if (template.peek())
            retVal = this.value;
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
