/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.actions.OpenJavaElementAction;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.ui.IJavaElementSearchConstants;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jface.action.IAction;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.IWorkbenchWindowActionDelegate;import org.eclipse.ui.PartInitException;import org.eclipse.ui.dialogs.SelectionDialog;

public class GotoTypeAction extends OpenJavaElementAction {
	
	private static final String PREFIX= "GotoTypeAction.";
	private static final String DIALOG_PREFIX= PREFIX + "dialog.";
	private static final String ERROR_OPEN_PREFIX= PREFIX + "error.open.";
	private PackageExplorerPart fPackageExplorer;
	
	public GotoTypeAction(PackageExplorerPart part) {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		//setImageDescriptor(JavaPluginImages.DESC_TOOL_OPENTYPE);
		fPackageExplorer= part;
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
			gotoType((IType) types[0]);
		}
				
	}
	
	private void gotoType(IType type) {
		ICompilationUnit cu= (ICompilationUnit)JavaModelUtility.getParent(type, IJavaElement.COMPILATION_UNIT);
		IJavaElement element= null;
		
		if (cu != null) {
			if (cu.isWorkingCopy())
				element= cu.getOriginalElement();
			else
				element= cu;
		}
		else {
			element= JavaModelUtility.getParent(type, IJavaElement.CLASS_FILE);
		}
		if (element != null) {
			PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
			if (view != null) {
				view.selectReveal(new StructuredSelection(element));
				return;
			}
		}
	}
	
	public void dispose() {
		// do nothing.
	}
		
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing. Action doesn't depend on selection.
	}
}