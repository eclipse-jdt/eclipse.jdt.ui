/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.Collections;import java.util.Comparator;import java.util.Iterator;import java.util.List;import java.util.ResourceBundle;import org.eclipse.core.resources.IContainer;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.MultiStatus;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaUIException;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jface.dialogs.InputDialog;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.actions.WorkspaceModifyOperation;

/** 
 * Action for deleting elements in a delete target.
 */
public class DeleteAction extends ReorgAction {
	private final static String PREFIX= "action.delete.";
	private final static String ACTION_NAME= PREFIX + "name";
	private final static String LABEL= PREFIX + "label";
	private final static String DESCRIPTION= PREFIX + "description";
	private final static String ERROR_PREFIX= PREFIX+"error.";
	private final static String ERROR_STATUS= ERROR_PREFIX+"status";
	private final static String TASK=PREFIX+"task.label";
	private final static String CONFIRM_TITLE=PREFIX+"confirm.title";
	private final static String CONFIRM_LABEL=PREFIX+"confirm.label";

	public DeleteAction(ISelectionProvider viewer) {
		super(viewer,JavaPlugin.getResourceString(LABEL));
		setDescription(JavaPlugin.getResourceString(DESCRIPTION));
	}

	/**
	 * The user has invoked this action
	 */
	public void doActionPerformed() {
		Shell activeShell= JavaPlugin.getActiveWorkbenchShell();
		
		String title= JavaPlugin.getResourceString(CONFIRM_TITLE);
		String label= JavaPlugin.getResourceString(CONFIRM_LABEL);
		
		final Iterator iter= getStructuredSelection().iterator();
		final MultiStatus status= new MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, JavaPlugin.getResourceString(ERROR_STATUS), null);
		final List elements= new ArrayList();
		boolean hasReadOnlyResources= false;
		while (iter.hasNext()) {
			Object element= iter.next();
			if (!hasReadOnlyResources && shouldConfirmReadOnly(element))
				hasReadOnlyResources= true;
			elements.add(element);
		}
		
		try {
			if (!isClasspathDelete(elements) && !MessageDialog.openConfirm(activeShell, title, label))
				return;
		} catch (CoreException e) {
			ExceptionHandler.handle(e, "Delete", "An exception occured while deleting elements");
			return;
		}
		if (!confirmIfUnsaved(elements))
			return;

		String msg= "The selected elements contain read-only resources. Do you still wish to delete them?";
		if (hasReadOnlyResources && !confirmReadOnly("Check Deletion", msg))
			return;
						
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor pm) {
				int size= elements.size();
				pm.beginTask(JavaPlugin.getResourceString(TASK), size);
				final IDeleteSupport support= ReorgSupportFactory.createDeleteSupport(elements);
				Comparator lengthComparator= new Comparator() {
					public int compare(Object left, Object right) {
						int leftLength= support.getPathLength(left);
						int rightLength= support.getPathLength(right);
						return rightLength - leftLength;
					}
				};
				
				Collections.sort(elements, lengthComparator);
				
				for (int i= 0; i < size; i++) {
					Object o= elements.get(i);
					try {
						support.delete(o, pm);
					} catch (CoreException e) {
						status.merge(e.getStatus());
					}
				}
				pm.done();
			}
		};
		try {
			new ProgressMonitorDialog(activeShell).run(true, true, op);
		} catch (InvocationTargetException e) {
			// this will never happen
		} catch (InterruptedException e) {
			return;
		}
		if (!status.isOK()) {
			Throwable t= new JavaUIException(status);
			ResourceBundle bundle= JavaPlugin.getResourceBundle();
			ExceptionHandler.handle(t, activeShell, bundle, ERROR_PREFIX);
		}	
	}

	protected boolean canExecute(IStructuredSelection selection) {
		//XXX: Could be done nicer<g>. DB & MA
		if (selection.isEmpty())
			return false;
		Iterator elements= selection.iterator();
		List allElements= new ArrayList();
		while (elements.hasNext()) {
			allElements.add(elements.next());
		}
		
		IDeleteSupport support= ReorgSupportFactory.createDeleteSupport(allElements);
		for (int i= 0; i < allElements.size(); i++) {
			if (!support.canDelete(allElements.get(i)))
				return false;
		}
		return true;
	}
	
	protected String getActionName() {
		return JavaPlugin.getResourceString(ACTION_NAME);
	}
	
	protected boolean isClasspathDelete(List elements) throws CoreException {
		Iterator iter= elements.iterator();
		while (iter.hasNext()) {
			Object o= iter.next();
			if (!(o instanceof IPackageFragmentRoot))
				return false;
			if (!ReorgSupport.isClasspathDelete((IPackageFragmentRoot)o))
				return false;
		}
		return true;
	}
	
}
