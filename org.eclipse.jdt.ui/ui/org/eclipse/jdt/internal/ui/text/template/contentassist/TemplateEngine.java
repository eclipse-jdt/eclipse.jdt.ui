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
package org.eclipse.jdt.internal.ui.text.template.contentassist;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.LinkedEnvironment;
import org.eclipse.jface.text.templates.ContextType;
import org.eclipse.jface.text.templates.GlobalVariables;
import org.eclipse.jface.text.templates.Template;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType;
import org.eclipse.jdt.internal.corext.template.java.Templates;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class TemplateEngine {

	private static final String $_LINE_SELECTION= "${" + GlobalVariables.LineSelection.NAME + "}"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String $_WORD_SELECTION= "${" + GlobalVariables.WordSelection.NAME + "}"; //$NON-NLS-1$ //$NON-NLS-2$

	/** The context type. */
	private ContextType fContextType;
	/** The result proposals. */
	private ArrayList fProposals= new ArrayList();

	/**
	 * Creates the template engine for a particular context type.
	 * See <code>TemplateContext</code> for supported context types.
	 */
	public TemplateEngine(ContextType contextType) {
		Assert.isNotNull(contextType);
		fContextType= contextType;
	}

	/**
	 * Empties the collector.
	 * 
	 * @param unit   the compilation unit (may be <code>null</code>)
	 */
	public void reset() {
		fProposals.clear();
	}

	/**
	 * Returns the array of matching templates.
	 */
	public TemplateProposal[] getResults() {
		return (TemplateProposal[]) fProposals.toArray(new TemplateProposal[fProposals.size()]);
	}

	/**
	 * Inspects the context of the compilation unit around <code>completionPosition</code>
	 * and feeds the collector with proposals.
	 * @param viewer the text viewer
	 * @param completionPosition the context position in the document of the text viewer
	 * @param compilationUnit the compilation unit (may be <code>null</code>)
	 */
	public void complete(ITextViewer viewer, int completionPosition, ICompilationUnit compilationUnit)
		throws JavaModelException
	{
	    IDocument document= viewer.getDocument();
	    
		// prohibit recursion
		if (LinkedEnvironment.hasEnvironment(document))
			return;

		if (!(fContextType instanceof CompilationUnitContextType))
			return;

		Point selection= viewer.getSelectedRange();

		// remember selected text
		String selectedText= null;
		if (selection.y != 0) {
			try {
				selectedText= document.get(selection.x, selection.y);
			} catch (BadLocationException e) {}
		}

		
		CompilationUnitContext context= ((CompilationUnitContextType) fContextType).createContext(document, completionPosition, selection.y, compilationUnit);
		context.setVariable("selection", selectedText); //$NON-NLS-1$
		int start= context.getStart();
		int end= context.getEnd();
		IRegion region= new Region(start, end - start);

		Template[] templates= Templates.getInstance().getTemplates();

		if (selection.y == 0) {
			for (int i= 0; i != templates.length; i++)
				if (context.canEvaluate(templates[i]))
					fProposals.add(new TemplateProposal(templates[i], context, region, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)));

		} else {

			if (context.getKey().length() == 0)
				context.setForceEvaluation(true);

			boolean multipleLinesSelected= areMultipleLinesSelected(viewer);
				
			for (int i= 0; i != templates.length; i++) {
				Template template= templates[i];				
				if (context.canEvaluate(template) &&
					template.getContextTypeName().equals(context.getContextType().getName()) &&				
					(!multipleLinesSelected && template.getPattern().indexOf($_WORD_SELECTION) != -1 || (multipleLinesSelected && template.getPattern().indexOf($_LINE_SELECTION) != -1)))
				{
					fProposals.add(new TemplateProposal(templates[i], context, region, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)));
				}
			}
		}
	}
	
	/**
	 * Returns <code>true</code> if one line is completely selected or if multiple lines are selected.
	 * Being completely selected means that all characters except the new line characters are 
	 * selected.
	 * 
	 * @return <code>true</code> if one or multiple lines are selected
	 * @since 2.1
	 */
	private boolean areMultipleLinesSelected(ITextViewer viewer) {
		if (viewer == null)
			return false;
		
		Point s= viewer.getSelectedRange();
		if (s.y == 0)
			return false;
			
		try {
			
			IDocument document= viewer.getDocument();
			int startLine= document.getLineOfOffset(s.x);
			int endLine= document.getLineOfOffset(s.x + s.y);
			IRegion line= document.getLineInformation(startLine);
			return startLine != endLine || (s.x == line.getOffset() && s.y == line.getLength());
		
		} catch (BadLocationException x) {
			return false;
		}
	}
}
