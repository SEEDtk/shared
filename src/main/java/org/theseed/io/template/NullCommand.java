/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.TabbedLineReader.Line;

/**
 * This command produces no output, and is used to suppress extra spaces in multi-line templates.
 * Such templates typically put spaces between lines except when the line starts with a command.
 *
 * @author Bruce Parrello
 *
 */
public class NullCommand implements ITemplateCommand {

    @Override
    public int getIfEffect() {
        return 0;
    }

    @Override
    public boolean requiresIf() {
        return false;
    }

    @Override
    public String translate(LineTemplate template, Line line) {
        return "";
    }

}
