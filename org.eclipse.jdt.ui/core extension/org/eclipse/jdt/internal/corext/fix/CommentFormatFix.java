/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingStrategy;

public class CommentFormatFix extends TextEditFix {
	
	public static IFix createCleanUp(ICompilationUnit unit, IRegion[] regions, boolean singleLine, boolean multiLine, boolean javaDoc, HashMap preferences) throws CoreException {
		if (!singleLine && !multiLine && !javaDoc)
			return null;
		
		String content= unit.getBuffer().getContents();
		Document document= new Document(content);
		
		if (regions == null)
			regions= new IRegion[] {new Region(0, document.getLength())};
		
		final List edits= format(document, singleLine, multiLine, javaDoc, preferences, regions);
		if (edits.size() == 0)
			return null;
		
		MultiTextEdit resultEdit= new MultiTextEdit();
		resultEdit.addChildren((TextEdit[])edits.toArray(new TextEdit[edits.size()]));
		return new CommentFormatFix(resultEdit, unit, MultiFixMessages.CommentFormatFix_description);
	}
	
	static String format(String input, boolean singleLine, boolean multiLine, boolean javaDoc) {
		if (!singleLine && !multiLine && !javaDoc)
			return input;
		
		HashMap preferences= new HashMap(JavaCore.getOptions());
		Document document= new Document(input);
		List edits= format(document, singleLine, multiLine, javaDoc, preferences, new IRegion[] {new Region(0, document.getLength())});
		
		if (edits.size() == 0)
			return input;
		
		MultiTextEdit resultEdit= new MultiTextEdit();
		resultEdit.addChildren((TextEdit[])edits.toArray(new TextEdit[edits.size()]));
		
		try {
			resultEdit.apply(document);
		} catch (MalformedTreeException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		return document.get();
	}
	
	private static List format(IDocument document, boolean singleLine, boolean multiLine, boolean javaDoc, HashMap preferences, IRegion[] changedRegions) {
		final List edits= new ArrayList();
		
		JavaPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);
		
		String content= document.get();
		
		CommentFormattingStrategy formattingStrategy= new CommentFormattingStrategy();
		
		IFormattingContext context= new CommentFormattingContext();
		context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, preferences);
		context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.TRUE);
		context.setProperty(FormattingContextProperties.CONTEXT_MEDIUM, document);
		
		try {
			ITypedRegion[] regions= TextUtilities.computePartitioning(document, IJavaPartitions.JAVA_PARTITIONING, 0, document.getLength(), false);
			for (int i= 0; i < regions.length; i++) {
				ITypedRegion region= regions[i];
				
				if (isChanged(region, changedRegions)) {
					if (singleLine && region.getType().equals(IJavaPartitions.JAVA_SINGLE_LINE_COMMENT)) {
						TextEdit edit= format(region, context, formattingStrategy, content);
						if (edit != null)
							edits.add(edit);
					} else if (multiLine && region.getType().equals(IJavaPartitions.JAVA_MULTI_LINE_COMMENT)) {
						TextEdit edit= format(region, context, formattingStrategy, content);
						if (edit != null)
							edits.add(edit);
					} else if (javaDoc && region.getType().equals(IJavaPartitions.JAVA_DOC)) {
						TextEdit edit= format(region, context, formattingStrategy, content);
						if (edit != null)
							edits.add(edit);
					}
				}
			}
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		} finally {
			context.dispose();
		}
		
		return edits;
	}
	
	private static boolean isChanged(IRegion region, IRegion[] changedRegions) {
		int regionOffset= region.getOffset();
		int regionEnd= getRegionEnd(region);

		for (int i= 0; i < changedRegions.length; i++) {
			IRegion changed= changedRegions[i];

			int changeOffset= changed.getOffset();
			int changeEnd= getRegionEnd(changed);

			if (changeOffset <= regionOffset && changeEnd >= regionEnd)//covers
				return true;

			if (changeOffset > regionOffset && changeEnd < regionEnd)//inside
				return true;

			if (changeOffset < regionOffset && changeEnd > regionOffset)//touches start
				return true;

			if (changeOffset < regionEnd && changeEnd > regionEnd)//touches end
				return true;
		}
		return false;
	}

	private static int getRegionEnd(IRegion region) {
		return region.getOffset() + region.getLength() - 1;
	}
	
	private static TextEdit format(ITypedRegion region, IFormattingContext context, CommentFormattingStrategy formattingStrategy, String content) {
		TypedPosition typedPosition= new TypedPosition(region);
		context.setProperty(FormattingContextProperties.CONTEXT_PARTITION, typedPosition);
		formattingStrategy.formatterStarts(context);
		TextEdit edit= formattingStrategy.calculateTextEdit();
		formattingStrategy.formatterStops();
		if (edit == null)
			return null;
		
		if (!edit.hasChildren())
			return null;
		
		// Filter out noops
		TextEdit[] children= edit.getChildren();
		for (int i= 0; i < children.length; i++) {
			if (!(children[i] instanceof ReplaceEdit))
				return edit;
		}
		
		IDocument doc= new Document(content);
		try {
			edit.copy().apply(doc, TextEdit.NONE);
			if (content.equals(doc.get()))
				return null;
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		}
		
		return edit;
	}

	public CommentFormatFix(TextEdit edit, ICompilationUnit unit, String changeDescription) {
		super(edit, unit, changeDescription);
	}

}
