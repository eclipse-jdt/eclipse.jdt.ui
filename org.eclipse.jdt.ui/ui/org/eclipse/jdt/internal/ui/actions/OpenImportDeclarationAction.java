/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.actions;

import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;


/**
 * Open a resource (ClassFile or CompilationUnit) from the PackageViewer
 */
public class OpenImportDeclarationAction extends JavaUIAction implements IUpdate {

	private static final String PREFIX= "OpenImportDeclarationAction.";	
	private final static String ERROR_TITLE= "OpenImportDeclarationAction.error.open_resource.title";
	private final static String ERROR_MESSAGE= "OpenImportDeclarationAction.error.open_resource.message";
	
	private ISelectionProvider fSelectionProvider;
	
	public OpenImportDeclarationAction(ISelectionProvider provider) {
		super(JavaPlugin.getResourceString(PREFIX + "label"));
		fSelectionProvider= provider;
	}
	
	public void update() {
		setEnabled(canOperateOn());
	}
	
	private boolean canOperateOn() {	
		ISelection s= fSelectionProvider.getSelection();
		if (s.isEmpty() || ! (s instanceof IStructuredSelection))
			return false;

		IStructuredSelection selection= (IStructuredSelection)s;
		if (selection.size() != 1)
			return false;
			
		Object element= selection.getFirstElement();
		if (element instanceof IImportDeclaration && !((IImportDeclaration)element).isOnDemand())
			return true;
			
		return false;	
	}
	
	public void run() {
		ISelection s= fSelectionProvider.getSelection();	
		if (s instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) s;
			IImportDeclaration declaration= (IImportDeclaration) ss.getFirstElement();
			IJavaElement element= JavaModelUtility.convertFromImportDeclaration(declaration);
			try {
				EditorUtility.openInEditor(element);
			} catch (JavaModelException x) {
				Shell shell= JavaPlugin.getActiveWorkbenchShell();
				ResourceBundle b= JavaPlugin.getResourceBundle();
				ErrorDialog.openError(shell, b.getString(ERROR_TITLE), b.getString(ERROR_MESSAGE), x.getStatus());
			} catch (PartInitException x) {
				Shell shell= JavaPlugin.getActiveWorkbenchShell();
				shell.getDisplay().beep();
			}
		}
	}
}