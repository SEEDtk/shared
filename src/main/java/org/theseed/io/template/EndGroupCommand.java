/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.FieldInputStream.Record;

/**
 * @author Bruce Parrello
 *
 */
public class EndGroupCommand implements ITemplateCommand {

    /**
     * @param activeGroup
     */
    public EndGroupCommand(GroupCommand activeGroup) {
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
    // TODO data members for EndGroupCommand

    // TODO constructors and methods for EndGroupCommand
}
