package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */

import org.eclipse.debug.core.DebugException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import java.util.Iterator;

/**
 * Drops to the selected stack frame
 */
public class DropToFrameAction extends Action implements IViewActionDelegate {

	private static final String PREFIX= "drop_to_frame_action.";

	protected IStructuredSelection fCurrentSelection;
	/**
	 * Constructs a DropToFrameAction
	 */
	public DropToFrameAction() {
		setEnabled(false);
	}

	public boolean isEnabledFor(Object element) {
		IJavaStackFrame frame= getJavaStackFrame(element);
		return frame != null && frame.supportsDropToFrame();
	}

	protected IJavaStackFrame getJavaStackFrame(Object object) {
		if (object instanceof IAdaptable) {
			return (IJavaStackFrame) ((IAdaptable) object).getAdapter(IJavaStackFrame.class);
		}
		return null;
	}

	/**
	 * Does the specific action of this action to the process.
	 */
	protected void doAction(Object element) throws DebugException {
		getJavaStackFrame(element).dropToFrame();
	}

	/**
	 * @see IActionDelegate
	 */
	public void run(IAction action) {
		Iterator enum= getStructuredSelection().iterator();
		//selectionChanged has already checked for correct selection

		while (enum.hasNext()) {
			Object element= enum.next();
			try {
				doAction(element);
			} catch (DebugException de) {
				DebugUIUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), PREFIX + "error.", de.getStatus());
			}
		}
	}

	/**
	 * @see IAction
	 */
	public void run() {
		run(null);
	}

	protected IStructuredSelection getStructuredSelection() {
		return fCurrentSelection;
	}

	/**
	 * @see IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fCurrentSelection= (IStructuredSelection) sel;
			Object[] elements= fCurrentSelection.toArray();
			action.setEnabled(elements.length == 1 && isEnabledFor(elements[0]));
		}
	}

	/**
	 * @see IViewActionDelegate
	 */
	public void init(IViewPart view) {
	}

}
