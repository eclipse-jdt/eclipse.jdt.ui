/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.debug.core.IJavaMethodEntryBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;


/**
 * Adds a method entry breakpoint on a single selected element of type IMethod 
 */
public class AddMethodEntryBreakpointAction extends Action implements IUpdate {
	
	private ISelectionProvider fSelectionProvider;
	private IMethod fMethod;
	private IBreakpoint fBreakpoint;
	
	private String fAddText, fAddDescription, fAddToolTip;
	private String fRemoveText, fRemoveDescription, fRemoveToolTip;
	
	
	public AddMethodEntryBreakpointAction(ISelectionProvider provider) {
		super();
		
		fSelectionProvider= provider;
		
		fAddText= "&Add Entry Breakpoint";
		fAddDescription= "Add a method entry breakpoint";
		fAddToolTip= "Add Entry Breakpoint";
		
		fRemoveText= "Remove &Entry Breakpoint";
		fRemoveDescription= "Remove a method entry breakpoint";
		fRemoveToolTip= "Remove Entry Breakpoint";
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.ADD_METHODBREAKPOINT_ACTION });
	}
	
	/**
	 * Perform the action
	 */
	public void run() {
		if (fBreakpoint == null) {
			// add breakpoint
			
			try {
				fBreakpoint= JDIDebugModel.createMethodEntryBreakpoint(fMethod, 0);
			} catch (DebugException x) {
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), "Problems creating breakpoint", x.getMessage());
			}
			
		} else {
			// remove breakpoint
			try {
				getBreakpointManager().removeBreakpoint(fBreakpoint, true);
			} catch (CoreException x) {
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), "Problems removing breakpoint", x.getMessage());
			}
		}
	}
	
	public void update() {
		fMethod= getMethod();
		if (fMethod != null) {
			setEnabled(true);
			fBreakpoint= getBreakpoint(fMethod);
			setText(fBreakpoint == null ? fAddText : fRemoveText);
			setDescription(fBreakpoint == null ? fAddDescription : fRemoveDescription);
			setToolTipText(fBreakpoint == null ? fAddToolTip : fRemoveToolTip);
		} else
			setEnabled(false);
	}
	
	private IBreakpoint getBreakpoint(IMethod method) {
		IBreakpoint[] breakpoints= getBreakpointManager().getBreakpoints(JDIDebugModel.getPluginIdentifier());
		for (int i= 0; i < breakpoints.length; i++) {
			IBreakpoint breakpoint= breakpoints[i];
			if (breakpoint instanceof IJavaMethodEntryBreakpoint) {
				IMethod container= ((IJavaMethodEntryBreakpoint) breakpoint).getMethod();
				if (method.equals(container))
					return breakpoint;
			}
		}
		return null;
	}
	
	private IMethod getMethod() {
		ISelection s= fSelectionProvider.getSelection();
		if (s instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) s;
			if (ss.size() == 1) {
				Object o=  ss.getFirstElement();
				if (o instanceof IMethod)
					return (IMethod) o;
			}
		}
		return null;
	}
	
	private IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}
}


