/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

import java.io.IOException;
import java.io.Reader;

public abstract class AbstractJspParser { 
	
	private Reader fReader;
	private boolean fHasUnread;
	private int fUnread;
	private int fPos;
	protected int fLines= 1;
	
	AbstractJspParser() {
		super();
	}
	
	private int getc() throws IOException {
		fPos++;
		if (fHasUnread) {
			fHasUnread= false;
			return fUnread;
		}

		int ch= fReader.read();

		if (ch == '\n') // LF
			fLines++;
		else if (ch == '\r')  { // CR
			int nextCh= getc();
			if (nextCh != '\n')
				fLines++;
			return nextCh;
		}
		return ch;
	}
	
	private void ungetc(int c) {
		fHasUnread= true;
		fUnread= c;
		fPos--;
	}
	
	private void parseDirective() throws IOException {
		StringBuffer sb= new StringBuffer();
		int pos= fPos;
		while (true) {
			int c = getc();
			if (c == '%') {
				c= getc();
				if (c == '>') {
					// get attributes
					parseAttributes(pos, sb.toString());
					return;
				}
			}
			sb.append((char)c);
		}
	}
	
	private void parseTag(boolean endTag) throws IOException {
		StringBuffer sb= new StringBuffer();
		int pos= fPos;
		while (true) {
			int c= getc();
			if (c == '/') {
				c= getc();
				if (c == '>') {
					// get attributes
					parseAttributes(pos, sb.toString());
					return;
				} else {
					ungetc(c);
				}
			} else if (c == '>') {
				// get attributes
				parseAttributes(pos, sb.toString());
				return;
			}
			sb.append((char)c);
		}
	}
	
	private void parseComment() throws IOException {
		while (true) {
			int c = getc();
			if (c == '-') {
				c= getc();
				if (c == '-') {
					c= getc();
					if (c == '%') {
						c= getc();
						if (c == '>') {
							return;
						}
					}
				}
			}
		}
	}

	private void parseJava(char type) throws IOException {
		StringBuffer sb= new StringBuffer();
		int line= fLines;
		while (true) {
			int c = getc();
			if (c == '%') {
				c= getc();
				if (c == '>') {
					java(type, sb.toString(), line);
					return;
				}
			}
			sb.append((char)c);
		}
	}
	
	protected void java(char tagType, String contents, int line) {
		// empty implementation
	}

	private void parseAttributes(int pos, String s) {
		
		boolean hasValue= false;
		StringBuffer name= new StringBuffer();
		StringBuffer value= new StringBuffer();
		String startTag= ""; //$NON-NLS-1$
		int i= 0;
		int ix= 0;
		int startName= 0;
		int startValue= 0;
		char c= s.charAt(i++);
		
		try {
			while (true) {
				
				// whitespace		
				while (Character.isWhitespace(c))
					c= s.charAt(i++);
					
				startName= i;
				while (Character.isLetterOrDigit(c) || c == ':') {
					name.append(c);
					c= s.charAt(i++);
				}
				
				// whitespace		
				while (Character.isWhitespace(c))
					c= s.charAt(i++);
					
				hasValue= false;
				if (c == '=') {
					c= s.charAt(i++);
					
					// value
					while (Character.isWhitespace(c))
						c= s.charAt(i++);
						
					startValue= i;

					// Special handling for this taglib tag
					if (startTag.equals("c:out"))  { //$NON-NLS-1$
						value= value.append(s.substring(startValue, Math.max(startValue, s.length() - 2)));
						name.setLength(0);
						tagAttribute(name.toString(), value.toString(), startName+pos, startValue+pos);
						break;
					} else if (c == '"') {
						c= s.charAt(i++);
						while (c != '"') {
							value.append(c);
							c= s.charAt(i++);
						}
						c= s.charAt(i++);

					} else {
						while (Character.isLetterOrDigit(c)) {
							value.append(c);
							c= s.charAt(i++);
						}
					}
					hasValue= true;
				}
					
				if (ix == 0)  {
					startTag= name.toString();
					startTag(false, startTag, startName+pos);
				} else 
					tagAttribute(name.toString(), hasValue ? value.toString() : null, startName+pos, startValue+pos);
				ix++;
				
				name.setLength(0);
				value.setLength(0);
			}
		} catch (StringIndexOutOfBoundsException e) {
			// we don't log this exception because it is used
			// as one way to exit the scanning loop
		}
		
		if (name.length() > 0) {
			if (ix == 0)
				startTag(false, name.toString(), startName+pos);
			else 
				tagAttribute(name.toString(), hasValue ? value.toString() : null, startName+pos, startValue+pos);
		}
		
		endTag(false);
	}
	
	protected void startTag(boolean endTag, String name, int startName) {
		// empty implementation
	}
	
	protected void tagAttribute(String attrName, String value, int startName, int startValue) {
		// empty implementation
	}
	
	protected void endTag(boolean end) {
		// empty implementation
	}
	
	protected void text(String t, int line) {
		// empty implementation
	}
	
	void parse(Reader reader) throws IOException {
		int c;
		StringBuffer buffer= new StringBuffer();
		fPos= 0;
		fLines= 1;
		int line= fLines;
		fReader= reader;	

		while (true) {
			c= getc();
			switch (c) {
			case -1:
				if (buffer.length() > 0)
					text(buffer.toString(), line);
				return;
			case '<':
				c= getc();
				if (c == '%') {
					// flush buffer
					if (buffer.length() > 0) {
						text(buffer.toString(), line);
						buffer.setLength(0);
						line= fLines;
					}
					c= getc();
					switch (c) {
					case '-':
						c= getc();
						if (c == '-') {
							parseComment();
						} else {
							ungetc(c);
							continue;
						}	
						break;
					case '@':
						parseDirective();
						break;
					case ' ':
					case '!':
					case '=':
						parseJava((char)c);
						break;
					default:
						parseComment();
						break;
					}
				} else if (c == '/') {
					parseTag(true);
				} else {
					ungetc(c);
					parseTag(false);
				}
				break;
			default:
				buffer.append((char)c);
				break;
			}
		}
	}
}
