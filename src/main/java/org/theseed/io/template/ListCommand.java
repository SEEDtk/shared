/**
 *
 */
package org.theseed.io.template;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.theseed.io.TabbedLineReader;
import org.theseed.io.TabbedLineReader.Line;
import org.theseed.utils.ParseFailureException;

import java.io.IOException;
import java.util.regex.Matcher;

/**
 * This command converts a list of strings into text.  The column content comes in as a single string, and we
 * need to know the separator being used in order to split it up.  The list elements are then strung together
 * using a conjunction ("and" or "or").
 *
 * @author Bruce Parrello
 *
 */
public class ListCommand implements ITemplateCommand {

    // FIELDS
    /** index of the column relevant to the if-command */
    private int colIdx;
    /** conjunction to use at end of list */
    private String conjunction;
    /** separator to use for splitting list */
    private String separator;
    /** match pattern for parsing list specification */
    private static final Pattern LIST_SPEC = Pattern.compile("(\\w+):([^:]+):(.+)");

    /**
     * Construct a list command.
     *
     * @param columnSpec	list specification (conjunction, colon, column name, colon, separator)
     * @param inStream		input stream
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    public ListCommand(String columnSpec, TabbedLineReader inStream) throws ParseFailureException, IOException {
        Matcher m = LIST_SPEC.matcher(columnSpec);
        if (! m.matches())
            throw new ParseFailureException("Invalid list specification \"" + columnSpec + "\".");
        this.conjunction = m.group(1);
        this.colIdx = inStream.findField(m.group(2));
        this.separator = m.group(3);
    }

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
        String retVal;
        // Only proceed if we have an active context.
        if (! template.peek())
            retVal = "";
        else {
            // Get the column content and split it.
            String columnValue = line.get(this.colIdx);
            String[] pieces = StringUtils.splitByWholeSeparator(columnValue, this.separator);
            switch (pieces.length) {
            case 0:
                retVal = "";
                break;
            case 1:
                retVal = pieces[0];
                break;
            case 2:
                retVal = pieces[0] + " " + this.conjunction + " " + pieces[1];
                break;
            default :
                StringBuffer buffer = new StringBuffer(columnValue.length() + pieces.length * 2 + conjunction.length() + 10);
                final int n = pieces.length - 1;
                for (int i = 0; i < n; i++)
                    buffer.append(pieces[i]).append(this.separator);
                buffer.append(" " + this.conjunction + " " + pieces[n]);
                retVal = buffer.toString();
                break;
            }
        }
        return retVal;
    }

}
