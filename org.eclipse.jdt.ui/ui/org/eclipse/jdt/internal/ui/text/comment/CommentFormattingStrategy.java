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

package org.eclipse.jdt.internal.ui.text.comment;

import java.util.LinkedList;
import java.util.Map;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.text.comment.CommentFormatter;
import org.eclipse.jdt.internal.corext.text.comment.CommentObjectFactory;
import org.eclipse.jdt.internal.corext.text.comment.ITextMeasurement;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

/**
 * Formatting strategy for general source code comments.
 * 
 * @since 3.0
 */
public class CommentFormattingStrategy extends ContextBasedFormattingStrategy {

	/** Documents to be formatted by this strategy */
	private final LinkedList fDocuments= new LinkedList();

	/** Partitions to be formatted by this strategy */
	private final LinkedList fPartitions= new LinkedList();

	/** Text measurement, can be <code>null</code> */
	private ITextMeasurement fTextMeasurement;

	/**
	 * Creates a new comment formatting strategy.
	 * 
	 * @param textMeasurement
	 *                  The text measurement
	 */
	public CommentFormattingStrategy(ITextMeasurement textMeasurement) {
		super();
		fTextMeasurement= textMeasurement;
	}

	/**
	 * @inheritDoc
	 */
	public void format() {
		super.format();
		
		final IDocument document= (IDocument) fDocuments.removeFirst();
		final TypedPosition position= (TypedPosition)fPartitions.removeFirst();
		
		if (document != null && position != null) {
			TextEdit edit= null;
			try {
				int sourceOffset= document.getLineOffset(document.getLineOfOffset(position.getOffset()));
				int partitionOffset= position.getOffset() - sourceOffset;
				int sourceLength= partitionOffset + position.getLength();
				String source= document.get(sourceOffset, sourceLength);
				
				Map preferences= CommentFormattingContext.mapOptions(getPreferences());
				CodeFormatter commentFormatter= new CommentFormatter(fTextMeasurement, preferences);
				int indentationLevel= inferIndentationLevel(source.substring(0, partitionOffset), getTabSize(preferences));
				edit= commentFormatter.format(getPartitionKind(position.getType()), source, partitionOffset, position.getLength(), indentationLevel, TextUtilities.getDefaultLineDelimiter(document));
				edit.moveTree(sourceOffset);
			} catch (BadLocationException exception) {
				JavaPlugin.log(exception);
			}
			
			try {
				if (edit != null)
					edit.apply(document);
			} catch (MalformedTreeException exception) {
				JavaPlugin.log(exception);
			} catch (BadLocationException exception) {
				JavaPlugin.log(exception);
			}
		}
	}

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingStrategyExtension#formatterStarts(org.eclipse.jface.text.formatter.IFormattingContext)
	 */
	public void formatterStarts(IFormattingContext context) {
		super.formatterStarts(context);

		fPartitions.addLast(context.getProperty(FormattingContextProperties.CONTEXT_PARTITION));
		fDocuments.addLast(context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM));
	}

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingStrategyExtension#formatterStops()
	 */
	public void formatterStops() {
		fPartitions.clear();
		fDocuments.clear();
		
		super.formatterStops();
	}

	/**
	 * Map from {@link IJavaPartitions}comment partition types to
	 * {@link CodeFormatter}code snippet kinds.
	 * 
	 * @param type the partition type
	 * @return the code snippet kind
	 * @since 3.1
	 */
	private static int getPartitionKind(String type) {
		if (IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(type))
				return CommentObjectFactory.K_SINGLE_LINE_COMMENT;
		if (IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(type))
				return CommentObjectFactory.K_MULTI_LINE_COMMENT;
		if (IJavaPartitions.JAVA_DOC.equals(type))
				return CommentObjectFactory.K_JAVA_DOC;
		return -1;
	}
	
	/**
	 * Infer the indentation level based on the given reference indentation
	 * and tab size.
	 * 
	 * @param reference the reference indentation
	 * @param tabSize the tab size
	 * @return the inferred indentation level
	 * @since 3.1
	 */
	private static int inferIndentationLevel(String reference, int tabSize) {
		int level= 0;
		for (int i= 0, n= reference.length(), spaces= 0; i < n; i++) {
			char ch= reference.charAt(i);
			if (ch == ' ') {
				spaces++;
				if (spaces >= tabSize) {
					spaces= 0;
					level++;
				}
			} else if (ch == '\t') {
				spaces= 0;
				level++;
			} else
				throw new IllegalArgumentException();
		}
		return level;
	}
	
	/**
	 * Returns the value of {@link DefaultCodeFormatterConstants#FORMATTER_TAB_SIZE}
	 * from the given preferences.
	 * 
	 * @param preferences the preferences
	 * @return the value of {@link DefaultCodeFormatterConstants#FORMATTER_TAB_SIZE}
	 *         from the given preferences
	 * @since 3.1
	 */
	private static int getTabSize(Map preferences) {
		if (preferences.containsKey(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE))
			try {
				return Integer.parseInt((String) preferences.get(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE));
			} catch (NumberFormatException e) {
				// use default
			}
		return 4;
	}
}
