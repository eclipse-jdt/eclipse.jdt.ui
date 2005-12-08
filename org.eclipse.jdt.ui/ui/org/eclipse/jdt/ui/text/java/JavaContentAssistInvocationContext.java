/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.java;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.ContentAssistHistory.RHSHistory;

/**
 * Describes the context of a content assist invocation in a Java editor.
 * <p>
 * Clients may use but not subclass this class.
 * </p>
 * <p>
 * XXX this API is provisional and may change anytime during the course of 3.2
 * </p>
 * 
 * @since 3.2
 */
public class JavaContentAssistInvocationContext extends TextContentAssistInvocationContext {
	private final IEditorPart fEditor;
	
	private ICompilationUnit fCU= null;
	private boolean fCUComputed= false;
	
	private CompletionProposalLabelProvider fLabelProvider;
	private CompletionProposalCollector fCollector;
	private RHSHistory fRHSHistory;
	private IType fType;

	/**
	 * Creates a new context.
	 * 
	 * @param viewer the viewer used by the editor
	 * @param offset the invocation offset
	 * @param editor the editor that content assist is invoked in
	 */
	public JavaContentAssistInvocationContext(ITextViewer viewer, int offset, IEditorPart editor) {
		super(viewer, offset);
		Assert.isNotNull(editor);
		fEditor= editor;
	}
	
	/**
	 * Creates a new context.
	 * 
	 * @param unit the compilation unit in <code>document</code>
	 */
	public JavaContentAssistInvocationContext(ICompilationUnit unit) {
		super();
		fCU= unit;
		fCUComputed= true;
		fEditor= null;
	}
	
	/**
	 * Returns the compilation unit that content assist is invoked in, <code>null</code> if there
	 * is none.
	 * 
	 * @return the compilation unit that content assist is invoked in, possibly <code>null</code>
	 */
	public ICompilationUnit getCompilationUnit() {
		if (!fCUComputed) {
			fCUComputed= true;
			if (fCollector != null)
				fCU= fCollector.getCompilationUnit();
			else
				fCU= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
		}
		return fCU;
	}
	
	/**
	 * Returns the project of the compilation unit that content assist is invoked in,
	 * <code>null</code> if none.
	 * 
	 * @return the current java project, possibly <code>null</code>
	 */
	public IJavaProject getProject() {
		ICompilationUnit unit= getCompilationUnit();
		return unit == null ? null : unit.getJavaProject();
	}
	
	/**
	 * Returns the keyword proposals that are available in this context, possibly none.
	 * 
	 * @return the available keyword proposals.
	 */
	public IJavaCompletionProposal[] getKeywordProposals() {
		if (fCollector != null)
			return fCollector.getKeywordCompletionProposals();
		return new IJavaCompletionProposal[0];
	}
	
	/**
	 * Sets the collector, which is used to access the compilation unit, the core context and the
	 * label provider.
	 * 
	 * @param collector the collector
	 */
	void setCollector(CompletionProposalCollector collector) {
		fCollector= collector;
	}
	
	/**
	 * Returns the {@link CompletionContext core completion context} if available, <code>null</code>
	 * otherwise.
	 * 
	 * @return the core completion context if available, <code>null</code> otherwise
	 */
	public CompletionContext getCoreContext() {
		if (fCollector != null)
			return fCollector.getContext();
		return null;
	}
	
	/**
	 * Returns the content assist type history for the expected type.
	 * 
	 * @return the content assist type history for the expected type
	 */
	public RHSHistory getRHSHistory() {
		if (fRHSHistory == null) {
			CompletionContext context= getCoreContext();
			if (context != null) {
				char[][] expectedTypes= context.getExpectedTypesSignatures();
				if (expectedTypes != null && expectedTypes.length > 0) {
					String expected= SignatureUtil.stripSignatureToFQN(String.valueOf(expectedTypes[0]));
					fRHSHistory= JavaPlugin.getDefault().getContentAssistHistory().getHistory(expected, 10);
				}
			}
			if (fRHSHistory == null)
				fRHSHistory= JavaPlugin.getDefault().getContentAssistHistory().getHistory(null);
		}
		return fRHSHistory;
	}
	
	/**
	 * Returns the expected type if any, <code>null</code> otherwise.
	 * 
	 * @return the expected type if any, <code>null</code> otherwise
	 */
	public IType getExpectedType() {
		if (fType == null && getCompilationUnit() != null) {
			CompletionContext context= getCoreContext();
			if (context != null) {
				char[][] expectedTypes= context.getExpectedTypesSignatures();
				if (expectedTypes != null && expectedTypes.length > 0) {
					IJavaProject project= getCompilationUnit().getJavaProject();
					if (project != null) {
						try {
							fType= project.findType(SignatureUtil.stripSignatureToFQN(String.valueOf(expectedTypes[0])));
						} catch (JavaModelException x) {
							JavaPlugin.log(x);
						}
					}
				}
			}
		}
		return fType;
	}
	
	/**
	 * Returns a label provider that can be used to compute proposal labels.
	 * 
	 * @return a label provider that can be used to compute proposal labels
	 */
	public CompletionProposalLabelProvider getLabelProvider() {
		if (fLabelProvider == null) {
			if (fCollector != null)
				fLabelProvider= fCollector.getLabelProvider();
			else
				fLabelProvider= new CompletionProposalLabelProvider();
		}

		return fLabelProvider;
	}
	
	/*
	 * Implementation note: There is no need to override hashCode and equals, as we only add cached
	 * values shared across one assist invocation.
	 */
}
