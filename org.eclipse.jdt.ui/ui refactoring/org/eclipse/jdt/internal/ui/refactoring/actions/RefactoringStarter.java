/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.window.Window;

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
import org.eclipse.jdt.internal.ui.refactoring.PerformRefactoringUtil;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog2;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * A helper class to activate the UI of a refactoring
 */
public class RefactoringStarter {
	
	private RefactoringSaveHelper fSaveHelper= new RefactoringSaveHelper();
	
	public Object activate(Refactoring refactoring, RefactoringWizard wizard, Shell parent, String dialogTitle, boolean mustSaveEditors) throws JavaModelException {
		if (! canActivate(mustSaveEditors))
			return null;
		RefactoringStatus activationStatus= checkActivation(refactoring, dialogTitle);
		if (activationStatus.hasFatalError()){
			return RefactoringErrorDialogUtil.open(dialogTitle, activationStatus);
		} else {
			wizard.setActivationStatus(activationStatus);
			Dialog dialog;
			if (wizard.hasMultiPageUserInput())
				dialog= new RefactoringWizardDialog(parent, wizard);
			else 
				dialog= new RefactoringWizardDialog2(parent, wizard);
			if (dialog.open() == Dialog.CANCEL)
				fSaveHelper.triggerBuild();
			return null;	
		} 
	}
		
	public Object activate(final IRenameRefactoring renameRefactoring, Shell parent, String dialogTitle, String dialogMessage, boolean mustSaveEditors, Object element) throws JavaModelException {
		if (! canActivate(mustSaveEditors))
			return null;
		//XXX
		if (! checkReadOnly(element))
			return null;
		Refactoring refactoring= (Refactoring)renameRefactoring;
		RefactoringStatus activationStatus= checkActivation(refactoring, dialogTitle);
		if (activationStatus.hasFatalError()){
			return RefactoringErrorDialogUtil.open(dialogTitle, activationStatus);
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
			IRunnableContext context= new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell());
			PerformRefactoringUtil.performRefactoring(pco, refactoring, context, parent);
			return null;
		} 		
	}
	
	private RefactoringStatus checkActivation(Refactoring refactoring, String errorDialogTitle){		
		try {
			CheckConditionsOperation cco= new CheckConditionsOperation(refactoring, CheckConditionsOperation.ACTIVATION);
			IRunnableContext context= new BusyIndicatorRunnableContext();
			context.run(false, false, cco);
			return cco.getStatus();
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, "Error", RefactoringMessages.getString("RefactoringStarter.unexpected_exception"));//$NON-NLS-1$ //$NON-NLS-2$
			return RefactoringStatus.createFatalErrorStatus(RefactoringMessages.getString("RefactoringStarter.unexpected_exception"));//$NON-NLS-1$
		} catch (InterruptedException e) {
			Assert.isTrue(false);
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
}
