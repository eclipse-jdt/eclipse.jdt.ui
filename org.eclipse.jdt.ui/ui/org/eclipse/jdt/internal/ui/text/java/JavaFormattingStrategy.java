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
package org.eclipse.jdt.internal.ui.text.java;

import java.util.LinkedList;
import java.util.Map;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContext;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

/**
 * Formatting strategy for java source code.
 * <p>
 * This strategy implements <code>IFormattingStrategyExtension</code>. It must be
 * registered with a content formatter implementing <code>IContentFormatterExtension2<code>
 * to take effect.
 * 
 * @since 3.0
 */
public class JavaFormattingStrategy extends ContextBasedFormattingStrategy {

	/** Indentations to use by this strategy */
	private final LinkedList fIndentations= new LinkedList();

	/** Partitions to be formatted by this strategy */
	private final LinkedList fPartitions= new LinkedList();

	/** The position sets to keep track of during formatting */
	private final LinkedList fPositions= new LinkedList();

	/**
	 * Creates a new java formatting strategy.
	 * 
	 * @param viewer ISourceViewer to operate on
	 */
	public JavaFormattingStrategy(final ISourceViewer viewer) {
		super(viewer);
	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#format()
	 */
	public void format() {
		super.format();

		Assert.isLegal(fIndentations.size() > 0);
		Assert.isLegal(fPartitions.size() > 0);
		Assert.isLegal(fPositions.size() > 0);

		final int[] positions= (int[])fPositions.removeFirst();
		
		final TypedPosition partition= (TypedPosition)fPartitions.removeFirst();

		final Map preferences= getPreferences();
		final IDocument document= getViewer().getDocument();

		int indent= 0;
		// not needed with the new API, but still in discussion..
		//final String indentation= (String)fIndentations.removeFirst();
		//if (indentation != null) {
		//	indent= Strings.computeIndent(indentation, CodeFormatterUtil.getTabWidth());
		//}

		try {
			//TODO rewrite using the edit API (CodeFormatterUtil.format2)
			final String formatted= CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, document.get(), partition.getOffset(), partition.getLength(), indent, positions, TextUtilities.getDefaultLineDelimiter(document), preferences);
			final String raw= document.get(partition.getOffset(), partition.getLength());
			if (formatted != null && !formatted.equals(raw))
				document.replace(partition.getOffset(), partition.getLength(), formatted);

		} catch (BadLocationException exception) {
			// Can not happen
		}
	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStarts(org.eclipse.jface.text.formatter.IFormattingContext)
	 */
	public void formatterStarts(IFormattingContext context) {
		super.formatterStarts(context);

		final FormattingContext current= (FormattingContext)context;

		fIndentations.addLast(current.getProperty(FormattingContextProperties.CONTEXT_INDENTATION));
		fPartitions.addLast(current.getProperty(FormattingContextProperties.CONTEXT_PARTITION));
		fPositions.addLast(current.getProperty(FormattingContextProperties.CONTEXT_POSITIONS));
	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStops()
	 */
	public void formatterStops() {
		super.formatterStops();

		fIndentations.clear();
		fPartitions.clear();
		fPositions.clear();
	}
}
