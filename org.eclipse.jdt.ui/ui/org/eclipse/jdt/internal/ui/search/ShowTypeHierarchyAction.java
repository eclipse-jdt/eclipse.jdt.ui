/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;
import org.eclipse.jdt.ui.JavaUI;/**
 * Shows the type hierarchy on a single selected element of type IType, ICompilationUnit or IClassFile 
 */
public class ShowTypeHierarchyAction extends JavaElementAction {
	
	protected static final String PREFIX= "ShowTypeHierarchyAction.";
	protected static final String ERROR_OPEN_VIEW= PREFIX+"error.open_view";
	
	public ShowTypeHierarchyAction() {
		super(JavaPlugin.getResourceString(PREFIX + "label"), new Class[] {IType.class});
	}
	/**
	 * Perform the action
	 */
	protected void run(IJavaElement element) {
		if (element instanceof IType)
			showType((IType)element);
		else
			beep();
	}

	private void showType(IType type) {
		IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		try {
			IViewPart view= page.showView(JavaUI.ID_TYPE_HIERARCHY);
			((TypeHierarchyViewPart) view).setInput(type);
		} catch (PartInitException ex) {
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JavaPlugin.getResourceString(ERROR_OPEN_VIEW), ex.getMessage());
		}			
	}
}
