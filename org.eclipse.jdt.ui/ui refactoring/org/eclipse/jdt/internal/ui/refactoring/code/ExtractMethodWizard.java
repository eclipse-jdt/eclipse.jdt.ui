/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.jface.text.ITextSelection;import org.eclipse.jface.util.Assert;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.ui.texteditor.IDocumentProvider;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;import org.eclipse.jdt.internal.core.refactoring.code.ExtractMethodRefactoring;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;import org.eclipse.jdt.internal.ui.refactoring.PreviewWizardPage;import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
public class ExtractMethodWizard extends RefactoringWizard {
	
	private ICompilationUnit fCUnit;
	private ITextSelection fSelection;
	private IDocumentProvider fDocumentProvider;

	public ExtractMethodWizard(ICompilationUnit cunit, ITextSelection selection, IDocumentProvider provider) {
		super(doGetRefactoring(cunit, selection, provider), 
					RefactoringMessages.getString("ExtractMethodWizard.extract_method"), 
					IJavaHelpContextIds.EXTRACT_METHOD_ERROR_WIZARD_PAGE); //$NON-NLS-1$
		fCUnit= cunit;
		fSelection= selection;
		fDocumentProvider= provider;
	}

	protected static Refactoring doGetRefactoring(ICompilationUnit cunit, ITextSelection selection, IDocumentProvider provider) {
		Assert.isNotNull(cunit);
		Assert.isNotNull(selection);	
		Assert.isNotNull(provider);		
		
		return new ExtractMethodRefactoring(
			cunit, new DocumentTextBufferChangeCreator(provider), 
			selection.getOffset(), selection.getLength(),
			CodeFormatterPreferencePage.isCompactingAssignment(),
			CodeFormatterPreferencePage.getTabSize());
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