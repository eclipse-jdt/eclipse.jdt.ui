/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.Collections;import java.util.Comparator;import java.util.Iterator;import java.util.List;import java.util.ResourceBundle;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.core.resources.IProject;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.MultiStatus;import org.eclipse.ui.actions.WorkspaceModifyOperation;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaUIException;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

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
	private final static String CONFIRM_DELETE_PROJECT_TITLE= PREFIX + "confirm.project.title";
	private final static String CONFIRM_DELETE_PROJECT_MESSAGE1= PREFIX + "confirm.project1.message";	private final static String CONFIRM_DELETE_PROJECT_MESSAGEN= PREFIX + "confirm.projectN.message";

	private boolean fDeleteProjectContent;

	public DeleteAction(ISelectionProvider viewer) {
		super(viewer,JavaPlugin.getResourceString(LABEL));
		setDescription(JavaPlugin.getResourceString(DESCRIPTION));
	}
	/**
	 * The user has invoked this action
	 */
	public void doActionPerformed() {
		Shell activeShell= JavaPlugin.getActiveWorkbenchShell();
		
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
		
		fDeleteProjectContent= false;
		// fix: 1GEPN8H: ITPJUI:WIN98 - Packages View Delete does not confirm
		if (!confirmDelete(activeShell, getStructuredSelection()))
			return;
		// end fix.

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
						support.delete(o, fDeleteProjectContent, pm);
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
		if (selection.isEmpty())
			return false;
		Iterator elements= selection.iterator();
		List allElements= new ArrayList();
		// begin 1GEZU7T: ITPJUI:ALL - Track workbench changes to DeleteAction
		boolean projectElementFound= false;
		boolean nonProjectElementFound= false;
		while (elements.hasNext()) {
			Object item= elements.next();
			if (item instanceof IJavaProject) {
				projectElementFound= true;
			} else {
				nonProjectElementFound= true;
			}
			if (projectElementFound && nonProjectElementFound)
				return false;
			allElements.add(item);
		}
		// end 1GEZU7T: ITPJUI:ALL - Track workbench changes to DeleteAction		
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
	
	private boolean confirmDelete(Shell parent, IStructuredSelection selection) {
		// 1GEZU7T: ITPJUI:ALL - Track workbench changes to DeleteAction
		List projects= getProjects(selection, parent);
		if (projects.size() == 0) {
			return confirmNonProjects(parent);
		} else {
			return confirmProjets(parent, projects);
		}
	}
	
	private boolean confirmNonProjects(Shell parent) {	
		// 1GEZU7T: ITPJUI:ALL - Track workbench changes to DeleteAction
		String title= JavaPlugin.getResourceString(CONFIRM_TITLE);
		String label= JavaPlugin.getResourceString(CONFIRM_LABEL);
		// fix: 1GEPN8H: ITPJUI:WIN98 - Packages View Delete does not confirm
		return MessageDialog.openConfirm(parent, title, label);
		// end fix.
	}
	
	private boolean confirmProjets(Shell parent, List projects) {
		// 1GEZU7T: ITPJUI:ALL - Track workbench changes to DeleteAction
		String title = JavaPlugin.getResourceString(CONFIRM_DELETE_PROJECT_TITLE);
		String msg;
		if (projects.size() == 1) {
			IProject project = (IProject)projects.get(0);
			msg =
				JavaPlugin.getFormattedString(
					CONFIRM_DELETE_PROJECT_MESSAGE1,
					new String[] { project.getName(), project.getLocation().toOSString()});
			//$NON-NLS-1$
		} else {
			msg =
				JavaPlugin.getFormattedString(
					CONFIRM_DELETE_PROJECT_MESSAGEN,
					new String[] { "" + projects.size() });
			//$NON-NLS-1$
		}
		
		MessageDialog dialog =
			new MessageDialog(parent, title, null, // accept the default window icon
				msg,
				MessageDialog.QUESTION,
				new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.NO_LABEL,
					IDialogConstants.CANCEL_LABEL },
				0);
				
		// yes is the default
		int code = dialog.open();
		switch (code) {
			case 0 : // YES
				fDeleteProjectContent = true;
				return true;
			case 1 : // NO
				fDeleteProjectContent = false;
				return true;
			default : // CANCEL and close dialog
				return false;
		}
	}
	
	private List getProjects(IStructuredSelection selection, Shell parent) {
		// 1GEZU7T: ITPJUI:ALL - Track workbench changes to DeleteAction
		List result= new ArrayList();
		for(Iterator iter= selection.iterator(); iter.hasNext(); ) {
			Object element= iter.next();
			if (element instanceof IJavaProject) {
				try {
					result.add(((IJavaProject)element).getUnderlyingResource());
				} catch (JavaModelException e) {
					if (!e.isDoesNotExist()) {
						ExceptionHandler.handle(e, parent, JavaPlugin.getResourceBundle(), ERROR_PREFIX);
					}
				}
			}
		}
		return result;
	}	
}
