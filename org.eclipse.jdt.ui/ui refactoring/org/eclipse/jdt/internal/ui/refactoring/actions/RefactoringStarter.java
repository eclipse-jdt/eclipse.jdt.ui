/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.window.Window;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.CheckConditionsOperation;
import org.eclipse.jdt.internal.ui.refactoring.CreateChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringPreferences;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog2;

/**
 * A helper class to activate the UI of a refactoring
 */
public class RefactoringStarter {
	
	private RefactoringSaveHelper fSaveHelper= new RefactoringSaveHelper();
	
	public Object activate(Refactoring refactoring, RefactoringWizard wizard, Shell parent, String dialogTitle, boolean mustSaveEditors) throws JavaModelException {
		if (! canActivate(mustSaveEditors))
			return null;
		RefactoringStatus activationStatus= refactoring.checkActivation(new NullProgressMonitor());
		if (! activationStatus.hasFatalError()){
			wizard.setActivationStatus(activationStatus);
			Dialog dialog;
			if (RefactoringPreferences.useWizardUI() || mustUseWizardUI(wizard))
				dialog= new RefactoringWizardDialog(parent, wizard);
			else 
				dialog= new RefactoringWizardDialog2(parent, wizard);
			if (dialog.open() == Dialog.CANCEL)
				fSaveHelper.triggerBuild();
			return null;	
		} else{
			return RefactoringErrorDialogUtil.open(dialogTitle, activationStatus);
		}	
	}
		
	public Object activate(final IRenameRefactoring renameRefactoring, Shell parent, String dialogTitle, String dialogMessage, boolean mustSaveEditors, Object element) throws JavaModelException {
		if (! canActivate(mustSaveEditors))
			return null;
		//XXX
		if (! checkReadOnly(element))
			return null;
		Refactoring refactoring= (Refactoring)renameRefactoring;
		RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor()); 
		if (status.hasFatalError()){
			return RefactoringErrorDialogUtil.open(dialogTitle, status);
		} else{
			IInputValidator validator= new IInputValidator(){
				public String isValid(String newText){
					try{
						RefactoringStatus check= renameRefactoring.checkNewName(newText);
						if (check.isOK())
							return null;
						return check.getFirstMessage(RefactoringStatus.INFO);	
					}	catch (JavaModelException e){
						JavaPlugin.log(e);
						return RefactoringMessages.getString("RefactoringStarter.unexpected_exception"); //$NON-NLS-1$
					}
				}
			};
			InputDialog dialog= new RenameInputDialog(parent, dialogTitle, dialogMessage, renameRefactoring.getCurrentName(), validator, renameRefactoring);
			int result= dialog.open();
			if (result != Window.OK) {
				fSaveHelper.triggerBuild();
				return null;
			}
			renameRefactoring.setNewName(dialog.getValue());
			PerformChangeOperation pco= new PerformChangeOperation(new CreateChangeOperation(refactoring, CheckConditionsOperation.PRECONDITIONS));
			PerformRefactoringUtil.performRefactoring(pco, refactoring);
			return null;
		} 		
	}
	
	private boolean checkReadOnly(Object element) throws JavaModelException{
		//Do a quick read only check
		if (isReadOnly(element))
			return MessageDialog.openQuestion(
				JavaPlugin.getActiveWorkbenchShell(),
				RefactoringMessages.getString("RefactoringStarter.rename"), //$NON-NLS-1$
				RefactoringMessages.getFormattedString("RefactoringStarter.is_read_only", ReorgUtils.getName(element))); //$NON-NLS-1$
		else
			return true;
	}
	
	private boolean isReadOnly(Object element) throws JavaModelException{
		if (element instanceof IResource)
			return ((IResource)element).isReadOnly();
		if (element instanceof IJavaProject)	
			return ((IJavaProject)element).getProject().isReadOnly();
		if (element instanceof IPackageFragmentRoot)
			return isReadOnly((IPackageFragmentRoot)element);
		Assert.isTrue(false);
		return false;	
	}
	
	private boolean isReadOnly(IPackageFragmentRoot root) throws JavaModelException{
		if (Checks.isClasspathDelete(root))
			return false;
		return root.getResource().isReadOnly();
	}
	
	private boolean canActivate(boolean mustSaveEditors) {
		return ! mustSaveEditors || fSaveHelper.saveEditors();
	}
	
	private boolean mustUseWizardUI(RefactoringWizard wizard) {
		return wizard.hasMultiPageUserInput();
	}
}
