package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.template.TemplateContext;
import org.eclipse.jdt.internal.ui.text.template.TemplateEngine;



/**
 * Simple Java doc completion processor.
 */
public class JavaDocCompletionProcessor implements IContentAssistProcessor {
	
	private static class CompletionProposalComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			ICompletionProposal c1= (ICompletionProposal) o1;
			ICompletionProposal c2= (ICompletionProposal) o2;
			return c1.getDisplayString().compareTo(c2.getDisplayString());
		}
	};
	
	private IEditorPart fEditor;
	private IWorkingCopyManager fManager;
	private char[] fProposalAutoActivationSet;
	private Comparator fComparator;
	private TemplateEngine fTemplateEngine;
	
	
	public JavaDocCompletionProcessor(IEditorPart editor) {
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		fTemplateEngine= new TemplateEngine(TemplateContext.JAVADOC);
	}
	
	/**
	 * Tells this processor to order the proposals alphabetically.
	 * 
	 * @param order <code>true</code> if proposals should be ordered.
	 */
	public void orderProposalsAlphabetically(boolean order) {
		fComparator= order ? new CompletionProposalComparator() : null;
	}
	
	/**
	 * Tells this processor to restrict is proposals to those
	 * starting with matching cases.
	 * 
	 * @param restrict <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToMatchingCases(boolean restrict) {
		// not yet supported
	}
	
	/**
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return fProposalAutoActivationSet;
	}
	
	/**
	 * Sets this processor's set of characters triggering the activation of the
	 * completion proposal computation.
	 * 
	 * @param activationSet the activation set
	 */
	public void setCompletionProposalAutoActivationCharacters(char[] activationSet) {
		fProposalAutoActivationSet= activationSet;
	}

	/**
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());
		IDocument document= viewer.getDocument();

		ICompletionProposal[] results= new ICompletionProposal[0];

		try {
			if (unit != null) {
				
				int offset= documentOffset;
				int length= 0;
				
				Point selection= viewer.getSelectedRange();
				if (selection.y > 0) {
					offset= selection.x;
					length= selection.y;
				}
				
				CompletionEvaluator evaluator= new CompletionEvaluator(unit, document, offset, length);
				results= evaluator.computeProposals();
			}
		} catch (JavaModelException x) {
		}

		try {
			fTemplateEngine.reset();
			fTemplateEngine.complete(viewer, documentOffset, unit);
		} catch (JavaModelException x) {
		}				
		
		ICompletionProposal[] templateResults= fTemplateEngine.getResults();

		// concatenate arrays
		ICompletionProposal[] total= new ICompletionProposal[results.length + templateResults.length];
		System.arraycopy(templateResults, 0, total, 0, templateResults.length);
		System.arraycopy(results, 0, total, templateResults.length, results.length);

		/*
		 * Order here and not in result collector to make sure that the order
		 * applies to all proposals and not just those of the compilation unit. 
		 */
		return order(total);
	}
	
	/**
	 * Order the given proposals.
	 */
	private ICompletionProposal[] order(ICompletionProposal[] proposals) {
		if (fComparator != null)
			Arrays.sort(proposals, fComparator);
		return proposals;	
	}
}