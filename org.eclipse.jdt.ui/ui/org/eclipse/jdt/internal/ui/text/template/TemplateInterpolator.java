/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

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

	// tokens
	private static final char ESCAPE_CHARACTER= '$';
	private static final char IDENTIFIER_BEGIN= '{';
	private static final char IDENTIFIER_END= '}';
	
	/**
	 * Interpolates a string.
	 */
	public void interpolate(String string, VariableEvaluator evaluator) {
		StringBuffer text= new StringBuffer();
		int state= TEXT;
		
		evaluator.reset();
				
		for (int i= 0; i != string.length(); i++) {
			char ch= string.charAt(i);
			
			switch (state) {
			case TEXT:
				switch (ch) {
				case ESCAPE_CHARACTER:
					state= ESCAPE;
					break;
					
				default:
					text.append(ch);
					break;
				}
				break;
				
			case ESCAPE:
				switch (ch) {
				case ESCAPE_CHARACTER:
					text.append(ch);
					state= TEXT;
					break;
				
				case IDENTIFIER_BEGIN:
					// flush text
					evaluator.acceptText(text.toString());

					// transition to variable identifier				
					text.setLength(0);
					state= IDENTIFIER;
					break;
					
				default:
					// illegal single escape character, but be tolerant
					evaluator.acceptError(TemplateMessages.getString("TemplateInterpolator.error.incomplete.variable")); //$NON-NLS-1$
					text.append(ESCAPE_CHARACTER);
					text.append(ch);
					state= TEXT;
					break;
				}
				break;

			case IDENTIFIER:
				switch (ch) {
				case IDENTIFIER_END:
					// flush variable
					evaluator.acceptVariable(text.toString());						

					// transition to text
					text.setLength(0);						
					state= TEXT;
					break;
				
				default:
					if (!Character.isUnicodeIdentifierStart(ch) &&
						!Character.isUnicodeIdentifierPart(ch))
					{
						// illegal identifier character
						evaluator.acceptError(TemplateMessages.getString("TemplateInterpolator.error.invalid.identifier")); //$NON-NLS-1$
					}
				
					text.append(ch);
					break;
				}
				break;
			}			
		}
		
		switch (state) {
		case TEXT:
			evaluator.acceptText(text.toString());
			break;
		
		// illegal, but be tolerant
		case ESCAPE:
			evaluator.acceptError(TemplateMessages.getString("TemplateInterpolator.error.incomplete.variable")); //$NON-NLS-1$
			text.append(ESCAPE_CHARACTER);
			evaluator.acceptText(text.toString());
			break;
				
		// illegal, but be tolerant
		case IDENTIFIER:
			evaluator.acceptError(TemplateMessages.getString("TemplateInterpolator.error.incomplete.variable")); //$NON-NLS-1$
			text.append(ESCAPE_CHARACTER);
			evaluator.acceptText(text.toString());
			break;		
		}
	}

}

