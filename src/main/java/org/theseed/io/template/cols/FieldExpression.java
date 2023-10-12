/**
 * 
 */
package org.theseed.io.template.cols;

import java.io.IOException;

import org.theseed.io.FieldInputStream;
import org.theseed.io.template.LineTemplate;
import org.theseed.utils.ParseFailureException;

/**
 * This is the base class for a field expression.  Field expressions are used in conditionals, and the
 * expression class must be able to parse the expression and evaluate it to TRUE or FALSE.
 * 
 * The simplest field expression is simply a column name.  It is also, however, possible to have a function.
 * The function consists of the function name, a left parenthesis, a comma-delimited parameter list, and a
 * right parenthesis.
 * 
 * @author Bruce Parrello
 *
 */
public class FieldExpression {
	
	// FIELDS
	/** controlling line template */
	private LineTemplate template;
	

	/**
	 * Create a field expression from an expression string.
	 * 
	 * @param template		master line template
	 * @param inStream		relevant field input stream
	 * @param exp			expression string
	 * 
	 * @throws ParseFailureExcpression
	 * @throws IOException
	 */
	public static FieldExpression compile(LineTemplate template, FieldInputStream inStream, String expression)
			throws IOException, ParseFailureException {
		
		return null;
	}
	
	// TODO constructors and methods for FieldExpression
}
