/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class OpenTypeAction extends OpenJavaElementAction implements IWorkbenchWindowActionDelegate {
	
	private static final String PREFIX= "OpenTypeAction.";
	private static final String DIALOG_PREFIX= PREFIX + "dialog.";
	private static final String ERROR_OPEN_PREFIX= PREFIX + "error.open.";
	
	public OpenTypeAction() {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		setImageDescriptor(JavaPluginImages.DESC_TOOL_OPENTYPE);
	}

	public void run() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		SelectionDialog dialog= null;
		try {
			dialog= JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell),
				SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_TYPES, false);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, JavaPlugin.getResourceBundle(), ERROR_OPEN_PREFIX);
			return;
		}
	
		dialog.setTitle(JavaPlugin.getResourceString(DIALOG_PREFIX + "title"));
		dialog.setMessage(JavaPlugin.getResourceString(DIALOG_PREFIX + "message"));
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			try {
				open((IType) types[0]);
			} catch (JavaModelException x) {
				ExceptionHandler.handle(x, JavaPlugin.getResourceBundle(), ERROR_OPEN_PREFIX);
			} catch (PartInitException x) {
			}
		}
				
	}

	//---- IWorkbenchWindowActionDelegate ------------------------------------------------

	public void run(IAction action) {
		run();
	}
	
	public void dispose() {
		// do nothing.
	}
	
	public void init(IWorkbenchWindow window) {
		// do nothing.
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing. Action doesn't depend on selection.
	}
}