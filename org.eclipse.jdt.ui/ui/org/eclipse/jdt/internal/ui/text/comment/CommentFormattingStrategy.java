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

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.jdt.internal.corext.text.comment.CommentFormatter;
import org.eclipse.jdt.internal.corext.text.comment.CommentRegion;
import org.eclipse.jdt.internal.corext.text.comment.ITextMeasurement;
import org.eclipse.jdt.internal.corext.text.comment.JavaPartitions;

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
			CommentFormattingContext.mapOptions(getPreferences());
			CommentFormatter commentFormatter= new CommentFormatter(fTextMeasurement, getPreferences());
			// TODO: pass minimal string, pass inferred indentation
			TextEdit edit= commentFormatter.format(getPartitionKind(position.getType()), document.get(), position.getOffset(), position.getLength(), CommentRegion.INFER_INDENTATION, TextUtilities.getDefaultLineDelimiter(document));
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

	private static int getPartitionKind(String type) {
		if (IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(type))
				return JavaPartitions.K_SINGLE_LINE_COMMENT;
		if (IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(type))
				return JavaPartitions.K_MULTI_LINE_COMMENT;
		if (IJavaPartitions.JAVA_DOC.equals(type))
				return JavaPartitions.K_JAVA_DOC;
		return -1;
	}
}
