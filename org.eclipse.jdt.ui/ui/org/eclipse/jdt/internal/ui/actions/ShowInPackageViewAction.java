/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

/**
 * On a selected member; opens the Javadoc in an external browser (if existing)
 */
public class ShowInPackageViewAction extends Action implements IUpdate, IObjectActionDelegate {
	private StructuredSelectionProvider fSelectionProvider;
	
	/**
	 * Use only for IWorkbenchWindowActionDelegates!
	 */ 
	public ShowInPackageViewAction() {
		this(null);
	}
	
	public ShowInPackageViewAction(StructuredSelectionProvider provider) {
		super(JavaUIMessages.getString("ShowInPackageViewAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("ShowInPackageViewAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("ShowInPackageViewAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.SHOW_IN_PACKAGEVIEW_ACTION);	
		fSelectionProvider= provider;
	}
	
	public void update() {
		setEnabled(canOperateOn());
	}
	
	private boolean canOperateOn() {
		if (fSelectionProvider != null) {
			IStructuredSelection selection= fSelectionProvider.getSelection(StructuredSelectionProvider.FLAGS_GET_EDITOR_INPUT);
			return selection.size() == 1;
		}
		return false;
	}
	
	private Object getSelectedElement() throws JavaModelException {
		if (fSelectionProvider == null) {
			return null;
		}
		
		IStructuredSelection selection= fSelectionProvider.getSelection(StructuredSelectionProvider.FLAGS_DO_CODERESOLVE | StructuredSelectionProvider.FLAGS_GET_EDITOR_INPUT);
		if (selection.size() != 1)
			return null;
		
		Object obj= selection.getFirstElement();
		if (!(obj instanceof IJavaElement)) {
			return obj;
		}
		IJavaElement elem= (IJavaElement) obj;
		switch (elem.getElementType()) {
			case IJavaElement.PACKAGE_DECLARATION:
				// select package fragment
				elem= JavaModelUtil.getPackageFragmentRoot(elem);
				break;
			case IJavaElement.IMPORT_DECLARATION:
				// select referenced element: package fragment or cu/classfile of referenced type
				IImportDeclaration declaration= (IImportDeclaration) elem;
				if (declaration.isOnDemand()) {
					elem= JavaModelUtil.findTypeContainer(elem.getJavaProject(), Signature.getQualifier(elem.getElementName()));
				} else {
					elem= JavaModelUtil.findType(elem.getJavaProject(), elem.getElementName());
				}
				if (elem instanceof IType) {
					elem= (IJavaElement) JavaModelUtil.getOpenable(elem);
				}
				break;
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.TYPE:
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
			case IJavaElement.INITIALIZER:
				// select parent cu/classfile
				elem= (IJavaElement) JavaModelUtil.getOpenable(elem);
				break;
			case IJavaElement.JAVA_MODEL:
				elem= null;
				break;
			default:
		}
		if (elem instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit) elem;
			if (cu.isWorkingCopy()) {
				elem= cu.getOriginalElement();
			}
		}
		return elem;
	}
		
				
	public void run() {
		try {
			Object element= getSelectedElement();
			if (element == null) {
				return;
			}
			PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
			if (view != null) {
				view.selectReveal(new StructuredSelection(element));
				return;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			String title= JavaUIMessages.getString("ShowInPackageViewAction.error.title"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("ShowInPackageViewAction.error.message"); //$NON-NLS-1$
			ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), title, message, e.getStatus());
		}
	}
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		run();
	}
	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}
	/*
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		setEnabled(!JavaUI.ID_PACKAGES.equals(targetPart.getSite().getId())); // does not work yet
		
		fSelectionProvider= StructuredSelectionProvider.createFrom(targetPart.getSite().getWorkbenchWindow().getSelectionService());		
	}
	
}