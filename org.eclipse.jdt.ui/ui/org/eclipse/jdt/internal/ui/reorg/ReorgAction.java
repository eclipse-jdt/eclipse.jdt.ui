/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;

/**
  * Base class for actions related to reorganizing resources
  */
public abstract class ReorgAction extends SelectionProviderAction {
	private static final String PREFIX= "action.reorg.";
	private static final String CONFIRM_DISCARD_MSG= PREFIX+"discard_changes.message";
	private static final String SAVE_TARGETS_MSG= PREFIX+"save_targets.message";
	private static final String SAVE_TARGETS_PROGRESS= PREFIX +"save_targets.progress";
	private static final String SAVE_EDITORS_EXCEPTION= PREFIX+"save_editors.error.message";
	
	public ReorgAction(ISelectionProvider p, String name) {
		super(p, name);
	}
	
	public final void run() {
		doActionPerformed();
	}
	
	public final boolean saveEditors() {
		return JavaPlugin.getActivePage().saveAllEditors(true);
	}
	
	/**
	 *Set self's enablement based upon the currently selected resources
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canExecute(selection));
	}

	protected abstract boolean canExecute(IStructuredSelection selection);
	protected abstract void doActionPerformed();
	protected abstract String getActionName();
	protected void select(Shell shell, final List elements) {
		// Must post the selection change to the event queue since 
		// reorg can change the elements displayed in the packages view
		Display display= shell.getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				StructuredSelection sel= new StructuredSelection(elements.toArray());
				getSelectionProvider().setSelection(sel);
			}
		});
	}

	protected boolean ensureSaved(final List elements) {
		final List unsavedEditors= new ArrayList();
		final List unsavedElements= new ArrayList();
		
		collectUnsavedEditors(elements, unsavedEditors, unsavedElements);
		
		if (unsavedEditors.size() == 0)
			return true;
			
		int labelFlags= JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION 
					| JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION;
		Shell parent= JavaPlugin.getActiveWorkbenchShell();			
		ListSelectionDialog dialog= new ListSelectionDialog(parent, unsavedElements, new ListContentProvider(), 
			new JavaElementLabelProvider(labelFlags), getSaveTargetsMessage());
		dialog.setInitialSelections(unsavedElements.toArray());
		if (dialog.open() != dialog.OK)
			return false;
		final Object[] elementsToSave= dialog.getResult();
		final int nrFiles= elementsToSave.length;
		ProgressMonitorDialog pmd= new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell());
		IRunnableWithProgress r= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				pm.beginTask(JavaPlugin.getResourceString(SAVE_TARGETS_PROGRESS), nrFiles*10);
				for (int i= 0; i < nrFiles; i++) {
					int index= elements.indexOf(elementsToSave[i]);
					
					IEditorPart editor= (IEditorPart)unsavedEditors.get(index);
					editor.doSave(new SubProgressMonitor(pm, 10));
				}
				pm.done();
			}
		};
		try {
			pmd.run(false, false, r);
		} catch (InvocationTargetException e) {
			if (!ExceptionHandler.handle(e.getTargetException(), getActionName(), JavaPlugin.getResourceString(SAVE_EDITORS_EXCEPTION))) {
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), "Error", e.getMessage());
			}
			return false;
		} catch (InterruptedException e) {
		}
		return true;
	}

	protected boolean confirmIfUnsaved(List elements) {
		List unsavedEditors= new ArrayList();
		List unsavedElements= new ArrayList();
		
		collectUnsavedEditors(elements, unsavedEditors, unsavedElements);
		
		if (unsavedEditors.size() == 0)
			return true;
			
		int labelFlags= JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION 
					| JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION;
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		ListDialog dialog= new ListDialog(parent, unsavedElements, getActionName(), getConfirmDiscardChangesMessage(), new ListContentProvider(),
			new JavaElementLabelProvider(labelFlags));
		
		return dialog.open() == dialog.OK;
	}

	
	protected void collectUnsavedEditors(List elements, List unsavedEditors, List unsavedElements) {
		
		IEditorPart[] editors= JavaPlugin.getDirtyEditors();
		for (int i= 0; i < editors.length; i++) {
			for (int k= 0; k < elements.size(); k++) {
				Object element= elements.get(k);
				if (EditorUtility.isEditorInput(element, editors[i])) {
					unsavedEditors.add(editors[i]);
					unsavedElements.add(element);
				}
			}
		}
	}
	
	protected String getConfirmDiscardChangesMessage() {
		return JavaPlugin.getResourceString(CONFIRM_DISCARD_MSG);
	}
	
	protected String getSaveTargetsMessage() {
		return JavaPlugin.getResourceString(SAVE_TARGETS_MSG);
	}
}