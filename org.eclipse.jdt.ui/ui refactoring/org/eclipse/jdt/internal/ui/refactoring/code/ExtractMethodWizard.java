/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.NewPreviewWizardPage;
import org.eclipse.jdt.internal.ui.refactoring.PreviewWizardPage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
public class ExtractMethodWizard extends RefactoringWizard {
	
	public ExtractMethodWizard(ExtractMethodRefactoring ref){
		super(ref, RefactoringMessages.getString("ExtractMethodWizard.extract_method"), 
					IJavaHelpContextIds.EXTRACT_METHOD_ERROR_WIZARD_PAGE); //$NON-NLS-1$
	}

	public IChange createChange(){
		// creating the change is cheap. So we don't need to show progress.
		try {
			return getRefactoring().createChange(new NullProgressMonitor());
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		}	
	}
		
	protected void addUserInputPages(){
		addPage(new ExtractMethodInputPage());
	}

	protected void addPreviewPage(){
		NewPreviewWizardPage page= new NewPreviewWizardPage();
		page.setExpandFirstNode(true);
		addPage(page);
	}
}