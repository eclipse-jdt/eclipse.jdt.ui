package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

/**
 * Reads the text contents from a reader of HTML contents and translates 
 * the tags or cut them out.
 */
public class HTML2TextReader extends SubstitutionTextReader {
		
	private static HashMap fgHTMLTagLookup;
	private static HashMap fgEntityLookup;
	
	static {
		fgHTMLTagLookup= new HashMap(7);
		fgHTMLTagLookup.put("p", LINE_DELIM); //$NON-NLS-1$
		fgHTMLTagLookup.put("br", LINE_DELIM); //$NON-NLS-1$
		fgHTMLTagLookup.put("dt", LINE_DELIM);	// definition title in definition list //$NON-NLS-1$
		fgHTMLTagLookup.put("dd", " ");		// definition doc in definition list //$NON-NLS-1$ //$NON-NLS-2$
		fgHTMLTagLookup.put("li", LINE_DELIM + "· "); //$NON-NLS-2$ //$NON-NLS-1$
		
		fgEntityLookup= new HashMap(7);
		fgEntityLookup.put("lt", "<"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("gt", ">"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("nbsp", " "); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("amp", "&"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("circ", "^"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("tilde", "~"); //$NON-NLS-2$ //$NON-NLS-1$
		fgEntityLookup.put("quot", "\"");		 //$NON-NLS-1$ //$NON-NLS-2$
	}
	public HTML2TextReader(Reader reader) {
		super(reader);
	}
		
	/**
	 * @see SubstitutionTextReader#computeSubstitution(char)
	 */
	protected String computeSubstitution(int c) throws IOException {
		if (c == '<')
			return processHTMLTag();
		else if (c == '&')
			return processEntity();
		else
			return null;
	}
	
	protected String html2Text(String htmlTag) {
		String text= (String) fgHTMLTagLookup.get(htmlTag);
		return (text == null ? "" : text);
	}
	
	/*
	 * A '<' has been read. Process a html tag
	 */ 
	private String processHTMLTag() throws IOException {
		StringBuffer buf= new StringBuffer();
		int ch;
		do {		
			ch= nextChar();
			while (ch != -1 && ch != '>') {
				// to be done: skip strings
				buf.append(Character.toLowerCase((char)ch));
				ch= nextChar();
			}
			
			if (ch == -1)
				return null;
			
			int tagLen= buf.length();
			// needs special treatment for comments 
			if ((tagLen >= 3 && "!--".equals(buf.substring(0, 3))) //$NON-NLS-1$
				&& !(tagLen >= 5 && "--".equals(buf.substring(tagLen - 2)))) { //$NON-NLS-1$
				// unfinished comment
				buf.append(ch);
			} else {
				break;
			}
		} while (true);
		 
		return html2Text(buf.toString());
	}

	protected String entity2Text(String symbol) {
		if (symbol.length() > 1 && symbol.charAt(0) == '#') {
			int ch;
			try {
				if (symbol.charAt(1) == 'x') {
					ch= Integer.parseInt(symbol.substring(2), 16);
				} else {
					ch= Integer.parseInt(symbol.substring(1), 10);
				}
				return "" + (char)ch; //$NON-NLS-1$
			} catch (NumberFormatException e) {
			}
		} else {
			String str= (String) fgEntityLookup.get(symbol);
			if (str != null) {
				return str;
			}
		}
		return "&" + symbol; // not found //$NON-NLS-1$
	}
	
	/*
	 * A '&' has been read. Process a entity
	 */ 	
	private String processEntity() throws IOException {
		StringBuffer buf= new StringBuffer();
		int ch= nextChar();
		while (Character.isLetterOrDigit((char)ch) || ch == '#') {
			buf.append((char) ch);
			ch= nextChar();
		}
		
		if (ch == ';') 
			return entity2Text(buf.toString());
		
		buf.insert(0, '&');
		if (ch != -1)
			buf.append((char) ch);
		return buf.toString();
	}
}