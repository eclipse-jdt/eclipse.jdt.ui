/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringPreferences;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusContentProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusEntryLabelProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;

public abstract class RefactoringAction extends Action {

	private StructuredSelectionProvider fProvider;

	protected RefactoringAction(String text, StructuredSelectionProvider provider) {
		super(text);
		fProvider= provider;
		Assert.isNotNull(fProvider);
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
		boolean enabled= false;
		if (selection != null)
			enabled= canOperateOn(selection);
			
		setEnabled(enabled);
	}	
	
	public static void activateRefactoringWizard(Refactoring refactoring, RefactoringWizard wizard, String dialogTitle, boolean mustSaveEditors) throws JavaModelException {
		if (mustSaveEditors && ! areAllEditorsSaved())
			return;
		RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor());
		if (! status.hasFatalError()){
			wizard.setActivationStatus(status);
			new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard).open();
		} else{
			if (status.getEntries().size() == 1){
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), dialogTitle, status.getFirstMessage(RefactoringStatus.FATAL));
			}else{
				ListDialog dialog= new ListDialog(JavaPlugin.getActiveWorkbenchShell());
				dialog.setInput(status);
				dialog.setTitle(dialogTitle);
				dialog.setMessage("The following errors prevent performing the refactoring.");
				dialog.setContentProvider(new RefactoringStatusContentProvider());
				dialog.setLabelProvider(new RefactoringStatusEntryLabelProvider());
				dialog.open();															
			}	
		}	
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

