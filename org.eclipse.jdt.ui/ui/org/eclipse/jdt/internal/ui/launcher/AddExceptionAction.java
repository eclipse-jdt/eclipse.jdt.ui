/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class AddExceptionAction implements IViewActionDelegate {

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
				IBreakpoint breakpoint= JDIDebugModel.createExceptionBreakpoint(result, caught, uncaught, exceptionKind == AddExceptionDialog.CHECKED_EXCEPTION);
			} catch (DebugException exc) {
				ExceptionHandler.handle(exc, LauncherMessages.getString("addExceptionAction.error.title"), LauncherMessages.getString("addExceptionAction.error.message")); //$NON-NLS-2$ //$NON-NLS-1$
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