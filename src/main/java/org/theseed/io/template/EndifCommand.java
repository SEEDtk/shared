/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.TabbedLineReader.Line;

/**
 * This command terminates an if-context, revealing the previous context.
 *
 * @author Bruce Parrello
 *
 */
public class EndifCommand implements ITemplateCommand {

    @Override
    public int getIfEffect() {
        return -1;
    }

    @Override
    public boolean requiresIf() {
        return true;
    }

    @Override
    public String translate(LineTemplate template, Line line) {
        template.pop();
        return "";
    }

}
