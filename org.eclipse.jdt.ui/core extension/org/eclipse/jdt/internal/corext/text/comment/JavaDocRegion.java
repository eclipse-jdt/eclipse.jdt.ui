/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.text.comment;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ConfigurableLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;

import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.javadoc.IJavaDocTagConstants;

/**
 * Javadoc region in a source code document.
 * 
 * @since 3.0
 */
public class JavaDocRegion extends MultiCommentRegion implements IJavaDocTagConstants {

	/** The positions of code ranges */
	private final ArrayList fCodePositions= new ArrayList();
	 
	/** Should HTML tags be formatted? */
	private final boolean fFormatHtml;

	/** Should source code regions be formatted? */
	private final boolean fFormatSource;
	
 	/**
 	 * Creates a new Javadoc region.
 	 * 
	 * @param document
	 *                   The document which contains the comment region
 	 * @param position
	 *                   The position of this comment region in the document
 	 * @param delimiter
	 *                   The line delimiter of this comment region
	 * @param preferences
	 *                   The formatting preferences for this region
	 * @param textMeasurement
	 *                   The text measurement. Can be <code>null</code>.
 	 */	
	protected JavaDocRegion(final IDocument document, final TypedPosition position, final String delimiter, final Map preferences, final ITextMeasurement textMeasurement) {
		super(document, position, delimiter, preferences, textMeasurement);

		fFormatSource= IPreferenceStore.TRUE.equals(preferences.get(PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE));
		fFormatHtml= IPreferenceStore.TRUE.equals(preferences.get(PreferenceConstants.FORMATTER_COMMENT_FORMATHTML));
	}

	/**
	 * @inheritDoc
	 */
	protected boolean canFormat(final CommentRange previous, final CommentRange next) {
		
		if (previous != null) {
			
			final boolean isCurrentCode= next.hasAttribute(COMMENT_CODE);
			final boolean isLastCode= previous.hasAttribute(COMMENT_CODE);
			
			final int base= getOffset();
			
			if (!isLastCode && isCurrentCode)
				fCodePositions.add(new Position(base + previous.getOffset()));
			else if (isLastCode && !isCurrentCode)
				fCodePositions.add(new Position(base + next.getOffset() + next.getLength()));
			
			if (previous.hasAttribute(COMMENT_IMMUTABLE) && next.hasAttribute(COMMENT_IMMUTABLE))
				return false;
			
			
		}
		return true;
	}

	/**
	 * @inheritDoc
 	 */
	protected final void formatRegion(final String indentation, final int width) {
	
		super.formatRegion(indentation, width);
		
		if (fFormatSource) {
			
			try {
				
				if (fCodePositions.size() > 0) {
					
					int begin= 0;
					int end= 0;
					
					Position position= null;
					
					final IDocument document= getDocument();
					
					for (int index= fCodePositions.size() - 1; index >= 0;) {
						
						position= (Position)fCodePositions.get(index--);
						begin= position.getOffset();
						
						if (index >= 0) {
							position= (Position)fCodePositions.get(index--);
							end= position.getOffset();
						} else {
							/* 
							 * Handle missing closing tag
							 * see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=57011
							 */
							position= null;
							end= getOffset() + getLength() - MultiCommentLine.MULTI_COMMENT_END_PREFIX.trim().length();
							while (end > begin && Character.isWhitespace(document.getChar(end - 1)))
								end--;
						}
						
						String snippet= document.get(begin, end - begin);
						snippet= preprocessCodeSnippet(snippet);
						snippet= formatCodeSnippet(snippet);
						snippet= postprocessCodeSnippet(snippet, indentation);
						
						logEdit(snippet, begin - getOffset(), end - begin);
					}
				}
			} catch (BadLocationException e) {
				// Can not happen
				JavaPlugin.log(e);
			}
		}
	}

	/**
	 * Preprocess a given code snippet.
	 * @param snippet The code snippet
	 * @return The preprocessed code snippet
	 */
	private String preprocessCodeSnippet(String snippet) {
		// strip content prefix
		StringBuffer buffer= new StringBuffer();
		ILineTracker tracker= new ConfigurableLineTracker(new String[] { getDelimiter()});
		String contentPrefix= MultiCommentLine.MULTI_COMMENT_CONTENT_PREFIX.trim();
		
		buffer.setLength(0);
		buffer.append(snippet);
		tracker.set(snippet);
		for (int line= tracker.getNumberOfLines() - 1; line > 0; line--) {
			int lineOffset;
			try {
				lineOffset= tracker.getLineOffset(line);
			} catch (BadLocationException e) {
				// Can not happen
				JavaPlugin.log(e);
				return snippet;
			}
			int prefixOffset= buffer.indexOf(contentPrefix, lineOffset);
			if (prefixOffset >= 0 && buffer.substring(lineOffset, prefixOffset).trim().length() == 0)
				buffer.delete(lineOffset, prefixOffset + contentPrefix.length());
		}
		
		return convertHtml2Java(buffer.toString());
	}

	/**
	 * Format the given code snippet
	 * @param snippet The code snippet
	 * @return The formatted code snippet
	 */
	private String formatCodeSnippet(String snippet) {
		String lineDelimiter= TextUtilities.getDefaultLineDelimiter(getDocument());
		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_UNKNOWN, snippet, 0, lineDelimiter, getPreferences());
		if (edit != null)
			snippet= CodeFormatterUtil.evaluateFormatterEdit(snippet, edit, null);
		return snippet;
	}

	/**
	 * Postprocesses the given code snippet with the given indentation.
	 * @param snippet The code snippet
	 * @param indentation The indentation
	 * @return The postprocessed code snippet
	 */
	private String postprocessCodeSnippet(String snippet, String indentation) {
		// patch content prefix
		StringBuffer buffer= new StringBuffer();
		ILineTracker tracker= new ConfigurableLineTracker(new String[] { getDelimiter()});
		String patch= indentation + MultiCommentLine.MULTI_COMMENT_CONTENT_PREFIX;

		buffer.setLength(0);
		buffer.append(getDelimiter());
		buffer.append(convertJava2Html(snippet));
		buffer.append(getDelimiter());
		tracker.set(buffer.toString());
		
		for (int line= tracker.getNumberOfLines() - 1; line > 0; line--)
			try {
				buffer.insert(tracker.getLineOffset(line), patch);
			} catch (BadLocationException e) {
				// Can not happen
				JavaPlugin.log(e);
				return snippet;
			}
		
		return buffer.toString();
	}

	/**
	 * @inheritDoc
	 */
	protected final void markHtmlRanges() {

		markTagRanges(JAVADOC_IMMUTABLE_TAGS, COMMENT_IMMUTABLE, true);

		if (fFormatSource)
			markTagRanges(JAVADOC_CODE_TAGS, COMMENT_CODE, false);
	}

	/**
	 * @inheritDoc
	 */
	protected final void markHtmlTag(final CommentRange range, final String token) {

		if (range.hasAttribute(COMMENT_HTML)) {

			range.markHtmlTag(JAVADOC_IMMUTABLE_TAGS, token, COMMENT_IMMUTABLE, true, true);
			if (fFormatHtml) {

				range.markHtmlTag(JAVADOC_SEPARATOR_TAGS, token, COMMENT_SEPARATOR, true, true);
				range.markHtmlTag(JAVADOC_BREAK_TAGS, token, COMMENT_BREAK, false, true);
				range.markHtmlTag(JAVADOC_SINGLE_BREAK_TAG, token, COMMENT_BREAK, true, false);
				range.markHtmlTag(JAVADOC_NEWLINE_TAGS, token, COMMENT_NEWLINE, true, false);

			} else
				range.markHtmlTag(JAVADOC_CODE_TAGS, token, COMMENT_SEPARATOR, true, true);
		}
	}

	/**
	 * @inheritDoc
	 */
	protected final void markJavadocTag(final CommentRange range, final String token) {

		range.markPrefixTag(JAVADOC_PARAM_TAGS, COMMENT_TAG_PREFIX, token, COMMENT_PARAMETER);

		if (token.charAt(0) == JAVADOC_TAG_PREFIX && !range.hasAttribute(COMMENT_PARAMETER))
			range.setAttribute(COMMENT_ROOT);
	}

	/**
	 * Marks the comment region with the HTML range tag.
	 * 
	 * @param tags		the HTML tag which confines the HTML range
	 * @param attribute	the attribute to set if the comment range is in the HTML range
	 * @param html		<code>true</code> iff the HTML tags in this HTML range
	 *                  	should be marked too, <code>false</code> otherwise
	 */
	protected final void markTagRanges(final String[] tags, final int attribute, final boolean html) {
		
		int level= 0;
		int count= 0;
		String token= null;
		CommentRange current= null;
		
		for (int index= 0; index < tags.length; index++) {
			
			level= 0;
			for (final Iterator iterator= getRanges().iterator(); iterator.hasNext();) {
				
				current= (CommentRange)iterator.next();
				count= current.getLength();
				
				if (count > 0 || level > 0) { // PR44035: when inside a tag, mark blank lines as well to get proper snippet formatting
					
					token= getText(current.getOffset(), current.getLength());
					level= current.markTagRange(token, tags[index], level, attribute, html);
				}
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	protected boolean canAppend(CommentLine line, CommentRange previous, CommentRange next, int index, int count) {
		// don't append code sections
		if (next.hasAttribute(COMMENT_CODE | COMMENT_FIRST_TOKEN) && line.getSize() != 0)
			return false;
		return super.canAppend(line, previous, next, index, count);
	}

	/**
	 * Converts <code>formatted</code> into valid html code suitable to be
	 * put inside &lt;pre&gt;&lt;/pre&gt; tags by replacing any html symbols by
	 * the relevant entities.
	 * 
	 * @param formatted the formatted java code
	 * @return html version of the formatted code
	 */
	private String convertJava2Html(String formatted) {
		Java2HTMLEntityReader reader= new Java2HTMLEntityReader(new StringReader(formatted));
		char[] buf= new char[256];
		StringBuffer buffer= new StringBuffer();
		int l;
		try {
			do {
				l= reader.read(buf);
				if (l != -1)
					buffer.append(buf, 0, l);
			} while (l > 0);
			return buffer.toString();
		} catch (IOException e) {
			return formatted;
		}
	}

	/**
	 * Converts <code>html</code> into java code suitable for formatting by
	 * replacing any html entities by their plain text representation.
	 * 
	 * @param html html code, may contain html entities
	 * @return plain textified version of <code>html</code>
	 */
	private String convertHtml2Java(String html) {
		HTMLEntity2JavaReader reader= new HTMLEntity2JavaReader(new StringReader(html));
		char[] buf= new char[html.length()]; // html2text never gets longer, only shorter!
		
		try {
			int read= reader.read(buf);
			return new String(buf, 0, read);
		} catch (IOException e) {
			return html;
		}
	}
}
