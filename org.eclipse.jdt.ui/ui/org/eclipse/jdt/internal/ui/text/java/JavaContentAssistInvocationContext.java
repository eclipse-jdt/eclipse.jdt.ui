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
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;

/**
 * 
 * @since 3.2
 */
public class JavaContentAssistInvocationContext extends TextContentAssistInvocationContext {
	private final IEditorPart fEditor;
	
	private ICompilationUnit fCU= null;
	private boolean fCUComputed= false;
	
	private ASTNode fAST= null;
	private boolean fASTComputed= false;

	private CompletionProposalCollector fCollector;

	public JavaContentAssistInvocationContext(ITextViewer viewer, int offset, IEditorPart editor) {
		super(viewer, offset);
		Assert.isNotNull(editor);
		fEditor= editor;
	}
	
	public ICompilationUnit computeCompilationUnit() {
		if (!fCUComputed) {
			fCUComputed= true;
			fCU= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
		}
		return fCU;
	}
	
	public ASTNode computeAST(IProgressMonitor pm) {
		if (!fASTComputed) {
			fASTComputed= true;
			ICompilationUnit unit= computeCompilationUnit();
			if (unit != null) {
				final ASTNode[] astholder= new ASTNode[0];
				ICompilationUnit[] cus= {unit};
				ASTParser.newParser(0).createASTs(cus, null, new ASTRequestor() {
					/*
					 * @see org.eclipse.jdt.core.dom.ASTRequestor#acceptAST(org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.core.dom.CompilationUnit)
					 */
					public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
						astholder[0]= ast;
					}
				}, pm);
				
				fAST= astholder[0];
			}
		}
		
		return fAST;
	}

	public CompletionProposalCollector getCollector() {
		return fCollector;
	}
	
	public void setCollector(CompletionProposalCollector collector) {
		fCollector= collector;
	}
	
	/*
	 * Implementation note: There is no need to override hashcode and equals, as the only change is
	 * the editor, which is equal anyway if the viewer is.
	 */
}
