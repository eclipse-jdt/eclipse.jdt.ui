/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.debug.core.JDIDebugModel;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

/**
 * Adds a method entry breakpoint on a single selected element of type IMethod 
 */
public class AddMethodEntryBreakpointAction extends Action implements IUpdate {
	
	private ISelectionProvider fSelectionProvider;
	
	public AddMethodEntryBreakpointAction(ISelectionProvider selProvider) {
		super(JavaUIMessages.getString("AddMethodEntryBreakpointAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("AddMethodEntryBreakpointAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("AddMethodEntryBreakpointAction.tooltip")); //$NON-NLS-1$
		fSelectionProvider= selProvider;
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.ADD_METHODBREAKPOINT_ACTION });
	}

	/**
	 * Perform the action
	 */
	public void run() {
		ISelection sel= fSelectionProvider.getSelection();
		if (!(sel instanceof IStructuredSelection))
			return;
		IMethod method = (IMethod)((IStructuredSelection)sel).getFirstElement();
		try {
			IMarker breakpoint = JDIDebugModel.createMethodEntryBreakpoint(method, 0);
			DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(breakpoint);
		} catch (DebugException x) {
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JavaUIMessages.getString("AddMethodEntryBreakpointAction.errorTitle"), x.getMessage()); //$NON-NLS-1$
		}
	}
	
	public void update() {
		setEnabled(canActionBeAdded());
	}
	
	public boolean canActionBeAdded() {
		ISelection sel= fSelectionProvider.getSelection();
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			IStructuredSelection s = (IStructuredSelection)sel;
			Object o= s.getFirstElement();
			return s.size() == 1 &&  o instanceof IMethod && ((IMethod)o).isBinary();
		}
		return false;
	}

}


