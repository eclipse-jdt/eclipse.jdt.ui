/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;


/**
 * Open a resource (ClassFile or CompilationUnit) from the PackageViewer
 */
public class OpenImportDeclarationAction extends Action implements IUpdate {

	private ISelectionProvider fSelectionProvider;
	
	public OpenImportDeclarationAction(ISelectionProvider provider) {
		super(JavaUIMessages.getString("OpenImportDeclarationAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("OpenImportDeclarationAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("OpenImportDeclarationAction.tooltip")); //$NON-NLS-1$
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
		if (!canOperateOn())
			return;
			
		IStructuredSelection selection= (IStructuredSelection)fSelectionProvider.getSelection();	
		IImportDeclaration declaration= (IImportDeclaration)selection.getFirstElement();
		try {
			String containerName;
			if (declaration.isOnDemand()) {
				String importName= declaration.getElementName();
				containerName= importName.substring(0, importName.length() - 2);
			} else {
				containerName= declaration.getElementName();
			}
			IJavaElement element= JavaModelUtil.findTypeContainer(declaration.getJavaProject(), containerName);
			EditorUtility.openInEditor(element);
		} catch (CoreException x) {
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			JavaPlugin.log(x.getStatus());
			ErrorDialog.openError(shell, JavaUIMessages.getString("OpenImportDeclarationAction.errorTitle"), JavaUIMessages.getString("OpenImportDeclarationAction.errorMessage"), x.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
		}
	}
}