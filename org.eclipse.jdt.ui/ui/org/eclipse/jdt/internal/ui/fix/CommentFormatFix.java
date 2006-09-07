/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingStrategy;

public class CommentFormatFix implements IFix {

	public static IFix createCleanUp(CompilationUnit compilationUnit, boolean singleLine, boolean multiLine, boolean javaDoc) throws CoreException {
		if (!singleLine && !multiLine && !javaDoc)
			return null;
		
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		String content= cu.getBuffer().getContents();
		HashMap preferences= new HashMap(cu.getJavaProject().getOptions(true));
		Document document= new Document(content);
		
		final List edits= format(document, singleLine, multiLine, javaDoc, preferences);

		if (edits.size() == 0)
			return null;

		MultiTextEdit resultEdit= new MultiTextEdit();
		resultEdit.addChildren((TextEdit[])edits.toArray(new TextEdit[edits.size()]));

		TextChange change= new CompilationUnitChange("", cu); //$NON-NLS-1$
		change.setEdit(resultEdit);

		String label= MultiFixMessages.CommentFormatFix_description;
		change.addTextEditGroup(new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label))));

		return new CommentFormatFix(change, cu);
    }
	
	static String format(String input, boolean singleLine, boolean multiLine, boolean javaDoc) {
		if (!singleLine && !multiLine && !javaDoc)
			return input;
		
		HashMap preferences= new HashMap(JavaCore.getOptions());
		Document document= new Document(input);
		List edits= format(document, singleLine, multiLine, javaDoc, preferences);
		
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

	private static List format(IDocument document, boolean singleLine, boolean multiLine, boolean javaDoc, HashMap preferences) {
	    final List edits= new ArrayList();
	    
	    if (DefaultCodeFormatterConstants.FALSE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT)))
	    	preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT, DefaultCodeFormatterConstants.TRUE);
		
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
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		} finally {
			context.dispose();
		}
		
	    return edits;
    }
	
	private static TextEdit format(ITypedRegion region, IFormattingContext context, CommentFormattingStrategy formattingStrategy, String content) {
	    TypedPosition typedPosition= new TypedPosition(region.getOffset(), region.getLength(), region.getType());
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
	
	private final ICompilationUnit fCompilationUnit;
	private final TextChange fChange;

	public CommentFormatFix(TextChange change, ICompilationUnit compilationUnit) {
		fChange= change;
		fCompilationUnit= compilationUnit;
    }

	/**
	 * {@inheritDoc}
	 */
	public TextChange createChange() throws CoreException {
		return fChange;
	}

	/**
	 * {@inheritDoc}
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return MultiFixMessages.CommentFormatFix_description;
	}

	/**
	 * {@inheritDoc}
	 */
	public IStatus getStatus() {
	    return StatusInfo.OK_STATUS;
	}
}
