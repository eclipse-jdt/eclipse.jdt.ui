/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.List;import org.eclipse.core.resources.IContainer;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.IEditorPart;import org.eclipse.ui.actions.SelectionProviderAction;import org.eclipse.ui.dialogs.ListSelectionDialog;

/**
  * Base class for actions related to reorganizing resources
  */
public abstract class ReorgAction extends SelectionProviderAction {
	
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
	 * Hook to update the action's enable state before it is added to the context
	 * menu. This default implementation does nothing.
	 */
	public void update() {
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
				pm.beginTask(ReorgMessages.getString("ReorgAction.task.saving"), nrFiles*10); //$NON-NLS-1$
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
			if (!ExceptionHandler.handle(e.getTargetException(), getActionName(), ReorgMessages.getString("ReorgAction.exception.saving"))) { //$NON-NLS-1$
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), ReorgMessages.getString("ReorgAction.error.title"), e.getMessage()); //$NON-NLS-1$
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
		return ReorgMessages.getString("ReorgAction.confirmDiscard"); //$NON-NLS-1$
	}
	
	protected String getSaveTargetsMessage() {
		return ReorgMessages.getString("ReorgAction.checkSaveTargets"); //$NON-NLS-1$
	}
	
	// readonly confirmation
	protected boolean shouldConfirmReadOnly(Object element) {
		if (element instanceof IJavaElement) {
			try {
				if ((element instanceof IPackageFragmentRoot) && ReorgSupport.isClasspathDelete((IPackageFragmentRoot)element)) {
					return false;
				}
				element= ((IJavaElement)element).getCorrespondingResource();
			} catch (JavaModelException e) {
				// we catch this, we're only interested in knowing
				// whether to pop up the read-only dialog.
			}
		}
		
		if (element instanceof IResource) {
			return shouldConfirmReadOnly((IResource)element);
		}
		return false;
	}
	
	protected boolean shouldConfirmReadOnly(IResource res) {
		if (res.isReadOnly()) 
			return true;
		if (res instanceof IContainer) {
			IContainer container= (IContainer)res;
			try {
				IResource[] children= container.members();
				for (int i= 0; i < children.length; i++) {
					if (shouldConfirmReadOnly(children[i]))
						return true;
				}
			} catch (CoreException e) {
				// we catch this, we're only interested in knowing
				// whether to pop up the read-only dialog.
			}
		}
		return false;
	}
	
	protected boolean confirmReadOnly(String title, String msg) {
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), title, msg);
	}

}