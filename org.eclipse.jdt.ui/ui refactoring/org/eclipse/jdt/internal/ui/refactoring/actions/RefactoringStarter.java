/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.GlobalBuildAction;

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
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;

/**
 * A helper class to activate the UI of a refactoring
 */
public class RefactoringStarter {
	
	private boolean fSavedFiles;
	private boolean fAutobuildState;
	
	public Object activate(Refactoring refactoring, RefactoringWizard wizard, String dialogTitle, boolean mustSaveEditors) throws JavaModelException {
		if (! canActivate(mustSaveEditors))
			return null;
		RefactoringStatus activationStatus= refactoring.checkActivation(new NullProgressMonitor());
		if (! activationStatus.hasFatalError()){
			wizard.setActivationStatus(activationStatus);
			RefactoringWizardDialog dialog= new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard);
			if (dialog.open() == Dialog.CANCEL)
				triggerBuild();
			return null;	
		} else{
			return RefactoringErrorDialogUtil.open(dialogTitle, activationStatus);
		}	
	}
	
	public Object activate(final IRenameRefactoring renameRefactoring, String dialogTitle, String dialogMessage, boolean mustSaveEditors, Object element) throws JavaModelException {
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
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
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
			InputDialog dialog= new RenameInputDialog(shell, dialogTitle, dialogMessage, renameRefactoring.getCurrentName(), validator, renameRefactoring);
			int result= dialog.open();
			if (result != Window.OK) {
				triggerBuild();
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
			return ((IJavaProject)element).getCorrespondingResource().isReadOnly();
		if (element instanceof IPackageFragmentRoot)
			return isReadOnly((IPackageFragmentRoot)element);
		Assert.isTrue(false);
		return false;	
	}
	
	private boolean isReadOnly(IPackageFragmentRoot root) throws JavaModelException{
		if (Checks.isClasspathDelete(root))
			return false;
		return root.getCorrespondingResource().isReadOnly();
	}
	
	private boolean canActivate(boolean mustSaveEditors) {
		return ! mustSaveEditors || areAllEditorsSaved();
	}
	
	private boolean areAllEditorsSaved(){
		if (JavaPlugin.getDirtyEditors().length == 0)
			return true;
		if (! saveAllDirtyEditors())
			return false;
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		try {
			// Save isn't cancelable.
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			IWorkspaceDescription description= workspace.getDescription();
			boolean autoBuild= description.isAutoBuilding();
			description.setAutoBuilding(false);
			workspace.setDescription(description);
			try {
				new ProgressMonitorDialog(shell).run(false, false, createRunnable());
				fSavedFiles= true;
			} finally {
				description.setAutoBuilding(autoBuild);
				workspace.setDescription(description);
			}
			return true;
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, shell, 
				RefactoringMessages.getString("RefactoringStarter.saving"), RefactoringMessages.getString("RefactoringStarter.unexpected_exception"));  //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, 
				RefactoringMessages.getString("RefactoringStarter.saving"), RefactoringMessages.getString("RefactoringStarter.unexpected_exception"));  //$NON-NLS-1$ //$NON-NLS-2$
			return false;			
		} catch (InterruptedException e) {
			Assert.isTrue(false); // Can't happen. Operation isn't cancelable.
			return false;
		}
	}

	private IRunnableWithProgress createRunnable() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				IEditorPart[] editorsToSave= JavaPlugin.getDirtyEditors();
				pm.beginTask(RefactoringMessages.getString("RefactoringStarter.saving_dirty_editors"), editorsToSave.length); //$NON-NLS-1$
				for (int i= 0; i < editorsToSave.length; i++) {
					editorsToSave[i].doSave(new SubProgressMonitor(pm, 1));
					pm.worked(1);
				}
				pm.done();
			}
		};
	}

	private boolean saveAllDirtyEditors() {
		if (RefactoringPreferences.getSaveAllEditors()) //must save everything
			return true;
		ListDialog dialog= new ListDialog(JavaPlugin.getActiveWorkbenchShell()) {
			protected Control createDialogArea(Composite parent) {
				Composite result= (Composite) super.createDialogArea(parent);
				final Button check= new Button(result, SWT.CHECK);
				check.setText(RefactoringMessages.getString("RefactoringStarter.always_save")); //$NON-NLS-1$
				check.setSelection(RefactoringPreferences.getSaveAllEditors());
				check.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						RefactoringPreferences.setSaveAllEditors(check.getSelection());
					}
				});
				return result;
			}
		};
		dialog.setTitle(RefactoringMessages.getString("RefactoringStarter.save_all_resources")); //$NON-NLS-1$
		dialog.setAddCancelButton(true);
		dialog.setLabelProvider(createDialogLabelProvider());
		dialog.setMessage(RefactoringMessages.getString("RefactoringStarter.must_save")); //$NON-NLS-1$
		dialog.setContentProvider(new ListContentProvider());
		dialog.setInput(Arrays.asList(JavaPlugin.getDirtyEditors()));
		return dialog.open() == Dialog.OK;
	}

	private ILabelProvider createDialogLabelProvider() {
		return new LabelProvider() {
			public Image getImage(Object element) {
				return ((IEditorPart) element).getTitleImage();
			}
			public String getText(Object element) {
				return ((IEditorPart) element).getTitle();
			}
		};
	}
	
	private void triggerBuild() {
		if (fSavedFiles && ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding()) {
			new GlobalBuildAction(JavaPlugin.getActiveWorkbenchWindow(), IncrementalProjectBuilder.INCREMENTAL_BUILD).run();
		}
	}
}
