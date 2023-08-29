/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
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
 * the conditional.
 *
 * There is a second kind of conditional called the group.  The group starts with the "group" command and
 * end with an "end" command.  It takes as input a conjunction, usually "and" or "or".  Each clause in the
 * group is associated with a column name.  The group is only generated if at least one column is nonblank (so
 * the group is treated as a conditional at runtime).
 * Inside the group, the "clause" command generates text if the specified column is nonblank.  (The column
 * name should be specified as a parameter.)  The specification of the column name At the
 * end, the clauses are assembled and put into a long sentence with commas and an and-separator if necessary.
 *
 * Besides conditionals, we have the following special commands
 *
 * 	list		takes as input a conjunction, a column name, a colon, and a separator string.  The column is
 * 				split on the separator string, and then formed into a comma-separated list using the conjunction.
 * 				If the separator string is omitted, the column is retrieved as a list.
 *  0			produces no output
 *  product		takes as input two column names separated by a colon.  The first column is presumed to be a gene
 *  			product, and is parsed into multiple English sentences accordingly.  THe second column should
 *  			contain the code for the feature type, which is included in the description and affects the
 *  			text.
 *
 * The template string is parsed into a list of commands.  This command list can then be processed rapidly
 * to form the result string.
 *
 * @author Bruce Parrello
 *
 */
public class LineTemplate {

    // FIELDS
    /** compiled template */
    private TemplateCommand compiledTemplate;
    /** compile stack */
    private Deque<TemplateCommand> compileStack;

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
        // Initialize the compile stack.
        this.compileStack = new ArrayDeque<TemplateCommand>();
        this.compileStack.push(new BlockCommand(this));
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
        return this.compiledTemplate.translate(line);
    }

    /**
     * Push a new command onto the compile stack.
     *
     * @param command	command to push
     */
    public void push(TemplateCommand command) {
        this.compileStack.push(command);

    }

    /**
     * Pop a command off the command stack.
     *
     * @return the command on top of the stack.
     */
    public TemplateCommand pop() {
        return this.compileStack.pop();
    }

}
