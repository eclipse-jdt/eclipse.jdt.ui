/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;

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
	private IMethod fMethod;
	private IMarker fMarker;
	
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
		if (fMarker == null) {
			// add breakpoint
			
			try {
				fMarker= JDIDebugModel.createMethodEntryBreakpoint(fMethod, 0);
				DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(fMarker);
			} catch (DebugException x) {
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), "Problems creating breakpoint", x.getMessage());
			}
			
		} else {
			// remove breakpoint
			
			try {
				DebugPlugin.getDefault().getBreakpointManager().removeBreakpoint(fMarker, true);
			} catch (CoreException x) {
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), "Problems removing breakpoint", x.getMessage());
			}
		}
	}
	
	public void update() {
		fMethod= getMethod();
		if (fMethod != null) {
			setEnabled(true);
			fMarker= getMarker(fMethod);
			setText(fMarker == null ? fAddText : fRemoveText);
			setDescription(fMarker == null ? fAddDescription : fRemoveDescription);
			setToolTipText(fMarker == null ? fAddToolTip : fRemoveToolTip);
		} else
			setEnabled(false);
	}
	
	private IMarker getMarker(IMethod method) {
		IBreakpointManager m= DebugPlugin.getDefault().getBreakpointManager();
		IMarker[] bps= m.getBreakpoints(JDIDebugModel.getPluginIdentifier());
		for (int i= 0; i < bps.length; i++) {
			if (JDIDebugModel.isMethodEntryBreakpoint(bps[i])) {
				IMethod container= JDIDebugModel.getMethod(bps[i]);
				if (method.equals(container))
					return bps[i];
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
}


