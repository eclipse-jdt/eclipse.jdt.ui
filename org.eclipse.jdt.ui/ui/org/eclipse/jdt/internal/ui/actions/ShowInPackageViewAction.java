/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JarEntryEditorInput;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.search.JavaElementAction;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
/**
 * Tries to reveal the selected element in the package navigator 
 * view.
 */
public class ShowInPackageViewAction extends JavaElementAction {

	private ISelectionProvider fSelectionProvider;

	public ShowInPackageViewAction() {
		super(JavaUIMessages.getString("ShowInPackageViewAction.label"), new Class[] {IJavaElement.class} ); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("ShowInPackageViewAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("ShowInPackageViewAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.SHOW_IN_PACKAGEVIEW_ACTION });	
	
	}

	/*
	 * @see JavaElementAction#run(IJavaElement)
	 */
	public void run(IJavaElement o) {
		IJavaElement element= null;
		if (o instanceof IPackageDeclaration)
			element= JavaModelUtil.findParentOfKind((IJavaElement)o, IJavaElement.PACKAGE_FRAGMENT);
		
		else if (o instanceof IImportDeclaration) {
			try {
				IImportDeclaration declaration= (IImportDeclaration) o;
				String containerName;
				if (declaration.isOnDemand()) {
					String importName= declaration.getElementName();
					containerName= importName.substring(0, importName.length() - 2);
				} else {
					containerName= declaration.getElementName();
				}
				element= JavaModelUtil.findTypeContainer(declaration.getJavaProject(), containerName);
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, JavaUIMessages.getString("ShowInPackageViewAction.errorTitle"), JavaUIMessages.getString("ShowInPackageViewAction.errorMessage")); //$NON-NLS-2$ //$NON-NLS-1$
			}
			if (element instanceof IType) {
				IJavaElement temp= JavaModelUtil.findParentOfKind(element, IJavaElement.COMPILATION_UNIT);
				if (temp == null)
					temp= JavaModelUtil.findParentOfKind(element, IJavaElement.CLASS_FILE);
					
				element= temp;
			}
		}
		else if (o instanceof IType) {
			ICompilationUnit cu= (ICompilationUnit)JavaModelUtil.findParentOfKind((IJavaElement)o, IJavaElement.COMPILATION_UNIT);
			if (cu != null) {
				if (cu.isWorkingCopy())
					element= cu.getOriginalElement();
				else
					element= cu;
			}
			else {
				element= JavaModelUtil.findParentOfKind((IJavaElement)o, IJavaElement.CLASS_FILE);
			}
		}
		else if (o instanceof IMember){
			element= (IJavaElement)JavaModelUtil.getOpenable(o);
		}
		if (element != null) {
			showInPackagesView(element);
			return;
		}	
		//XXX revisit need a standard way to give the user this feedback
		JavaPlugin.getActiveWorkbenchShell().getDisplay().beep();	
	}

	protected void showInPackagesView(Object element) {
		PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
		if (view != null) {
			view.selectReveal(new StructuredSelection(element));
			return;
		}
	}
	
	private Object getEditorInput() {
		IEditorPart part= JavaPlugin.getDefault().getActivePage().getActiveEditor();
		if (part != null) {
			IEditorInput input= part.getEditorInput();
			if (input instanceof IClassFileEditorInput)
				return ((IClassFileEditorInput)input).getClassFile();
			if (input instanceof IFileEditorInput) {
				IFile file= ((IFileEditorInput)input).getFile();
				return JavaCore.create(file);
			}
		}
		return null;
	}
		
	/*
	 * @see JavaElementAction#getJavaElement(ITextSelection)
	 */
	protected IJavaElement getJavaElement(ITextSelection selection) {
		if (selection.getLength() == 0) {
			Object input= getEditorInput();
			if (input != null)
				showInPackagesView(input);
			return RETURN_WITHOUT_BEEP;
		}
		return super.getJavaElement(selection);
	}

}