/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.javadoc;

import java.util.ArrayList;

/**
 *
 */
public class JavaDocTag {
	
	public static final String PARAM= "param"; //$NON-NLS-1$
	public static final String RETURN= "return"; //$NON-NLS-1$
	public static final String THROWS= "throws"; //$NON-NLS-1$
	public static final String EXCEPTION= "exception"; //$NON-NLS-1$
	
	private int fOffset;
	private int fLength;
	
	private int fContentOffset;
	private int fContentLength;
		
	private String fName;
	private String fContent;

	
	public JavaDocTag(String name, String content, int offset, int length, int contentOffset, int contentLength) {
		fOffset= offset;
		fLength= length;

		fContentOffset= contentOffset;
		fContentLength= contentLength;
		
		fName=name;
		fContent= content;
	}
	
	public JavaDocTag(String tag, String content) {
		this(tag, content, 0, 0, 0, 0);
	}
		
	
	/**
	 * @return Returns the tag name
	 */
	public String getName() {
		return fName;
	}
	/**
	 * @return Returns the content.
	 */
	public String getContent() {
		return fContent;
	}
	
	/**
	 * @return Returns the offset, starting from the tag's name including '@'.
	 */
	public int getOffset() {
		return fOffset;
	}
	
	/**
	 * @return Returns the length.
	 */
	public int getLength() {
		return fLength;
	}
	
	/**
	 * @return Returns the content offset.
	 */
	public int getContentOffset() {
		return fContentOffset;
	}
	
	/**
	 * @return Returns the content length.
	 */
	public int getContentLength() {
		return fContentLength;
	}
	
	public static JavaDocTag[] createFromComment(JavaDocCommentReader reader) {
		ArrayList res= new ArrayList();
		int ch= reader.read();
		while (ch != -1 && Character.isWhitespace((char) ch)) {
			ch= reader.read();
		}
		if (ch == -1) {
			return new JavaDocTag[0];
		}
		
		do {
			ch= readTag(reader, ch, res);
		} while (ch != -1);
		return (JavaDocTag[]) res.toArray(new JavaDocTag[res.size()]);
	}
	
	private static int readTag(JavaDocCommentReader reader, int ch, ArrayList res) {
		boolean isFirstSentence= ch != '@';
		
		int tagStart= reader.getOffset() - 1;
		
		String name= null;
		int prev= -1;
		
		// read name
		if (!isFirstSentence) {
			StringBuffer nameBuffer= new StringBuffer();
			prev= ch;
			ch= reader.read();
			while (ch != -1 && (ch != '@' || prev == '{') && Character.isLetterOrDigit((char) ch)) {
				nameBuffer.append((char) ch);
				prev= ch;
				ch= reader.read();
			}
			name= nameBuffer.toString();
		}
		
		// whitespace between name and content
		while (ch != -1 && Character.isWhitespace((char) ch)) {
			prev= ch;
			ch= reader.read();
		}
		
		// read content
		int descStart= reader.getOffset() - 1;
		int tagEnd= descStart;
		int tagBufEnd= 0;
		int descEnd= descStart;
		StringBuffer descBuffer= new StringBuffer();
		while (ch != -1 && (ch != '@' || prev == '{')) {
			descBuffer.append((char) ch);
			if (!Character.isWhitespace((char) ch)) {
				descEnd= reader.getOffset();
				tagBufEnd= descBuffer.length();
			}

			prev= ch;
			ch= reader.read();
		}
		tagEnd= reader.getOffset();
		if (ch != -1) {
			tagEnd--;
		}

		descBuffer.setLength(tagBufEnd); // remove white spaces
		res.add(new JavaDocTag(name, descBuffer.toString(), tagStart, tagEnd - tagStart, descStart, descEnd - descStart));

		return ch;
	}


}
