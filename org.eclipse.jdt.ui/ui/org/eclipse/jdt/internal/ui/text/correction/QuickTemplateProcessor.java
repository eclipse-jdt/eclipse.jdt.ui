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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.GlobalVariables;
import org.eclipse.jface.text.templates.Template;

import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal;


/**
 * Quick template processor.
 */
public class QuickTemplateProcessor implements IQuickAssistProcessor {
	
	private static final String $_LINE_SELECTION= "${" + GlobalVariables.LineSelection.NAME + "}"; //$NON-NLS-1$ //$NON-NLS-2$
		
	public QuickTemplateProcessor() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.IAssistProcessor#hasAssists(org.eclipse.jdt.internal.ui.text.correction.IAssistContext)
	 */
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		IDocument document= getDocument(cu);
			
		int offset= context.getSelectionOffset();
		int length= context.getSelectionLength();

		try {
			int startLine= document.getLineOfOffset(offset);
			int endLine= document.getLineOfOffset(offset + length);
			IRegion region= document.getLineInformation(endLine);
			return ((startLine  < endLine) || (length > 0 && offset == region.getOffset() && length == region.getLength()));
		} catch (BadLocationException e) {
			return false;
		}
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.IAssistProcessor#getAssists(org.eclipse.jdt.internal.ui.text.correction.IAssistContext, org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		if (locations != null && locations.length > 0) {
			return new IJavaCompletionProposal[0];
		}
		
		try {
			ICompilationUnit cu= context.getCompilationUnit();
			IDocument document= getDocument(cu);
			
			int offset= context.getSelectionOffset();
			int length= context.getSelectionLength();
			// test if selection is either a full line or spans over multiple lines
			int startLine= document.getLineOfOffset(offset);
			int endLine= document.getLineOfOffset(offset + length);
			IRegion endLineRegion= document.getLineInformation(endLine);
			//if end position is at start of line, set it back to the previous line's end
			if (endLine > startLine && endLineRegion.getOffset() == offset + length) {
				endLine--;
				endLineRegion= document.getLineInformation(endLine);
				length= endLineRegion.getOffset() + endLineRegion.getLength() - offset;
			}
			if (startLine  == endLine) {
				if (length == 0 || offset != endLineRegion.getOffset() || length != endLineRegion.getLength()) {
					return null;
				}
			} else {
				// expand selection
				offset= document.getLineOffset(startLine);
				length= endLineRegion.getOffset() + endLineRegion.getLength() - offset;
			}

			ArrayList resultingCollections= new ArrayList();
			collectSurroundTemplates(document, cu, offset, length, resultingCollections);
			return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}

	private IDocument getDocument(ICompilationUnit cu) throws JavaModelException {
		IFile file= (IFile) cu.getResource();
		IDocument document= JavaUI.getDocumentProvider().getDocument(new FileEditorInput(file));
		if (document == null) {
			return new Document(cu.getSource()); // only used by test cases
		}
		return document;
	}
	
	private void collectSurroundTemplates(IDocument document, ICompilationUnit cu, int offset, int length, Collection result) throws BadLocationException {
		CompilationUnitContextType contextType= (CompilationUnitContextType) JavaPlugin.getDefault().getTemplateContextRegistry().getContextType(JavaContextType.NAME);
		CompilationUnitContext context= contextType.createContext(document, offset, length, cu);
		context.setVariable("selection", document.get(offset, length)); //$NON-NLS-1$
		context.setForceEvaluation(true);

		int start= context.getStart();
		int end= context.getEnd();
		IRegion region= new Region(start, end - start);

		Template[] templates= JavaPlugin.getDefault().getTemplateStore().getTemplates();
		for (int i= 0; i != templates.length; i++) {
			Template curr= templates[i];
			if (context.canEvaluate(curr) && curr.getContextTypeId().equals(JavaContextType.NAME) && curr.getPattern().indexOf($_LINE_SELECTION) != -1) {
				// TODO using jdt proposals for the moment, as jdt expects IJavaCompletionProposals
				TemplateProposal proposal= new TemplateProposal(curr, context, region, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE));
				String[] arg= new String[] { curr.getName(), curr.getDescription() };
				proposal.setDisplayString(CorrectionMessages.getFormattedString("QuickTemplateProcessor.surround.label", arg)); //$NON-NLS-1$
				result.add(proposal);
			}
		}
	}

}
