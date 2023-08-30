/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * the group is treated as a conditional at runtime). Inside the group, the "clause" command generates text
 * if the specified column is nonblank.  (The column name should be specified as a parameter.) At the
 * end, the clauses are assembled and put into a long sentence with commas and the conjunction if necessary,
 * with a final period.
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
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(LineTemplate.class);
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
        this.compileStack.push(new BlockCommand(this,"block"));
        final int len = template.length();
        log.info("Compiling {}-character template string.", len);
        // We parse the template into tokens.  There are literals, variables, and commands.  The unparsed section
        // of the string is stored as the residual.  We set up a try-block so we can output the neighborhood of the
        // error.
        String residual = template;
        try {
            while(! residual.isEmpty()) {
                // Look for the next variable or command.
                Matcher m = VARIABLE.matcher(residual);
                if (! m.matches()) {
                    // Here the entire remainder of the template is a literal.
                    TemplateCommand residualCommand = new LiteralCommand(this, residual);
                    this.addToTop(residualCommand);
                } else {
                    // Here group 1 is the initial literal, group 2 is a variable or command, and group 3
                    // is the new residual.
                    String prefix = m.group(1);
                    String construct = m.group(2);
                    if (! prefix.isEmpty()) {
                        TemplateCommand prefixCommand = new LiteralCommand(this, prefix);
                        this.addToTop(prefixCommand);
                    }
                    // Is this a command or a variable reference?
                    if (construct.charAt(0) != '$') {
                        // Here we have a variable reference.
                        TemplateCommand varCommand = new ColumnCommand(this, construct, inStream);
                        this.addToTop(varCommand);
                    } else {
                        // Here we have a special command and we need to decode it.
                        Matcher m2 = COMMAND.matcher(construct);
                        if (! m2.matches())
                            throw new ParseFailureException("Invalid special command \"" + construct + "\".");
                        // Set up some local variables for use below.
                        TemplateCommand newCommand = null;
                        switch (m2.group(1)) {
                        case "product" :
                            // This command translates a gene product.
                            newCommand = new GeneProductCommand(this, inStream, m2.group(2));
                            this.addToTop(newCommand);
                            break;
                        case "0" :
                            // The null command does nothing, so it has no effect.
                            break;
                        case "list" :
                            // This command turns a field containing a list into a comma-separated
                            // phrase.
                            newCommand = new ListCommand(this, inStream, m2.group(2));
                            this.addToTop(newCommand);
                            break;
                        case "if" :
                            // This command starts an if-block.
                            newCommand = new IfCommand(this, inStream, m2.group(2));
                            this.addAndPush(newCommand);
                            // Start a block to cover the THEN clause.
                            this.addAndPush(new BlockCommand(this, "if"));
                            break;
                        case "else" :
                            // This command starts a block that executes when the IF is false.
                            // We first need to pop off a then-block.  The next method fails if
                            // the context is not IF.
                            this.popInContext("else", "if");
                            // Now create the ELSE and connect it to the IF.
                            newCommand = new BlockCommand(this, "else");
                            this.addAndPush(newCommand);
                            break;
                        case "fi" :
                            // This command terminates the scope of an IF-construct. We must
                            // insure we are in the scope of an if-construct.  Pop off the
                            // currently-active block command and verify we are in a valid
                            // context.
                            this.popInContext("fi", "if", "else");
                            // Pop off the IF itself.
                            this.pop();
                            break;
                        case "group" :
                            // The group command allows the template to create a conjuncted list of
                            // complex phrases.  The group consists of a prefix and a set of clauses.
                            // We need to construct the group command and then push on a block command
                            // for the prefix.
                            newCommand = new GroupCommand(this, m2.group(2));
                            this.addAndPush(newCommand);
                            this.addAndPush(new BlockCommand(this, "group"));
                            break;
                        case "clause" :
                            // The clause command indicates a conditional section of the group.
                            // Pop off the current block command and verify we are in a valid
                            // context.
                            this.popInContext("clause", "group", "clause");
                            // Create the clause command and add it to the group.
                            newCommand = new ClauseCommand(this, inStream, m2.group(2));
                            this.addAndPush(newCommand);
                            break;
                        case "end" :
                            // This command ends a group construct.  Pop off the currently-active
                            // block command and verify we are in a valid context.
                            this.popInContext("end", "group", "clause");
                            // Pop off the GROUP itself.
                            this.pop();
                            break;
                        default :
                            throw new ParseFailureException("Unknown special command \"" + m2.group(1) + "\".");
                        }
                    }
                    // Update the residual.
                    residual = m.group(3);
                }
            }
            if (this.compileStack.size() > 1)
                throw new ParseFailureException("Unclosed " + this.peek().getName() + " command in template.");
        } catch (ParseFailureException e) {
            int pos = len - residual.length();
            int start = pos - 20;
            if (start < 0) start = 0;
            int end = pos + 20;
            if (end > len) end = len;
            log.error("Parsing error encountered near \"{}\".", template.substring(start, end));
            log.error("Parser message: {}", e.getMessage());
            throw new ParseFailureException(e);
        }
    }

    private void popInContext(String newName, String... context) throws ParseFailureException {
        TemplateCommand top = this.pop();
        final String name = top.getName();
        if (! Arrays.stream(context).anyMatch(x -> x.contentEquals(name)))
            throw new ParseFailureException("\"" + newName + "\" command found outside of proper context.");
    }

    /**
     * Add a new subcommand to the top command on the compile stack.
     *
     * @param subCommand	subcommand to add
     *
     * @throws ParseFailureException
     */
    private void addToTop(TemplateCommand subCommand) throws ParseFailureException {
        TemplateCommand top = this.compileStack.peek();
        if (top == null)
            throw new ParseFailureException("Mismatched block construct.");
        top.addCommand(subCommand);
    }

    /**
     * Add a new subcommand to the top command of the compile stack and push it on
     * to start a new context.
     *
     * @param subCommand	subcommand to add and push
     */
    private void addAndPush(TemplateCommand subCommand) throws ParseFailureException {
        this.addToTop(subCommand);
        this.push(subCommand);
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
    private void push(TemplateCommand command) {
        this.compileStack.push(command);

    }

    /**
     * Pop a command off the command stack.
     *
     * @return the command on top of the stack.
     */
    private TemplateCommand pop() {
        return this.compileStack.pop();
    }

    /**
     * @return the top command on the stack without popping it
     */
    protected TemplateCommand peek() {
        return this.compileStack.peek();
    }

    /**
     * @return the index for the specified column
     *
     * @param colName	name of the column desired
     * @param inStream	source input stream
     *
     * @throws ParseFailureException
     *
     */
    protected int findField(String colName, FieldInputStream inStream) throws ParseFailureException {
        int retVal;
        try {
            retVal = inStream.findField(colName);
        } catch (IOException e) {
            // Convert a field-not-found to a parsing exception.
            throw new ParseFailureException("Could not find field \"" + colName + "\" in source input stream.");
        }
        return retVal;
    }

    /**
     * Form a list of phrases into an english-language list using a conjunction.
     *
     * @param conjunction	conjunction for the final phrase
     * @param phrases		list of phrases
     *
     * @return a string representation of the list
     */
    public static String conjunct(String conjunction, List<String> phrases) {
        String retVal;
        final int n = phrases.size() - 1;
        switch (phrases.size()) {
        case 0:
            retVal = "";
            break;
        case 1:
            retVal = phrases.get(0);
            break;
        case  2:
            retVal = phrases.get(0) + " " + conjunction + " " +  phrases.get(1);
            break;
        default:
            //  Here we need to assemble the phrases with the conjunction between the last two.
            int len = 10 + phrases.size() * 2 + phrases.stream().mapToInt(x -> x.length()).sum();
            StringBuilder buffer = new StringBuilder(len);
            IntStream.range(0, n).forEach(i -> buffer.append(phrases.get(i)).append(", "));
            buffer.append(" ").append(conjunction).append(" ").append(phrases.get(n));
            retVal = buffer.toString();
        }
        return retVal;
    }

}
