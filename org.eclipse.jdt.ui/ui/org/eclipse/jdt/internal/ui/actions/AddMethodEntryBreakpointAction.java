package org.eclipse.jdt.internal.ui.actions;


import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.debug.core.JDIDebugModel;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Adds a method entry breakpoint on a single selected element of type IMethod 
 */
public class AddMethodEntryBreakpointAction extends JavaUIAction implements IUpdate {
	
	private ISelectionProvider fSelectionProvider;
	
	public static final String PREFIX= "AddMethodEntryBreakpointAction.";
	public static final String ERROR_CREATE_BREAKPOINT= PREFIX+"error.create_breakpoint";
		
	public AddMethodEntryBreakpointAction(ISelectionProvider selProvider) {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		fSelectionProvider= selProvider;
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
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JavaPlugin.getResourceString(ERROR_CREATE_BREAKPOINT), x.getMessage());
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


