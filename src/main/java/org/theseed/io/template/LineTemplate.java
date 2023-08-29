/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.theseed.io.FieldInputStream;
import org.theseed.utils.ParseFailureException;



/**
 * This object manages a template that can be used to translate a line from a tab-delimited file into
 * a text string.  The template string contains variables which are column names surrounded by double braces.
 * Each such variable is replaced by the corresponding column value in the current line.
 *
 * Special commands are handled by a dollar sign and a command name.  If the command requires a variable or
 * expression, that follows the command with a colon separator.  Conditionals are handled by the special
 * commands "if", "else", and "fi".  "if" takes a column name as its argument, and the body is only
 * output if the column is nonblank.  "else" produces output if the column was blank, and "fi" terminates
 * the conditional.  Besides conditionals, we have the following special commands
 *
 * 	list		takes as input a conjunction, a column name, a colon, and a separator string.  The column is
 * 				split on the separator string, and then formed into a comma-separated list using the conjunction
 *  0			produces no output
 *  product		takes as input two column names separated by a colon.  The first column is presumed to be a gene
 *  			product, and is parsed into multiple English sentences accordingly.  THe second column should
 *  			contain the code for the feature type, which is included in the description and affects the
 *  			text.
 *
 * The template string is parsed into a list of commands, the most common commands being either a literal string
 * or a column index.  This command list can then be processed rapidly to form the result string.
 *
 * @author Bruce Parrello
 *
 */
public class LineTemplate {

    // FIELDS
    /** list of commands to process */
    private List<ITemplateCommand> commands;
    /** initial size of string buffer */
    private int bufferSize;
    /** boolean stack for conditionals */
    private BitSet ifStack;
    /** index of top position in the stack */
    private int stackSize;
    /** search pattern for variables */
    protected static final Pattern VARIABLE = Pattern.compile("(.*?)\\{\\{(.+?)\\}\\}(.*)");
    /** search pattern for special commands */
    protected static final Pattern COMMAND = Pattern.compile("\\$(\\w+)(?::(.+))?");


    /**
     * Construct a line template for the specified tab-delimited file and the specified template string.
     *
     * @param inStream	tab-delimited file stream
     * @param template	template string
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public LineTemplate(FieldInputStream inStream, String template) throws IOException, ParseFailureException {
        // Estimate the buffer size.
        this.bufferSize = template.length();
        // Initialize the if-stack.  It starts with an always-on bit at the bottom.
        this.ifStack = new BitSet();
        this.stackSize = 0;
        this.ifStack.set(0);
        // Create a list to store the commands.
        this.commands = new ArrayList<ITemplateCommand>();
        // This will track how many unclosed IF commands we have found.
        int ifLevel = 0;
        // Denote we are parsing the whole template.
        String residual = template;
        while (! residual.isEmpty()) {
            Matcher m = VARIABLE.matcher(residual);
            if (! m.matches()) {
                // Here we have stuff at the end.
                ITemplateCommand residualCommand = new LiteralCommand(residual);
                commands.add(residualCommand);
                residual = "";
            } else {
                // Here we have a variable.  Group 1 is the literal prefix, group 2 the variable name,
                // and group 3 the residual.
                String prefix = m.group(1);
                String columnName = m.group(2);
                if (! prefix.isEmpty()) {
                    // Here we have a real prefix.
                    ITemplateCommand prefixCommand = new LiteralCommand(prefix);
                    commands.add(prefixCommand);
                }
                // Now we have to handle the non-literal command.
                if (columnName.charAt(0) != '$') {
                    // Here we have a plain column substitution.
                    ITemplateCommand varCommand = new ColumnCommand(columnName, inStream);
                    this.commands.add(varCommand);
                    // Increase the buffer size for this variable.
                    this.bufferSize += 10;
                } else {
                    // Here we have a special command.
                    Matcher m2 = COMMAND.matcher(columnName);
                    if (! m2.matches())
                        throw new ParseFailureException("Invalid special command \"" + columnName + "\".");
                    // We will put the command handler in here.
                    ITemplateCommand varCommand;
                    switch (m2.group(1)) {
                    case "if" :
                        varCommand = new IfCommand(m2.group(2), inStream);
                        break;
                    case "else" :
                        varCommand = new ElseCommand();
                        break;
                    case "fi" :
                        varCommand = new EndifCommand();
                        break;
                    case "list" :
                        varCommand = new ListCommand(m2.group(2), inStream);
                        break;
                    case "product" :
                        varCommand = new GeneProductCommand(m2.group(2), inStream);
                        break;
                    case "0" :
                        varCommand = new NullCommand();
                        break;
                    default :
                        throw new ParseFailureException("Unknown special command \"" + columnName + "\".");
                    }
                    // Validate the command.
                    if (varCommand.requiresIf() && ifLevel <= 0)
                        throw new ParseFailureException("Command \"" + columnName + "\" must be in an IF-context.");
                    ifLevel += varCommand.getIfEffect();
                    // Add the command to the list.
                    this.commands.add(varCommand);
                }
                // Loop for the remainder of the template.
                residual = m.group(3);
            }
        }
        // Check the final if-balance.
        if (ifLevel != 0)
            throw new ParseFailureException("Unbalanced IF-context in template string.");
    }

    /**
     * This method applies the template to the current input line.
     *
     * @param line		input line to process
     *
     * @return the result of applying the template to the input line
     */
    public String apply(FieldInputStream.Record line) {
        // Create the string builder.
        StringBuffer retVal = new StringBuffer(this.bufferSize);
        // Apply each command to the input line.
        for (var command : this.commands) {
               retVal.append(command.translate(this, line));
        }
        // Return the result.
        return retVal.toString();
    }

    /**
     * Push a flag onto the if-stack.  Note that if the top if-context if FALSE, the context
     * pushed will always be FALSE.  This insures that everything inside a false context is
     * skipped.
     *
     * @param flag	flag to push
     */
    public void push(boolean flag) {
        boolean pushValue = this.peek() && flag;
        this.stackSize++;
        this.ifStack.set(this.stackSize, pushValue);
    }

    /**
     * @return the current if-context
     */
    public boolean peek() {
        return this.ifStack.get(this.stackSize);
    }

    /**
     * If the previous if-context is TRUE, flip the current if-context.  This is what we want to do
     * when we hit an ELSE command.
     */
    public void elseFlip() {
        if (this.ifStack.get(this.stackSize - 1))
            this.ifStack.flip(this.stackSize);
    }

    /**
     * Pop the current if-context, revealing the previous context.
     */
    public void pop() {
        this.stackSize--;
    }

}
