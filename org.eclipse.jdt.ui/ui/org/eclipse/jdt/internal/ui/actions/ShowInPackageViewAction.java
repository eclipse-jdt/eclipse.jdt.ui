/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.util.PortingFinder;

/**
 * Tries to reveal the selected element in the package navigator 
 * view.
 */
public class ShowInPackageViewAction extends JavaUIAction implements IUpdate {

	private static final String PREFIX= "ShowInPackageViewAction.";
	
	private ISelectionProvider fSelectionProvider;

	public ShowInPackageViewAction(ISelectionProvider provider) {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		fSelectionProvider= provider;
		Assert.isNotNull(fSelectionProvider);
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
		if (element instanceof IPackageDeclaration || element instanceof IImportDeclaration || element instanceof IType)
			return true;
			
		return false;	
	}
	
	public void run() {
		Object o= ((IStructuredSelection)fSelectionProvider.getSelection()).getFirstElement();
		IJavaElement element= null;
		if (o instanceof IPackageDeclaration)
			element= JavaModelUtility.getParent((IJavaElement)o, IJavaElement.PACKAGE_FRAGMENT);
		
		if (o instanceof IImportDeclaration) {
			try {
				element= JavaModelUtility.convertFromImportDeclaration((IImportDeclaration)o);
			} catch (JavaModelException e) {
				PortingFinder.toBeDone("handle Exception");
			}
			if (element instanceof IType) {
				IJavaElement temp= JavaModelUtility.getParent(element, IJavaElement.COMPILATION_UNIT);
				if (temp == null)
					temp= JavaModelUtility.getParent(element, IJavaElement.CLASS_FILE);
					
				element= temp;
			}
		}
		
		if (o instanceof IType) {
			ICompilationUnit cu= (ICompilationUnit)JavaModelUtility.getParent((IJavaElement)o, IJavaElement.COMPILATION_UNIT);
			if (cu != null) {
				if (cu.isWorkingCopy())
					element= cu.getOriginalElement();
				else
					element= cu;
			}
			else {
				element= JavaModelUtility.getParent((IJavaElement)o, IJavaElement.CLASS_FILE);
			}
		}
		if (element != null) {
			PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
			if (view != null) {
				view.selectReveal(new StructuredSelection(element));
				return;
			}
		}	
		JavaPlugin.getActiveWorkbenchShell().getDisplay().beep();	
	}	
}