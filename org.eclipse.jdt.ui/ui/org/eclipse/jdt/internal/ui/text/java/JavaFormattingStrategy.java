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
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
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
			
			// reshape the partition to get around some peculiarities of the formatter
			Position toFormat= new Position(partition.offset, partition.length);
			stripLeadingWS(document, toFormat);
			stripTrailingWS(document, toFormat, partition);
			
			IDocument formattedDoc= new Document(CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, document.get(), toFormat.offset, toFormat.length, indent, positions, TextUtilities.getDefaultLineDelimiter(document), preferences));
			
			int leadingEmptyLines= document.getNumberOfLines(partition.offset, toFormat.offset - partition.offset) - 1; // getNumberOfLines is one-based
			int trailingEmptyLines= document.getNumberOfLines(toFormat.offset + toFormat.length, partition.offset + partition.length - (toFormat.offset + toFormat.length)) - 1;
			int from= getLeadingWSOffset(leadingEmptyLines, formattedDoc);
			int to= getTrailingWSOffset(trailingEmptyLines, formattedDoc);
			String formatted= formattedDoc.get(from, to - from);

			final String raw= document.get(partition.getOffset(), partition.getLength());
			if (formatted != null && !formatted.equals(raw))
				document.replace(partition.getOffset(), partition.getLength(), formatted);

		} catch (BadLocationException exception) {
			// Can not happen
			Assert.isTrue(false);
		}
	}

	/**
	 * Strips leading white space off the region described by <code>toFormat</code>
	 * 
	 * @param document the doc
	 * @param toFormat the position
	 * @throws BadLocationException
	 */
	private void stripLeadingWS(final IDocument document, final Position toFormat) throws BadLocationException {
		// get rid of leading white space:
		// partition is an entire line selection (from start of a line)
		// the formatter, however, expects an offset that points to the first
		// non-ws of the line.
		int offset= toFormat.getOffset();
		int length= toFormat.getLength();
		for (int i= 0; i < length; i++) {
			if (!Character.isWhitespace(document.getChar(offset + i)))
				break;
			toFormat.offset++;
			toFormat.length--;
		}
	}

	/**
	 * Strips any trailing white space off the region described by <code>toFormat</code>.
	 * If the number of lines is reduced, <code>partition</code> is also trimmed off its last
	 * (emtpy) line.
	 * 
	 * @param document the document to format
	 * @param toFormat the position describing the range to be formatted
	 * @param partition the position describing the original range
	 * @throws BadLocationException
	 */
	private void stripTrailingWS(final IDocument document, Position toFormat, Position partition) throws BadLocationException {
		// strip an empty selected line end so we don't get additional lines
		int offset= toFormat.getOffset();
		int length= toFormat.getLength();
		int line= document.getLineOfOffset(offset + length);
		for (int i= length; i > 0; i--) {
			if (!Character.isWhitespace(document.getChar(offset + i - 1)))
				break;
			toFormat.length--;
		}
		if (document.getLineOfOffset(toFormat.length + toFormat.offset) != line) {
			IRegion region= document.getLineInformation(line - 1);
			int endOfLine= region.getOffset() + region.getLength();
			partition.setLength(endOfLine - partition.offset);
		}
	}

	/**
	 * Returns the offset of the line in <code>document</code> that is the
	 * <code>leadingEmptyLine</code>th empty line before any non-WS comes in
	 * <code>document</code>.
	 * 
	 * @param leadingEmptyLines the number of empty lines to leave
	 * @param document the document
	 * @return the index such that if the document is cut up to the offset, it will have <code>leadingEmptyLines</code> empty lines upfront
	 * @throws BadLocationException
	 */
	private int getLeadingWSOffset(int leadingEmptyLines, IDocument document) throws BadLocationException {
		// discard all leading ws except leadingEmtyLines lines
		int nLines= document.getNumberOfLines();
		int line= 0;
		for (; line < nLines; line++) {
			IRegion region= document.getLineInformation(line);
			if (document.get(region.getOffset(), region.getLength()).trim().length() != 0)
				break;
		}
		int wsLine= Math.max(line - leadingEmptyLines, 0);
		return document.getLineOffset(wsLine);
	}

	/**
	 * Returns the offset of the end of the line in <code>document</code> that is the
	 * <code>trailingEmptyLine</code>th empty line after the last non-WS character in
	 * <code>document</code>.
	 * 
	 * @param trailingEmptyLine the number of empty lines to leave
	 * @param document the document
	 * @return the index such that if the document is cut from the offset, it will have <code>trailingEmptyLine</code> empty lines at the end
	 * @throws BadLocationException
	 */
	private int getTrailingWSOffset(int trailingEmptyLines, IDocument document) throws BadLocationException {
		// discard all trailing ws
		int numberOfLines= document.getNumberOfLines();
		int line = numberOfLines - 1;
		for (; line >= 0; line--) {
			IRegion region= document.getLineInformation(line);
			if (document.get(region.getOffset(), region.getLength()).trim().length() != 0)
				break;
		}
		int wsLine= Math.min(line + trailingEmptyLines, numberOfLines);
		IRegion region= document.getLineInformation(wsLine);
		return region.getOffset() + region.getLength();
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
