/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.code.ExtractMethodRefactoring;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;

public class ExtractMethodWizard extends RefactoringWizard {
	
	private ICompilationUnit fCUnit;
	private ITextSelection fSelection;
	private IDocumentProvider fDocumentProvider;

	public ExtractMethodWizard(ICompilationUnit cunit, ITextSelection selection, IDocumentProvider provider) {
		super("Extract Method");
		fCUnit= cunit;
		Assert.isNotNull(fCUnit);
		fSelection= selection;
		Assert.isNotNull(fSelection);
		fDocumentProvider= provider;
		Assert.isNotNull(fDocumentProvider);
		init(doGetRefactoring());
	}

	protected Refactoring doGetRefactoring() {
		return new ExtractMethodRefactoring(
			fCUnit, new DocumentTextBufferChangeCreator(fDocumentProvider), 
			fSelection.getOffset(), fSelection.getLength());
	}
	
	
	public IChange createChange(){
		// creating the change is cheap. So we don't need to show progress.
		try {
			return getRefactoring().createChange(new NullProgressMonitor());
		} catch (JavaModelException e) {
			return null;
		}	
	}
		
	protected void addUserInputPages(){
		addPage(new ExtractMethodInputPage());
	}

	protected boolean checkActivationOnOpen() {
		return true;
	}	
}