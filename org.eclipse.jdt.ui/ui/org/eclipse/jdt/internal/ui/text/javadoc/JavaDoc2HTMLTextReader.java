package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.IOException;
import java.io.Reader;

import org.eclipse.jdt.internal.ui.text.SubstitutionTextReader;


/**
 * Processes JavaDoc tags.
 */
public class JavaDoc2HTMLTextReader extends SubstitutionTextReader {
	
	public JavaDoc2HTMLTextReader(Reader reader) {
		super(reader);
	}
	
	/*
	 * A '@' has been read. Process a jdoc tag
	 */ 			
	private String processJavaDocTag() throws IOException {
		StringBuffer buf= new StringBuffer("@"); //$NON-NLS-1$
		int ch= nextChar();
		while (ch != -1 && Character.isLetter((char) ch)) {
			buf.append((char)ch);
			ch= nextChar();
		}
		
		if (!"@link".equals(buf.toString()))  //$NON-NLS-1$
			buf.insert(0, "<p>");
			
		buf.append((char) ch);
		return buf.toString();
	}
	
	/*
	 * @see SubstitutionTextReaderr#computeSubstitution(int)
	 */
	protected String computeSubstitution(int c) throws IOException {
		if (c == '@')
			return processJavaDocTag();
		return null;
	}
}