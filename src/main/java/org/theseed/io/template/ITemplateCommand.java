/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.TabbedLineReader;

/**
 * This interface describes a template command.  Any object that wishes to serve as a command
 * processor must support this interface.
 *
 * @author Bruce Parrello
 *
 */
public interface ITemplateCommand {

    /**
     * @return 1 if this command increases the IF-stack, -1 if it decreases the IF-stack, and 0 if it has no effect
     */
    public int getIfEffect();

    /**
     * @return TRUE if this command requires a non-empty IF-stack, else FALSE
     */
    public boolean requiresIf();

    /**
     * @return the translated string for the command
     *
     * @param template	source template structure
     * @param line		input line to use as the source
     */
    public String translate(LineTemplate template, TabbedLineReader.Line line);
}
