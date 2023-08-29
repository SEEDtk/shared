/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.FieldInputStream.Record;

/**
 * @author Bruce Parrello
 *
 */
public class ClauseCommand implements ITemplateCommand {

    /**
     * @param activeGroup
     * @param parms
     */
    public ClauseCommand(GroupCommand activeGroup, String parms) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int getIfEffect() {
        // TODO code for getIfEffect
        return 0;
    }

    @Override
    public boolean requiresIf() {
        // TODO code for requiresIf
        return false;
    }

    @Override
    public String translate(LineTemplate template, Record line) {
        // TODO code for translate
        return null;
    }
    // FIELDS
    // TODO data members for ClauseCommand

    // TODO constructors and methods for ClauseCommand
}
