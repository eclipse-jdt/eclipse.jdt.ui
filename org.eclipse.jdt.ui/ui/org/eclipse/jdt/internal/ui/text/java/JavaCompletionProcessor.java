package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.ContextTypeRegistry;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.template.TemplateEngine;


/**
 * Java completion processor.
 */
public class JavaCompletionProcessor implements IContentAssistProcessor {
	
	private static class CompletionProposalComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			ICompletionProposal c1= (ICompletionProposal) o1;
			ICompletionProposal c2= (ICompletionProposal) o2;
			return c1.getDisplayString().compareToIgnoreCase(c2.getDisplayString());
		}
	};
	
	private final static String VISIBILITY= "org.eclipse.jdt.core.codeComplete.visibilityCheck";
	private final static String ENABLED= "enabled";
	private final static String DISABLED= "disabled";
	
	
	private IEditorPart fEditor;
	private ResultCollector fCollector;
	private IWorkingCopyManager fManager;
	private IContextInformationValidator fValidator;
	
	private char[] fProposalAutoActivationSet;
	private Comparator fComparator;
	private boolean fAllowAddImports;
	
	private TemplateEngine fTemplateEngine;
	private ExperimentalResultCollector fExperimentalCollector;	
	
	
	public JavaCompletionProcessor(IEditorPart editor) {
		fEditor= editor;
		fCollector= new ResultCollector();
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		ContextType contextType= ContextTypeRegistry.getInstance().getContextType("java");
		if (contextType != null)
			fTemplateEngine= new TemplateEngine(contextType);
		fExperimentalCollector= new ExperimentalResultCollector();
		fAllowAddImports= true;
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
	 * Tells this processor to restrict its proposal to those element
	 * visible in the actual invocation context.
	 * 
	 * @param restrict <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToVisibility(boolean restrict) {
		Hashtable options= JavaCore.getOptions();
		Object value= options.get(VISIBILITY);
		if (value instanceof String) {
			String newValue= restrict ? ENABLED : DISABLED;
			if ( !newValue.equals((String) value)) {
				options.put(VISIBILITY, newValue);
				JavaCore.setOptions(options);
			}
		}
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
	 * Tells this processor to add import statement for proposals that have
	 * a fully qualified type name
	 * 
	 * @param restrict <code>true</code> if import can be added
	 */
	public void allowAddingImports(boolean allowAddingImports) {
		fAllowAddImports= allowAddingImports;
	}	
		
	/**
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fCollector.getErrorMessage();
	}

	/**
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		if (fValidator == null)
			fValidator= new JavaParameterListValidator();
		return fValidator;
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
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());
		ICompletionProposal[] results;

		if (ExperimentalPreference.fillArgumentsOnMethodCompletion(JavaPlugin.getDefault().getPreferenceStore())) {
				
			try {
				if (unit != null) {
	
					fExperimentalCollector.reset(offset, unit.getJavaProject(), fAllowAddImports ? unit : null);
					fExperimentalCollector.setViewer(viewer);
					
					Point selection= viewer.getSelectedRange();
					if (selection.y > 0)
						fExperimentalCollector.setReplacementLength(selection.y);
					
					unit.codeComplete(offset, fExperimentalCollector);
				}
			} catch (JavaModelException x) {
				Shell shell= viewer.getTextWidget().getShell();
				ErrorDialog.openError(shell, JavaTextMessages.getString("CompletionProcessor.error.accessing.title"), JavaTextMessages.getString("CompletionProcessor.error.accessing.message"), x.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
			}				

			results= fExperimentalCollector.getResults();

		} else {

			try {
				if (unit != null) {
	
					fCollector.reset(offset, unit.getJavaProject(), fAllowAddImports ? unit : null);
					Point selection= viewer.getSelectedRange();
					if (selection.y > 0)
						fCollector.setReplacementLength(selection.y);
					
					unit.codeComplete(offset, fCollector);
				}
			} catch (JavaModelException x) {
				Shell shell= viewer.getTextWidget().getShell();
				ErrorDialog.openError(shell, JavaTextMessages.getString("CompletionProcessor.error.accessing.title"), JavaTextMessages.getString("CompletionProcessor.error.accessing.message"), x.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
			}
			
			results= fCollector.getResults();
		}

		if (fTemplateEngine != null) {
			try {
				fTemplateEngine.reset();
				fTemplateEngine.complete(viewer, offset, unit);
			} catch (JavaModelException x) {
				Shell shell= viewer.getTextWidget().getShell();
				ErrorDialog.openError(shell, JavaTextMessages.getString("CompletionProcessor.error.accessing.title"), JavaTextMessages.getString("CompletionProcessor.error.accessing.message"), x.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
			}				
			
			ICompletionProposal[] templateResults= fTemplateEngine.getResults();
	
			// concatenate arrays
			ICompletionProposal[] total= new ICompletionProposal[results.length + templateResults.length];
			System.arraycopy(templateResults, 0, total, 0, templateResults.length);
			System.arraycopy(results, 0, total, templateResults.length, results.length);
			results= total;
		}
		
		/*
		 * Order here and not in result collector to make sure that the order
		 * applies to all proposals and not just those of the compilation unit. 
		 */
		return order(results);
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