/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.core.resources.IMarker;import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.IBreakpointManager;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.debug.core.JDIDebugModel;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jface.action.IAction;import org.eclipse.jface.viewers.ISelection;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.IViewActionDelegate;import org.eclipse.ui.IViewPart;

public class AddExceptionAction implements IViewActionDelegate {
	protected final static String PREFIX= "launcher.add_exception.";
	protected final static String ERROR_HIERARCHY_PREFIX= PREFIX+"error_hierarchy.";
	protected final static String ERROR_HIERARCHY_STATUS= ERROR_HIERARCHY_PREFIX+"status";


	public void run(IAction action) {
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		AddExceptionDialog dialog= new AddExceptionDialog(shell);
		if (dialog.open() == dialog.OK) {
			IType result= (IType)dialog.getResult();
			int exceptionKind= dialog.getExceptionType();
			if (exceptionKind < 0)
				return;
				
			boolean caught= dialog.isCaughtSelected();
			boolean uncaught= dialog.isUncaughtSelected();
			IBreakpointManager mgr= DebugPlugin.getDefault().getBreakpointManager();
			try {
				IMarker e= JDIDebugModel.createExceptionBreakpoint(result, caught, uncaught, exceptionKind == AddExceptionDialog.CHECKED_EXCEPTION);
				mgr.addBreakpoint(e);
			} catch (DebugException exc) {
				ExceptionHandler.handle(exc, "Add Exception", "An exception occured while adding the breakpoint");
			}
			
		}
	}
	
	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart view) {
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

}