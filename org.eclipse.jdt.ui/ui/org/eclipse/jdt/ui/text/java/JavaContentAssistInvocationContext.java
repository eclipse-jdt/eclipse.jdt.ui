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

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.ui.JavaUI;

/**
 * Describes the context of a content assist invocation in a Java editor. The context knows the
 * {@link ICompilationUnit compilation unit}, the
 * {@link CompletionProposalCollector proposal collector} used to get Java core proposals and the
 * {@link CompletionContext core completion context} received from core.
 * <p>
 * Clients may use this class.
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
	
	private CompletionProposalCollector fCollector;

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
	 * Returns the compilation unit that content assist is invoked in.
	 * 
	 * @return the compilation unit that content assist is invoked in
	 */
	public ICompilationUnit computeCompilationUnit() {
		if (!fCUComputed) {
			fCUComputed= true;
			fCU= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
		}
		return fCU;
	}
	
	/**
	 * Returns the compeletion requestor that was used to get core proposals, or <code>null</code>
	 * if no core proposals have been requested.
	 * 
	 * @return the compeletion requestor that was used to get core proposals, or <code>null</code>
	 */
	public CompletionProposalCollector getCollector() {
		return fCollector;
	}
	
	/**
	 * XXX internal - do not use
	 */
	public void setCollector(CompletionProposalCollector collector) {
		fCollector= collector;
	}
	
	/**
	 * Returns the {@link CompletionContext core completion context} if available, <code>null</code>
	 * otherwise. Shortcut for <code>getCollector().getContext()</code>.
	 * 
	 * @return the core completion context if available, <code>null</code> otherwise
	 */
	public CompletionContext getContext() {
		if (fCollector != null)
			return fCollector.getContext();
		return null;
	}
	
	/*
	 * Implementation note: There is no need to override hashcode and equals, as the only change is
	 * the editor, which is equal anyway if the viewer is.
	 */
}
