/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
/**
 * Tries to reveal the selected element in the package navigator 
 * view.
 */
public class ShowInPackageViewAction extends Action implements IUpdate {

	private ISelectionProvider fSelectionProvider;

	public ShowInPackageViewAction(IWorkbenchPartSite site, ISelectionProvider provider) {
		super(JavaUIMessages.getString("ShowInPackageViewAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("ShowInPackageViewAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("ShowInPackageViewAction.tooltip")); //$NON-NLS-1$
		Assert.isNotNull(provider);
		fSelectionProvider= provider;
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.SHOW_IN_PACKAGEVIEW_ACTION });	
	}
	
	public void update() {
		setEnabled(canOperateOn());
	}
	
	public boolean canOperateOn() {
		ISelection s= fSelectionProvider.getSelection();
		Object element= SelectionUtil.getSingleElement(s);
		if (!(element instanceof IPackageDeclaration || element instanceof IImportDeclaration || element instanceof IType))
			return false;	
		return true;	
	}
	
	public void run() {
		if (!canOperateOn())
			return;
		Object o= ((IStructuredSelection)fSelectionProvider.getSelection()).getFirstElement();
		IJavaElement element= null;
		if (o instanceof IPackageDeclaration)
			element= JavaModelUtility.getParent((IJavaElement)o, IJavaElement.PACKAGE_FRAGMENT);
		
		else if (o instanceof IImportDeclaration) {
			try {
				element= JavaModelUtility.convertFromImportDeclaration((IImportDeclaration)o);
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, JavaUIMessages.getString("ShowInPackageViewAction.errorTitle"), JavaUIMessages.getString("ShowInPackageViewAction.errorMessage")); //$NON-NLS-2$ //$NON-NLS-1$
			}
			if (element instanceof IType) {
				IJavaElement temp= JavaModelUtility.getParent(element, IJavaElement.COMPILATION_UNIT);
				if (temp == null)
					temp= JavaModelUtility.getParent(element, IJavaElement.CLASS_FILE);
					
				element= temp;
			}
		}
		else if (o instanceof IType) {
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
		//XXX revisit need a standard way to give the user this feedback
		JavaPlugin.getActiveWorkbenchShell().getDisplay().beep();	
	}	
}