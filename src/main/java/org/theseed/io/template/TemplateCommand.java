/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.FieldInputStream.Record;
import org.theseed.utils.ParseFailureException;

/**
 * This is the base class for template commands.  It handles the process of tracking the length
 * estimate and exposes the methods needed by the template processor.
 *
 * @author Bruce Parrello
 *
 */
public abstract class TemplateCommand {

    // FIELDS
    /** estimated length of the command output */
    private int estimatedLength;
    /** controlling template */
    private LineTemplate masterTemplate;

    /**
     * Compile a new template command.
     *
     * @param template	controlling command template
     */
    public TemplateCommand(LineTemplate template) {
        this.estimatedLength = 0;
        this.masterTemplate = template;
    }

    /**
     * @return the estimated output length for this block
     */
    protected int getEstimatedLength() {
        return this.estimatedLength;
    }

    /**
     * Specify the estimated length for this block.
     *
     * @param newLength		proposed new estimated length
     */
    protected void setEstimatedLength(int newLength) {
        this.estimatedLength = newLength;
    }

    /**
     * Add the estimated length of the specified command to this one's.
     *
     * @param command		sub-command whose length should be added
     */
    protected void addEstimatedLength(TemplateCommand command) {
        this.estimatedLength += command.getEstimatedLength();
    }

    /**
     * Merge the estimated length of the specified command with this one's.
     * The larger length is kept.
     *
     * @param command		sub-command whose length should be merged
     */
    protected void mergeEstimatedLength(TemplateCommand command) {
        int newLength = command.getEstimatedLength();
        if (newLength > this.estimatedLength)
            this.estimatedLength = newLength;
    }

    /**
     * Add a new sub-command.
     *
     * @param command	sub-command to add
     */
    protected abstract void addCommand(TemplateCommand command);

    /**
     * @param template
     * @param line
     * @return
     */
    protected abstract String translate(Record line);

    /**
     * Push a new command onto the master template's compile stack.
     *
     *  @param command	command to push
     */
    public void push(TemplateCommand command) {
        var template = this.masterTemplate;
        template.push(command);
    }

    /**
     * Close a command block.
     *
     * @throws ParseFailureException
     */
    public TemplateCommand pop() throws ParseFailureException {
        var template = this.masterTemplate;
        TemplateCommand retVal = template.pop();
        return retVal;
    }
}
