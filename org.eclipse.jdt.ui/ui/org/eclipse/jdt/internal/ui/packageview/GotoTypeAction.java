/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.OpenJavaElementAction;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.ui.IJavaElementSearchConstants;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.dialogs.SelectionDialog;

public class GotoTypeAction extends OpenJavaElementAction {
	
	private PackageExplorerPart fPackageExplorer;
	
	public GotoTypeAction(PackageExplorerPart part) {
		super();
		setText(PackagesMessages.getString("GotoType.action.label")); //$NON-NLS-1$
		setDescription(PackagesMessages.getString("GotoType.action.description")); //$NON-NLS-1$
		fPackageExplorer= part;
	}


	public void run() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		SelectionDialog dialog= null;
		try {
			dialog= JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell),
				SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_TYPES, false);
		} catch (JavaModelException e) {
			String title= PackagesMessages.getString("GotoType.dialog.title"); //$NON-NLS-1$
			String message= PackagesMessages.getString("GotoType.error.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, title, message);
			return;
		}
	
		dialog.setTitle(PackagesMessages.getString("GotoType.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(PackagesMessages.getString("GotoType.dialog.message")); //$NON-NLS-1$
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
			}
		}
	}
}