/**
 *
 */
package org.theseed.io.template;

import java.util.List;

import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;

/**
 * The group command simplifies the creation of sentences involving several fields that have similar purposes.  The
 * command itself uses a list of column names, and the group only outputs if at least one column is nonblank.
 *
 * @author Bruce Parrello
 *
 */
public class GroupCommand implements ITemplateCommand {

    // FIELDS
    /** source input stream (only kept until END is compiled) */
    private FieldInputStream sourceStream;
    /** saved conjunction */
    private String conjunction;
    /** list of column names from clauses (only kept until END is compiled) */
    private List<String> columnNames;
    /** array of column indices, in clause order (created after END is compiled) */
    private int[] colIdxes;
    /** index number of currently-active clause (translation time only) */
    private int clauseIdx;
    /** accumulated list of clauses (output by END command) */
    private List<String> clauses;


    /**
     * Start compiling a group.  The supplied parameter string is the conjunction (usually "and" or "or").
     *
     * @param group		conjunction to use with two or more output clauses
     * @param inStream	source input stream
     */
    public GroupCommand(String parms, FieldInputStream inStream) {
        this.conjunction = conjunction;
        this.sourceStream = inStream;
    }

    @Override
    public int getIfEffect() {
        return 1;
    }

    @Override
    public boolean requiresIf() {
        return false;
    }

    @Override
    public String translate(LineTemplate template, Record line) {
        // TODO determine if we are generating
        // TODO clear clauses
        // TODO code for translate group
        return null;
    }

}
