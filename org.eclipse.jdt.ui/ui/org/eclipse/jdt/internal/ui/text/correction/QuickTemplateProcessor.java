/**********************************************************************
Copyright (c) 2000, 2003 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
	IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.ContextTypeRegistry;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.text.template.TemplateEngine;
import org.eclipse.jdt.internal.ui.text.template.TemplateProposal;


/**
 * Quick template processor.
 */
public class QuickTemplateProcessor {
		
	private JavaCompletionProposalComparator fComparator;
	private TemplateEngine fTemplateEngine;
	
	
	public QuickTemplateProcessor() {
		ContextType contextType= ContextTypeRegistry.getInstance().getContextType(JavaContextType.NAME);
		if (contextType != null)
			fTemplateEngine= new TemplateEngine(contextType);
		
		fComparator= new JavaCompletionProposalComparator();
	}

	public void computeCompletionProposals(ITextViewer viewer, ICompilationUnit unit, List result) {
		Point s= viewer.getSelectedRange();
		int offset= s.x;
		int length= s.y;

		if (fTemplateEngine != null && length > 0 && areMultipleLinesSelected(viewer, offset, length)) {
			try {
				fTemplateEngine.reset();
				fTemplateEngine.complete(viewer, offset, unit);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}				
			
			TemplateProposal[] templateResults= fTemplateEngine.getResults();
			Arrays.sort(templateResults, fComparator);
			for (int i= 0; i < templateResults.length; i++) {
				TemplateProposal proposal= templateResults[i];
				Template template= proposal.getTemplate();
				String[] arg= new String[] { template.getName(), template.getDescription() };
				proposal.setDisplayString(CorrectionMessages.getFormattedString("QuickTemplateProcessor.surround.label", arg)); //$NON-NLS-1$
				result.add(proposal);
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
	private boolean areMultipleLinesSelected(ITextViewer viewer, int offset, int length) {
		try {
			IDocument document= viewer.getDocument();
			int startLine= document.getLineOfOffset(offset);
			int endLine= document.getLineOfOffset(offset + length);
			IRegion line= document.getLineInformation(startLine);
			return startLine != endLine || (offset == line.getOffset() && length == line.getLength());
		
		} catch (BadLocationException x) {
			return false;
		}
	}

}