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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Code formatter for general source code comments.
 * 
 * @since 3.0
 */
public class CommentFormatter implements ICodeFormatter {

	/** Source viewer where to apply the formatting strategy */
	private final ISourceViewer fSourceViewer;

	/**
	 * Creates a new comment formatter.
	 * 
	 * @param viewer The source viewer where to apply the formatting strategy
	 */
	public CommentFormatter(ISourceViewer viewer) {
		fSourceViewer= viewer;
	}

	/*
	 * @see org.eclipse.jdt.core.ICodeFormatter#format(java.lang.String, int, int[], java.lang.String)
	 */
	public String format(String string, int indentationLevel, int[] positions, String lineSeparator) {

		try {
			final CommentRegion region= CommentObjectFactory.getRegion(fSourceViewer.getDocument(), fSourceViewer.getDocument().getPartition(0), lineSeparator);
			region.formatRegion();
		} catch (BadLocationException e) {
		}

		// TODO Do formatting
		return null;
	}
}
