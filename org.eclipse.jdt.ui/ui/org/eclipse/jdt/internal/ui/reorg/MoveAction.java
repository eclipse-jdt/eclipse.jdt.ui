/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
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

	private final static String PREFIX= "action.move.";
	private final static String NAME= PREFIX + "name";
	private final static String LABEL= PREFIX + "label";
	private final static String DESCRIPTION= PREFIX + "description";
	private final static String ERROR_EXCEPTION_PREFIX= PREFIX+"error.exception.";
	private final static String ERROR_STATUS= ERROR_EXCEPTION_PREFIX+"status";

	private final static String ACTION_NAME= PREFIX + "name";

	public MoveAction(ISelectionProvider viewer, String name) {
		super(viewer, name);
	}

	public MoveAction(ISelectionProvider viewer) {
		this(viewer, JavaPlugin.getResourceString(LABEL));
		setDescription(JavaPlugin.getResourceString(DESCRIPTION));
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

	protected void processElements(Shell activeShell, final Object destination, final List elements) {
		ArrayList toBeReplaced= new ArrayList();
		final String[] names= getRenamings(activeShell, destination, elements, toBeReplaced);
		if (!confirmIfUnsaved(toBeReplaced))
			return;
		if (names == null)
			return;
		final MultiStatus status= new MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, JavaPlugin.getResourceString(ERROR_STATUS), null);
		final List createdElements= new ArrayList();
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor pm) {
				int size= elements.size();
				pm.beginTask(getTaskName(), size);
				IMoveSupport support= ReorgSupportFactory.createMoveSupport(elements);
				for (int i= 0; i < size; i++) {
					IProgressMonitor subPM= new SubProgressMonitor(pm, 1);
					Object o= elements.get(i);
					try {
						Object newElement= support.moveTo(o, destination, names[i], subPM);
						createdElements.add(newElement);
					} catch (CoreException e) {
						status.merge(e.getStatus());
						e.printStackTrace();
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
			ExceptionHandler.handle(t, activeShell, bundle, ERROR_EXCEPTION_PREFIX);
		} else {
			select(activeShell, createdElements);
		}
	}

	protected String getPrefix() {
		return PREFIX;
	}

}
