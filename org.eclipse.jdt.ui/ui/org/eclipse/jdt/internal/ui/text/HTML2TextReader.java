package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import org.eclipse.jface.text.TextPresentation;


/**
 * Reads the text contents from a reader of HTML contents and translates 
 * the tags or cut them out.
 */
public class HTML2TextReader extends SubstitutionTextReader {
	
	
	private static final String LINE_DELIM= System.getProperty("line.separator", "\n");
		
	private static HashMap fgEntityLookup;
	private static List fgTags;
	
	static {
		
		fgTags= new ArrayList();
		fgTags.add("b");
		fgTags.add("h5");
		fgTags.add("p");
		fgTags.add("dl");
		fgTags.add("dt");
		fgTags.add("dd");
		fgTags.add("li");
		fgTags.add("ul");
		
		fgEntityLookup= new HashMap(7);
		fgEntityLookup.put("lt", "<"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("gt", ">"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("nbsp", " "); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("amp", "&"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("circ", "^"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("tilde", "~"); //$NON-NLS-2$ //$NON-NLS-1$
		fgEntityLookup.put("quot", "\"");		 //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private int fCounter= 0;
	private TextPresentation fTextPresentation;
	private int fBold= 0;
	private int fStartOffset= -1;
	
	
	public HTML2TextReader(Reader reader, TextPresentation presentation) {
		super(reader);
		fTextPresentation= presentation;
	}
	
	public int read() throws IOException {
		int c= super.read();
		if (c != -1)
			++ fCounter;
		return c;
	}
	
	protected void startBold() {
		if (fBold == 0)
			fStartOffset= fCounter;
		++ fBold;
	}
	
	protected void stopBold() {
		-- fBold;
		if (fBold == 0) {
			fTextPresentation.addStyleRange(new StyleRange(fStartOffset, fCounter - fStartOffset, null, null, SWT.BOLD));
			fStartOffset= -1;
		}
	}
	
	/**
	 * @see SubstitutionTextReader#computeSubstitution(char)
	 */
	protected String computeSubstitution(int c) throws IOException {
		
		if (c == '<')
			return  processHTMLTag();
		else if (c == '&')
			return processEntity();
		
		return null;
	}
	
	private String html2Text(String html) {
		
		String tag= html;
		if ('/' == tag.charAt(0))
			tag= tag.substring(1);
			
		if (!fgTags.contains(tag))
			return "";
			
		if ("b".equals(html)) {
			startBold();
			return "";
		}
				
		if ("h5".equals(html) || "dt".equals(html)) {
			startBold();
			return "";
		}
		
		if ("dl".equals(html))
			return LINE_DELIM;
		
		if ("dd".equals(html))
			return "\t";
		
		if ("li".equals(html))
			return LINE_DELIM + "\t" + "- ";
					
		if ("/b".equals(html)) {
			stopBold();
			return "";
		}
		
		if ("/p".equals(html))
			return LINE_DELIM;
			
		if ("/h5".equals(html) || "/dt".equals(html)) {
			stopBold();
			return LINE_DELIM;
		}
		
		if ("/dd".equals(html))
			return LINE_DELIM;
				
		return "";
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