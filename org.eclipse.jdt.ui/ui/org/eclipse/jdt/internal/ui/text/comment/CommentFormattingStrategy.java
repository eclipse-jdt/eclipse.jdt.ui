/*****************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package org.eclipse.jdt.internal.ui.text.comment;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Formatting strategy for general source code comments.
 * 
 * @since 3.0
 */
public class CommentFormattingStrategy implements IFormattingStrategy {

	/** Reference indentation to use for formatting */
	private String fIndentation= null;

	/** Source viewer where to apply the formatting strategy */
	private final ISourceViewer fSourceViewer;

	/**
	 * Creates a new comment formatting strategy.
	 * 
	 * @param viewer The source viewer where to apply the formatting strategy
	 */
	public CommentFormattingStrategy(ISourceViewer viewer) {
		fSourceViewer= viewer;
	}

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingStrategy#format(java.lang.String, boolean, java.lang.String, int[])
	 */
	public String format(String content, boolean isLineStart, String indent, int[] positions) {

		final ICodeFormatter formatter= new CommentFormatter(fSourceViewer);
		final IDocument document= fSourceViewer.getDocument();

		int indentation= 0;
		if (fIndentation != null) {
			indentation= Strings.computeIndent(fIndentation, CodeFormatterUtil.getTabWidth());
		}

		return formatter.format(content, indentation, positions, TextUtilities.getDefaultLineDelimiter(document));
	}

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingStrategy#formatterStarts(java.lang.String)
	 */
	public void formatterStarts(String indent) {
		fIndentation= indent;
	}

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingStrategy#formatterStops()
	 */
	public void formatterStops() {
		// Do nothing
	}
}
