/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.jdt.internal.core.Assert;

public class TemplateInterpolator {

	/*
	 * EBNF grammar.
	 *
	 * template := (text | escape)*.
	 * text := character - dollar.
	 * escape := dollar ('{' identifier '}' | dollar).
	 * dollar := '$'.
	 */	 

	// state
	private static final int TEXT= 0;
	private static final int ESCAPE= 1;
	private static final int IDENTIFIER= 2;

	private static final char ESCAPE_CHARACTER= '$';
	private static final char IDENTIFIER_BEGIN= '{';
	private static final char IDENTIFIER_END= '}';
	
	/**
	 * Interpolates a string.
	 */
	public String interpolate(String string, VariableEvaluator evaluator) {
		StringBuffer buffer= new StringBuffer(string.length());
		StringBuffer identifier= new StringBuffer();
		int state= TEXT;
				
		for (int i= 0; i != string.length(); i++) {
			char ch= string.charAt(i);
			
			switch (state) {
			case TEXT:
				switch (ch) {
				case ESCAPE_CHARACTER:
					state= ESCAPE;
					break;
					
				default:
					buffer.append(ch);
					break;
				}
				break;
				
			case ESCAPE:
				switch (ch) {
				case ESCAPE_CHARACTER:
					buffer.append(ch);
					state= TEXT;
					break;
				
				case IDENTIFIER_BEGIN:	
					identifier.setLength(0);
					state= IDENTIFIER;
					break;
					
				default:
					// XXX grammar would not allow occurence of single escape characters
					buffer.append(ESCAPE_CHARACTER);
					buffer.append(ch);
					state= TEXT;
					break;
				}
				break;

			case IDENTIFIER:
				switch (ch) {
				case IDENTIFIER_END:
					{
						String value= evaluator.evaluate(identifier.toString(), buffer.length());
					
						if (value == null) {
							// leave variable untouched
							buffer.append(ESCAPE_CHARACTER);
							buffer.append(IDENTIFIER_BEGIN);
							buffer.append(identifier);
							buffer.append(IDENTIFIER_END);
						} else {							
							buffer.append(value);
						}
					}
					state= TEXT;
					break;
				
				default:
					identifier.append(ch);
					break;
				}
				break;
			}			
		}
		
		return buffer.toString();
	}

}

