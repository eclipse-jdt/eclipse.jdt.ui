/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.sef;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.IField;

import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.PreviewWizardPage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
public class SelfEncapsulateFieldWizard extends RefactoringWizard {
	
	public SelfEncapsulateFieldWizard(IField field, IDocumentProvider provider) {
		super(doGetRefactoring(field, provider), 
					"Self Encapsulate Field",
					IJavaHelpContextIds.SEF_WIZARD_PAGE);
	}

	protected static Refactoring doGetRefactoring(IField field, IDocumentProvider provider) {
		Assert.isNotNull(field);
		Assert.isNotNull(provider);		
		
		return new SelfEncapsulateFieldRefactoring(
			field, new DocumentTextBufferChangeCreator(provider), 
			CodeFormatterPreferencePage.getTabSize());
	}
	
	protected void addUserInputPages() {
		addPage(new SelfEncapsulateFieldInputPage());
	}

	protected void addPreviewPage() {
		PreviewWizardPage page= new PreviewWizardPage();
		addPage(page);
	}
	
	protected boolean checkActivationOnOpen() {
		return true;
	}	
}