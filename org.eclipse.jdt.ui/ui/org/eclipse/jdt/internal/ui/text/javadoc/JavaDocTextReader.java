package org.eclipse.jdt.internal.ui.text.javadoc;
import java.util.HashMap;

import java.io.IOException;
import java.io.Reader;


/**
 * Reads the text contents from a reader of a JavaDoc (HTML) content
 * Has a mapping for tags, skips all others.
 */
public class JavaDocTextReader extends SingleCharReader {
	
	private static final String LINE_DELIM= System.getProperty("line.separator", "\n");
	private static final String JDOC_TAG_BEGIN= LINE_DELIM + '@';
	
	private static HashMap fgHTMLTagLookup;
	private static HashMap fgEntityLookup;
	
	static {
		fgHTMLTagLookup= new HashMap(7);
		fgHTMLTagLookup.put("p", LINE_DELIM);
		fgHTMLTagLookup.put("br", LINE_DELIM);
		fgHTMLTagLookup.put("dt", LINE_DELIM);	// definition title in definition list
		fgHTMLTagLookup.put("dd", " ");		// definition doc in definition list
		fgHTMLTagLookup.put("li", LINE_DELIM + "· ");
		
		fgEntityLookup= new HashMap(7);
		fgEntityLookup.put("lt", "<");
		fgEntityLookup.put("gt", ">");
		fgEntityLookup.put("nbsp", " ");
		fgEntityLookup.put("amp", "&");
		fgEntityLookup.put("circ", "^");
		fgEntityLookup.put("tilde", "~");
		fgEntityLookup.put("quot", "\"");		
	}
	
	private Reader fReader;
	
	private boolean fWasWhiteSpace;
	
	private StringBuffer fBuffer;
	private int fIndex;
	private boolean fReadFromBuffer;
	
	private int fCharAfterWhiteSpace;

	public JavaDocTextReader(Reader reader) {
		fReader= reader;
		fBuffer= new StringBuffer();
		fIndex= 0;
		fReadFromBuffer= false;
		fCharAfterWhiteSpace= -1;
		fWasWhiteSpace= true;
	}
	
	private int nextChar() throws IOException {
		fReadFromBuffer= (fBuffer.length() > 0);
		if (fReadFromBuffer) {
			char ch= fBuffer.charAt(fIndex++);
			if (fIndex >= fBuffer.length()) {
				fBuffer.setLength(0);
				fIndex= 0;
			}
			return ch;
		} else {
			int ch= fCharAfterWhiteSpace;
			if (ch == -1) {
				ch= fReader.read();
			}
			if (Character.isWhitespace((char)ch)) {
				do {
					ch= fReader.read();
				} while (Character.isWhitespace((char)ch));
				if (ch != -1) {
					fCharAfterWhiteSpace= ch;
					return ' ';
				}
			} else {
				fCharAfterWhiteSpace= -1;
			}
			return ch;
		}
	}
		
	private void addToBuffer(String str) {
		fBuffer.insert(0, str);
	}
	
	/**
	 * @see Reader#read(char)
	 */
	public int read() throws IOException {
		int ch;
		do {
			ch= nextChar();
			while (!fReadFromBuffer) {
				if (ch == '<') {
					processHTML();
				} else if (ch == '@') {
					processJDocTag();
				} else if (ch == '&') {
					processEscapeCharacter();
				} else {
					break;
				}
				ch= nextChar();
			}
		} while (fWasWhiteSpace && (ch == ' '));
				
		fWasWhiteSpace= (ch == ' ' || ch == '\r' || ch == '\n');
		return ch;
	}
	
	private void processHTML() throws IOException {
		StringBuffer buf= new StringBuffer();
		int ch;
		do {		
			ch= nextChar();
			while (ch != -1 && ch != '>') {
				//if (ch == '"' || ch == '\'') {
				//	readString(buf, ch);
				//} else {
					buf.append(Character.toLowerCase((char)ch));
					ch= nextChar();
				//}
			}
			if (ch == -1) {
				return;
			}
			
			int tagLen= buf.length();
			// needs special treatment for comments 
			if ((tagLen >= 3 && "!--".equals(buf.substring(0, 3)))
				&& !(tagLen >= 5 && "--".equals(buf.substring(tagLen - 2)))) {
				// unfinished comment
				buf.append(ch);
			} else {
				break;
			}
		} while (true);
		 
		String text= htmlToText(buf.toString());
		if (text != null) {
			addToBuffer(text);		
		}
	}
	
	protected String htmlToText(String htmlTag) {
		return (String) fgHTMLTagLookup.get(htmlTag);
	}	
			
	private void processJDocTag() {
		addToBuffer(JDOC_TAG_BEGIN);
	}
	
	private void processEscapeCharacter() throws IOException {
		StringBuffer buf= new StringBuffer();
		int ch= nextChar();
		while (Character.isLetterOrDigit((char)ch) || ch == '#') {
			buf.append((char)ch);
			ch= nextChar();
		}
		if (ch == ';') {
			String str= entityToText(buf.toString());
			addToBuffer(str);
		} else {
			buf.insert(0, '&');
			if (ch != -1) {
				buf.append((char)ch);
			}
			addToBuffer(buf.toString());
		}
	}
	
	protected String entityToText(String symbol) {
		if (symbol.length() > 1 && symbol.charAt(0) == '#') {
			int ch;
			try {
				if (symbol.charAt(1) == 'x') {
					ch= Integer.parseInt(symbol.substring(2), 16);
				} else {
					ch= Integer.parseInt(symbol.substring(1), 10);
				}
				return "" + (char)ch;
			} catch (NumberFormatException e) {
			}
		} else {
			String str= (String) fgEntityLookup.get(symbol);
			if (str != null) {
				return str;
			}
		}
		return "&" + symbol; // not found
	}
	
	/**
	 * @see Reader#ready()
	 */		
    	public boolean ready() throws IOException {
		return fReader.ready();
	}
		
	/**
	 * @see Reader#close()
	 */		
	public void close() throws IOException {
		fReader.close();
	}
	
	/**
	 * @see Reader#reset()
	 */		
	public void reset() throws IOException {
		fReader.reset();
		fWasWhiteSpace= true;
		fCharAfterWhiteSpace= -1;
		fBuffer.setLength(0);
		fIndex= 0;		
	}
}