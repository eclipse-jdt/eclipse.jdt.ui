/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;

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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.BuildAction;

import org.eclipse.ui.internal.GlobalBuildAction;

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
import org.eclipse.jdt.internal.ui.refactoring.RefactoringPreferences;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusContentProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusEntryLabelProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;

/**
 * A helper class to ativate the UI of a refactoring
 */
public class RefactoringStarter {
	
	private boolean fSavedFiles;
	private boolean fAutobuildState;
	
	public void activate(Refactoring refactoring, RefactoringWizard wizard, String dialogTitle, boolean mustSaveEditors) throws JavaModelException {
		if (! canActivate(mustSaveEditors))
			return;
		RefactoringStatus activationStatus= refactoring.checkActivation(new NullProgressMonitor());
		if (! activationStatus.hasFatalError()){
			wizard.setActivationStatus(activationStatus);
			RefactoringWizardDialog dialog= new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard);
			if (dialog.open() == Dialog.CANCEL)
				triggerBuild();
				
		} else{
			RefactoringErrorDialog.open(dialogTitle, activationStatus);
		}	
	}
	
	public void activate(final IRenameRefactoring renameRefactoring, String dialogTitle, String dialogMessage, boolean mustSaveEditors, Object element) throws JavaModelException {
		if (! canActivate(mustSaveEditors))
			return;
		//XXX
		if (! checkReadOnly(element))
			return;
		Refactoring refactoring= (Refactoring)renameRefactoring;
		RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor()); 
		if (status.hasFatalError()){
			RefactoringErrorDialog.open(dialogTitle, status);
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
						return "Unexpected exception. See log for details."; //don't want to show the error dialog
					}
				}
			};
			InputDialog dialog= new InputDialog(shell, dialogTitle, dialogMessage, renameRefactoring.getCurrentName(), validator);
			int result= dialog.open();
			if (result != Window.OK) {
				triggerBuild();
				return;
			}
			renameRefactoring.setNewName(dialog.getValue());
			PerformChangeOperation pco= new PerformChangeOperation(new CreateChangeOperation(refactoring, CheckConditionsOperation.PRECONDITIONS));
			PerformRefactoringUtil.performRefactoring(pco, refactoring);
		} 		
	}
	
	private boolean checkReadOnly(Object element) throws JavaModelException{
		//Do a quick read only check
		if (isReadOnly(element))
			return MessageDialog.openQuestion(
				JavaPlugin.getActiveWorkbenchShell(),
				"Rename",
				MessageFormat.format("{0} is read only. Do you still wish to rename it?", new Object[] {ReorgUtils.getName(element)}));
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
			ExceptionHandler.handle(e, shell, "Saving Resources", "Unexpected exception. See log for details."); 
			return false;
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, "Saving Resources", "Unexpected exception. See log for details."); 
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
				pm.beginTask("Saving dirty editors", editorsToSave.length);
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
				check.setText("&Always save all modified resources automatically prior to refactoring");
				check.setSelection(RefactoringPreferences.getSaveAllEditors());
				check.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						RefactoringPreferences.setSaveAllEditors(check.getSelection());
					}
				});
				return result;
			}
		};
		dialog.setTitle("Save all modified resources");
		dialog.setAddCancelButton(true);
		dialog.setLabelProvider(createDialogLabelProvider());
		dialog.setMessage("All modified resources must be saved before this operation.\nPress OK to confirm or Cancel otherwise.");
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
			new GlobalBuildAction(JavaPlugin.getDefault().getWorkbench(), IncrementalProjectBuilder.INCREMENTAL_BUILD).run();
		}
	}
}
