/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaTemplateMessages;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;

public class TemplateEngine {

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
		if (LinkedPositionManager.hasActiveManager(document))
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
					fProposals.add(new TemplateProposal(templates[i], context, region, viewer, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)));

		} else {
			final String CURSOR= "${" + JavaTemplateMessages.getString("GlobalVariables.variable.name.cursor") + '}'; //$NON-NLS-1$ //$NON-NLS-2$

			if (context.getKey().length() == 0)
				context.setForceEvaluation(true);
				
			for (int i= 0; i != templates.length; i++) {
				Template template= templates[i];				
				if (context.canEvaluate(template) &&
					template.getContextTypeName().equals(context.getContextType().getName()) &&				
					template.getPattern().indexOf(CURSOR) != -1)
				{
					fProposals.add(new TemplateProposal(templates[i], context, region, viewer, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)));
				}
			}
		}
	}
}
