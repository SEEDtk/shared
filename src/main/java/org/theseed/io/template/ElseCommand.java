/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.FieldInputStream;

/**
 * THis command processes ELSE.  The ELSE command flips the if-context if the underlying if-context is TRUE.
 * In other words, if we are not being suppressed by a larger if-context, we only output if the current if-context
 * is FALSE.
 *
 * @author Bruce Parrello
 *
 */
public class ElseCommand implements ITemplateCommand {

    @Override
    public int getIfEffect() {
        return 0;
    }

    @Override
    public boolean requiresIf() {
        return true;
    }

    @Override
    public String translate(LineTemplate template, FieldInputStream.Record line) {
        template.elseFlip();
        return "";
    }

}
