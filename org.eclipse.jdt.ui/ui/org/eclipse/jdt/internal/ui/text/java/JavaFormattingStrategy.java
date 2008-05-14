/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.util.LinkedList;
import java.util.Map;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Formatting strategy for java source code.
 *
 * @since 3.0
 */
public class JavaFormattingStrategy extends ContextBasedFormattingStrategy {

	/** Documents to be formatted by this strategy */
	private final LinkedList fDocuments= new LinkedList();
	/** Partitions to be formatted by this strategy */
	private final LinkedList fPartitions= new LinkedList();

	/**
	 * Creates a new java formatting strategy.
 	 */
	public JavaFormattingStrategy() {
		super();
	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#format()
	 */
	public void format() {
		super.format();

		final IDocument document= (IDocument)fDocuments.removeFirst();
		final TypedPosition partition= (TypedPosition)fPartitions.removeFirst();

		if (document != null && partition != null) {
			Map partitioners= null;
			int kind= CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS;
			try {
				// Start workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=95340
				ITypedRegion commentRegion= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, partition.offset, false);
				if (IJavaPartitions.JAVA_DOC.equals(commentRegion .getType()) && commentRegion .getOffset() + commentRegion .getLength() >= partition.offset + partition.length) {
					kind= CodeFormatter.K_JAVA_DOC;
				} else if (IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(commentRegion .getType()) && commentRegion .getOffset() + commentRegion .getLength() >= partition.offset + partition.length) {
					kind= CodeFormatter.K_MULTI_LINE_COMMENT;
				} else if (IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(commentRegion .getType()) && commentRegion .getOffset() + commentRegion .getLength() >= partition.offset + partition.length) {
					kind= CodeFormatter.K_SINGLE_LINE_COMMENT;
				}
				String source;
				int indentationLevel;
				int delta= 0;
				switch (kind) {
					case CodeFormatter.K_JAVA_DOC:
					case CodeFormatter.K_MULTI_LINE_COMMENT:
					case CodeFormatter.K_SINGLE_LINE_COMMENT:
						Map preferences= getPreferences();
						int lineOffset= document.getLineOffset(document.getLineOfOffset(commentRegion.getOffset()));
						String indentString= document.get(lineOffset, commentRegion.getOffset() - lineOffset);
						indentationLevel= inferIndentationLevel(indentString, getTabSize(preferences), getIndentSize(preferences));
						delta= commentRegion .getOffset();
						source= document.get(commentRegion.getOffset(), commentRegion.getLength());
						partition.setOffset(0);
						partition.setLength(commentRegion .getLength());
						break;
					default:
						source= document.get();
						indentationLevel= 0;
						break;
				}
				// End workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=95340

				final TextEdit edit= CodeFormatterUtil.reformat(kind, source, partition.getOffset(), partition.getLength(), indentationLevel, TextUtilities.getDefaultLineDelimiter(document), getPreferences());
				if (edit != null) {
					if (edit.getChildrenSize() > 20)
						partitioners= TextUtilities.removeDocumentPartitioners(document);
					if (delta > 0) {
						edit.moveTree(delta);
					}
					edit.apply(document);
				}

			} catch (MalformedTreeException exception) {
				JavaPlugin.log(exception);
			} catch (BadLocationException exception) {
				// Can only happen on concurrent document modification - log and bail out
				JavaPlugin.log(exception);
			} finally {
				if (partitioners != null)
					TextUtilities.addDocumentPartitioners(document, partitioners);
			}
		}
 	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStarts(org.eclipse.jface.text.formatter.IFormattingContext)
	 */
	public void formatterStarts(final IFormattingContext context) {
		super.formatterStarts(context);

		fPartitions.addLast(context.getProperty(FormattingContextProperties.CONTEXT_PARTITION));
		fDocuments.addLast(context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM));
	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStops()
	 */
	public void formatterStops() {
		super.formatterStops();

		fPartitions.clear();
		fDocuments.clear();
	}

	/**
	 * Infer the indentation level based on the given reference indentation
	 * and tab size.
	 *
	 * @param reference the reference indentation
	 * @param tabSize the tab size
	 * @param indentSize the indent size in space equivalents
	 * @return the inferred indentation level
	 * @since 3.4
	 */
	private int inferIndentationLevel(String reference, int tabSize, int indentSize) {
		StringBuffer expanded= expandTabs(reference, tabSize);

		int referenceWidth= expanded.length();
		if (tabSize == 0)
			return referenceWidth;

		int level= referenceWidth / indentSize;
		if (referenceWidth % indentSize > 0)
			level++;
		return level;
	}

	/**
	 * Expands the given string's tabs according to the given tab size.
	 *
	 * @param string the string
	 * @param tabSize the tab size
	 * @return the expanded string
	 * @since 3.4
	 */
	private static StringBuffer expandTabs(String string, int tabSize) {
		StringBuffer expanded= new StringBuffer();
		for (int i= 0, n= string.length(), chars= 0; i < n; i++) {
			char ch= string.charAt(i);
			if (ch == '\t') {
				for (; chars < tabSize; chars++)
					expanded.append(' ');
				chars= 0;
			} else {
				expanded.append(ch);
				chars++;
				if (chars >= tabSize)
					chars= 0;
			}

		}
		return expanded;
	}

	/**
	 * Returns the visual tab size.
	 *
	 * @param preferences the preferences
	 * @return the visual tab size
	 * @since 3.4
	 */
	private static int getTabSize(Map preferences) {
		/*
		 * If the tab-char is SPACE, FORMATTER_INDENTATION_SIZE is not used
		 * by the core formatter.
		 * We piggy back the visual tab length setting in that preference in
		 * that case. See CodeFormatterUtil.
		 */
		String key;
		if (JavaCore.SPACE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
			key= DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
		else
			key= DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;

		if (preferences.containsKey(key))
			try {
				return Integer.parseInt((String) preferences.get(key));
			} catch (NumberFormatException e) {
				// use default
			}
		return 4;
	}
	
	/**
	 * Returns the indentation size in space equivalents.
	 *
	 * @param preferences the preferences
	 * @return the indentation size in space equivalents
	 * @since 3.3
	 */
	private static int getIndentSize(Map preferences) {
		/*
		 * FORMATTER_INDENTATION_SIZE is only used if FORMATTER_TAB_CHAR is MIXED. Otherwise, the
		 * indentation size is in FORMATTER_TAB_CHAR. See CodeFormatterUtil.
		 */
		String key;
		if (DefaultCodeFormatterConstants.MIXED.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR)))
			key= DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
		else
			key= DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
		
		if (preferences.containsKey(key))
			try {
				return Integer.parseInt((String) preferences.get(key));
			} catch (NumberFormatException e) {
				// use default
			}
			return 4;
	}
}
