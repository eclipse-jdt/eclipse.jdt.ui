/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.jface.text.ITextSelection;import org.eclipse.jface.util.Assert;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.ui.texteditor.IDocumentProvider;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.Refactoring;import org.eclipse.jdt.core.refactoring.code.ExtractMethodRefactoring;import org.eclipse.jdt.internal.ui.refactoring.PreviewWizardPage;import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;

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
		org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("Query Preferences for asymetric assignment and tab width");
		return new ExtractMethodRefactoring(
			fCUnit, new DocumentTextBufferChangeCreator(fDocumentProvider), 
			fSelection.getOffset(), fSelection.getLength(),
			true, 4);	// asymertic Assignment and tab with
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

	protected void addPreviewPage(){
		PreviewWizardPage page= new PreviewWizardPage();
		page.setExpandFirstNode(true);
		addPage(page);
	}
	
	protected boolean checkActivationOnOpen() {
		return true;
	}	
}