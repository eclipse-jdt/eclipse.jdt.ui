/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.resources.IResource;
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
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

public abstract class RefactoringAction extends Action {
	private StructuredSelectionProvider fProvider;
	protected RefactoringAction(String text, ISelectionProvider selectionProvider){
		this(text, StructuredSelectionProvider.createFrom(selectionProvider));
	}
	
	protected RefactoringAction(String text, StructuredSelectionProvider provider) {
		super(text);
		Assert.isNotNull(provider);
		fProvider= provider;
	}
	
	protected StructuredSelectionProvider getProvider() {
		return fProvider;
	}
	
	/**
	 * Returns <code>true</code> iff the action can operate on the specified selection.
	 * @return <code>true</code> if the action can operate on the specified selection, 
	 *  <code>false</code> otherwise.
	 */
	public abstract boolean canOperateOn(IStructuredSelection selection);
	
	/**
	 * Returns the current selection.
	 * 
	 * @return the current selection as a structured selection or <code>null</code>
	 */
	protected IStructuredSelection getStructuredSelection() {
		return fProvider.getSelection();
	}
	
	/**
	 * Update the action's enable state according to the current selection of
	 * the used selection provider.
	 */
	public void update() {
		IStructuredSelection selection= getStructuredSelection();
		if (selection == null)
			setEnabled(false);
		else	
			setEnabled(canOperateOn(selection));
	}

	//--- wizard and dialog related methods + editor saving
	
	public static void activateRefactoringWizard(Refactoring refactoring, RefactoringWizard wizard, String dialogTitle, boolean mustSaveEditors) throws JavaModelException {
		if (! canActivate(mustSaveEditors))
			return;
		RefactoringStatus activationStatus= refactoring.checkActivation(new NullProgressMonitor());
		if (! activationStatus.hasFatalError()){
			wizard.setActivationStatus(activationStatus);
			new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard).open();
		} else{
			openErrorDialog(dialogTitle, activationStatus);
		}	
	}
	
	public static void activateRenameRefactoringDialog(final IRenameRefactoring renameRefactoring, String dialogTitle, String dialogMessage, boolean mustSaveEditors, Object element) throws JavaModelException {
		if (! canActivate(mustSaveEditors))
			return;
		//XXX
		if (! checkReadOnly(element))
			return;
		Refactoring refactoring= (Refactoring)renameRefactoring;
		RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor()); 
		if (status.hasFatalError()){
			openErrorDialog(dialogTitle, status);
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
						return "Unexpected exception. See log for details";//don't want to show the error dialog
					}
				}
			};
			InputDialog dialog= new InputDialog(shell, dialogTitle, dialogMessage, renameRefactoring.getCurrentName(), validator);
			int result= dialog.open();
			if (result != Window.OK)
				return;
			renameRefactoring.setNewName(dialog.getValue());
			PerformChangeOperation pco= new PerformChangeOperation(new CreateChangeOperation(refactoring, CheckConditionsOperation.PRECONDITIONS));
			PerformRefactoringUtil.performRefactoring(pco, refactoring);
		} 		
	}
	
	private static boolean checkReadOnly(Object element) throws JavaModelException{
		//Do a quick read only check
		if (isReadOnly(element))
			return MessageDialog.openQuestion(
				JavaPlugin.getActiveWorkbenchShell(),
				"Rename",
				MessageFormat.format("{0} is read only. Do you still wish to rename it?", new Object[] {ReorgUtils.getName(element)}));
		else
			return true;
	}
	
	private static boolean isReadOnly(Object element) throws JavaModelException{
		if (element instanceof IResource)
			return ((IResource)element).isReadOnly();
		if (element instanceof IJavaProject)	
			return ((IJavaProject)element).getCorrespondingResource().isReadOnly();
		if (element instanceof IPackageFragmentRoot)
			return isReadOnly((IPackageFragmentRoot)element);
		Assert.isTrue(false);
		return false;	
	}
	
	private static boolean isReadOnly(IPackageFragmentRoot root) throws JavaModelException{
		if (Checks.isClasspathDelete(root))
			return false;
		return root.getCorrespondingResource().isReadOnly();
	}
	
	private static boolean canActivate(boolean mustSaveEditors) {
		return ! mustSaveEditors || areAllEditorsSaved();
	}
	
	public static void openErrorDialog(String dialogTitle, RefactoringStatus status) {
		if (status.getEntries().size() == 1) {
			String message= "The following error prevents performing the operation.\n\n" + status.getFirstMessage(RefactoringStatus.FATAL);
			MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), dialogTitle, message);
		} else {
			openListDialog(dialogTitle, status);	
		}
	}
	
	private static void openListDialog(String dialogTitle, RefactoringStatus status) {
		ListDialog dialog= new ListDialog(JavaPlugin.getActiveWorkbenchShell());
		dialog.setInput(status);
		dialog.setTitle(dialogTitle);
		dialog.setMessage("The following errors prevent performing the operation.");
		dialog.setContentProvider(new RefactoringStatusContentProvider());
		dialog.setLabelProvider(new RefactoringStatusEntryLabelProvider());
		dialog.open();	
	}

	private static boolean areAllEditorsSaved(){
		if (JavaPlugin.getDirtyEditors().length == 0)
			return true;
		if (! saveAllDirtyEditors())
			return false;
		try {
			// Save isn't cancelable.
			new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()).run(false, false, createRunnable());
			return true;
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, JavaPlugin.getActiveWorkbenchShell(), "Saving Resources", "Unexpected exception. See log for details."); 
			return false;
		} catch (InterruptedException e) {
			Assert.isTrue(false); // Can't happen. Operation isn't cancelable.
			return false;
		}
	}

	/**
	 * Creates a runnable to be used inside an operation to save all editors.
	 */
	private static IRunnableWithProgress createRunnable() {
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

	private static boolean saveAllDirtyEditors() {
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

	private static ILabelProvider createDialogLabelProvider() {
		return new LabelProvider() {
			public Image getImage(Object element) {
				return ((IEditorPart) element).getTitleImage();
			}
			public String getText(Object element) {
				return ((IEditorPart) element).getTitle();
			}
		};
	}
}