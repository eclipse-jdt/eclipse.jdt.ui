/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class MoveAction extends CopyAction {

	public MoveAction(ISelectionProvider viewer, String name) {
		super(viewer, name);
	}

	public MoveAction(ISelectionProvider viewer) {
		this(viewer, ReorgMessages.getString("moveAction.label")); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("moveAction.description")); //$NON-NLS-1$
	}

	protected boolean canExecute(IStructuredSelection sel) {
		Iterator iter= sel.iterator();
		if (sel.isEmpty())
			return false;
		List allElements= new ArrayList();

		while (iter.hasNext()) {
			allElements.add(iter.next());

		}
		IMoveSupport support= ReorgSupportFactory.createMoveSupport(allElements);
		for (int i= 0; i < allElements.size(); i++) {
			if (!support.isMovable(allElements.get(i)))
				return false;
		}
		return true;
	}
	
	protected String getActionName() {
		return ReorgMessages.getString("moveAction.name"); //$NON-NLS-1$
	}
	
	protected String getDestinationDialogMessage() {
		return ReorgMessages.getString("moveAction.destination.label"); //$NON-NLS-1$
	}
	

	protected void processElements(Shell activeShell, final Object destination, final List elements) {
		Iterator iter= elements.iterator();
		while (iter.hasNext()) {
			Object element= iter.next();
			if (shouldConfirmReadOnly(element)) {
				if (confirmReadOnly(ReorgMessages.getString("moveAction.checkMove"),  //$NON-NLS-1$
					ReorgMessages.getString("moveAction.error.readOnly"))) //$NON-NLS-1$
					break;
				else
					return;
			}
		}

		ArrayList toBeReplaced= new ArrayList();
		final String[] names= getRenamings(activeShell, destination, elements, toBeReplaced);
		// 1GEPGHH: ITPJUI:WINNT - Illogical behaviour when copying over dirty file
		/*if (!confirmIfUnsaved(toBeReplaced))
			return;*/
		if (names == null)
			return;
		final MultiStatus status= new MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, ReorgMessages.getString("moveAction.error.exceptions"), null); //$NON-NLS-1$
		final List createdElements= new ArrayList();
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor pm) {
				int size= elements.size();
				IMoveSupport support= ReorgSupportFactory.createMoveSupport(elements);
				pm.beginTask(getTaskName(), size);
				for (int i= 0; i < size; i++) {
					Object o= elements.get(i);
					try {
						pm.subTask(support.getElementName(o));
						IProgressMonitor subPM= new SubProgressMonitor(pm, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
						Object newElement= support.moveTo(o, destination, names[i], subPM);
						createdElements.add(newElement);
					} catch (CoreException e) {
						status.merge(e.getStatus());
						// 1GEPG0W: ITPJUI:WINNT - problem with smart save while moving 
						// e.printStackTrace();
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
			ExceptionHandler.handle(t, activeShell, ReorgMessages.getString("moveAction.error.title"), ReorgMessages.getString("moveAction.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			select(activeShell, createdElements);
		}
	}

}
