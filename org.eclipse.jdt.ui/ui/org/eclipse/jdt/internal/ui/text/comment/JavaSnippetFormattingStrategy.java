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
package org.eclipse.jdt.internal.ui.text.comment;

import java.io.IOException;
import java.io.StringReader;
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
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Formatting strategy for java source code embedded inside javadoc comments.
 * <p>
 * This strategy implements <code>IFormattingStrategyExtension</code>. It
 * must be registered with a content formatter implementing <code>IContentFormatterExtension2<code>
 * to take effect.
 * 
 * @since 3.0
 */
public class JavaSnippetFormattingStrategy extends ContextBasedFormattingStrategy {

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
	public JavaSnippetFormattingStrategy(final ISourceViewer viewer) {
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
		final TypedPosition partition= (TypedPosition) fPartitions.removeFirst();
		final Map preferences= getPreferences();
		final IDocument document= getViewer().getDocument();
		int indent= 0;
		try {
			//TODO rewrite using the edit API (CodeFormatterUtil.format2)

			// convert html entities (mainly needed for &gt; and &lt;)
			String toFormat= document.get(partition.offset, partition.length);
			toFormat= convertHtml2Java(toFormat);
			String formatted= CodeFormatterUtil.format(CodeFormatter.K_UNKNOWN, toFormat, indent, null, TextUtilities
					.getDefaultLineDelimiter(document), preferences);
			formatted= convertJava2Html(formatted);
			final String raw= document.get(partition.getOffset(), partition.getLength());
			if (formatted != null && !formatted.equals(raw))
				document.replace(partition.getOffset(), partition.getLength(), formatted);
		} catch (BadLocationException exception) {
			// Can only happen on concurrent document modification - log and
			// bail out
			JavaPlugin.log(exception);
		}
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
		char[] buf= new char[html.length()]; // html2text never gets longer,
											 // only shorter!
		try {
			int read= reader.read(buf);
			return new String(buf, 0, read);
		} catch (IOException e) {
			return html;
		}
	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStarts(org.eclipse.jface.text.formatter.IFormattingContext)
	 */
	public void formatterStarts(IFormattingContext context) {
		super.formatterStarts(context);
		final FormattingContext current= (FormattingContext) context;
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
