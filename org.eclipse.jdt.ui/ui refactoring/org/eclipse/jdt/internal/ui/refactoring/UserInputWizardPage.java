/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.Arrays;import java.util.Iterator;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.IStructuredContentProvider;import org.eclipse.jface.viewers.LabelProvider;import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.core.resources.IFile;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.ui.IEditorInput;import org.eclipse.ui.IEditorPart;import org.eclipse.ui.IFileEditorInput;import org.eclipse.ui.dialogs.ListSelectionDialog;import org.eclipse.jdt.internal.corext.refactoring.DebugUtils;import org.eclipse.jdt.internal.corext.refactoring.base.IChange;import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * An abstract wizard page that can be used to implement user input pages for 
 * refactoring wizards. Usually user input pages are pages shown at the beginning 
 * of a wizard. As soon as the "last" user input page is left a corresponding 
 * precondition check is executed.
 */
public abstract class UserInputWizardPage extends RefactoringWizardPage {

	private boolean fIsLastUserPage;
	
	/**
	 * Creates a new user input page.
	 * @param name the page's name.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 */
	public UserInputWizardPage(String name, boolean isLastUserPage) {
		super(name);
		fIsLastUserPage= isLastUserPage;
	}
	
	/* (non-Javadoc)
	 * Method declared in WizardPage
	 */
	public void setVisible(boolean visible) {
		if (visible)
			getRefactoringWizard().setChange(null);
		super.setVisible(visible);
	}
	
	/* (non-JavaDoc)
	 * Method declared in IWizardPage.
	 */
	public IWizardPage getNextPage() {
		if (fIsLastUserPage) 
			return getRefactoringWizard().computeUserInputSuccessorPage(this);
		else
			return super.getNextPage();
	}
	
	/* (non-JavaDoc)
	 * Method declared in IWizardPage.
	 */
	public boolean canFlipToNextPage() {
		if (fIsLastUserPage) {
			// we can't call getNextPage to determine if flipping is allowed since computing
			// the next page is quite expensive (checking preconditions and creating a
			// change). So we say yes if the page is complete.
			return isPageComplete();
		} else {
			return super.canFlipToNextPage();
		}
	}
	
	/* (non-JavaDoc)
	 * Method defined in RefactoringWizardPage
	 */
	protected boolean performFinish() {
		RefactoringWizard wizard= getRefactoringWizard();
		int threshold= RefactoringPreferences.getCheckPassedSeverity();
		RefactoringStatus activationStatus= wizard.getActivationStatus();
		RefactoringStatus inputStatus= null;
		RefactoringStatus status= new RefactoringStatus();
		Refactoring refactoring= getRefactoring();
		boolean result= false;
		
		if (activationStatus != null && activationStatus.getSeverity() > threshold) {
			inputStatus= wizard.checkInput();
		} else {
			CreateChangeOperation create= new CreateChangeOperation(refactoring, CreateChangeOperation.CHECK_INPUT); 
			create.setCheckPassedSeverity(threshold);
			
			PerformChangeOperation perform= new PerformChangeOperation(create);
			perform.setCheckPassedSeverity(threshold);
			
			result= wizard.performFinish(perform);
			if (!result)
				return false;
			inputStatus= create.getStatus();
		}
		
		status.merge(activationStatus);
		status.merge(inputStatus);
		
		if (status.getSeverity() > threshold) {
			wizard.setStatus(status);
			IWizardPage nextPage= wizard.getPage(ErrorWizardPage.PAGE_NAME);
			wizard.getContainer().showPage(nextPage);
			return false;
		}
		
		return result;	
	}
}