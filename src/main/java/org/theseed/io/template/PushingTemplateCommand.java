/**
 *
 */
package org.theseed.io.template;

/**
 * This is the base class for a template command that starts a block.  The block is the first sub-command.
 *
 * @author Bruce Parrello
 *
 */
public abstract class PushingTemplateCommand extends TemplateCommand {

    /**
     * Compile a pushing template command.  The command is stacked along with a block.  The block
     * will eventually be the first parameter.
     *
     * @param template	master template controller
     */
    public PushingTemplateCommand(LineTemplate template) {
        super(template);
        template.push(this);
        // Set up a block to hold the commands in this command's first scope.
        template.push(new BlockCommand(template));
    }

}
