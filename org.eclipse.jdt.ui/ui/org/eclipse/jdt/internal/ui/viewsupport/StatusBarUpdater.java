/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;


import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

/**
 * Add the <code>StatusBarUpdater</code> to your ViewPart to have the statusbar
 * describing the selected elements.
 * Java Elements are formatted like this:
 * <ul>
 * <li>IJavaProject: name</li>
 * <li>IPackageFragmentRoot: underlying path (contains project if internal)</li>
 * <li>IPackageFragment: package name '-' path of parent PackageFragmentRoot</li>
 * <li>ICompilationUnit: fully qualified name '-' path of parent PackageFragmentRoot</li>
 * <li>IClassFile: fully qualified name '-' path of parent PackageFragmentRoot</li>
 * <li>IType: fully qualified name '-' path of IClassFile/ICompilationUnit or
 * , if in archive, parent PackageFragmentRoot</li>
 * <li>Elements in ICompilationUnit: name '-' path of IClassFile/ICompilationUnit or
 * , if in archive, parent PackageFragmentRoot</li>
 * <li>Fiels, Methods: name with signature '-' fully qualified name of descaring type '-'
 * path of IClassFile/ICompilationUnit or if in archive, parent PackageFragmentRoot</li>
 * </ul>
 */
public class StatusBarUpdater implements ISelectionChangedListener {
	
	private JavaTextLabelProvider fJavaTextLabelProvider;
	private IStatusLineManager fStatusLineManager;
	
	public StatusBarUpdater(IStatusLineManager statusLineManager) {
		fStatusLineManager= statusLineManager;
		
		int flags= JavaElementLabelProvider.SHOW_PARAMETERS
		 				| JavaElementLabelProvider.SHOW_RETURN_TYPE
		 				| JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION;
		fJavaTextLabelProvider= new JavaTextLabelProvider(flags);
	}
		
	/*
	 * @see ISelectionChangedListener#selectionChanged
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		String statusBarMessage= formatMessage(event.getSelection());
		fStatusLineManager.setMessage(statusBarMessage);
	}
	
	
	private String formatMessage(ISelection sel) {
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			IStructuredSelection selection= (IStructuredSelection) sel;
			
			int nElements= selection.size();
			if (nElements > 1) {
				return JavaUIMessages.getFormattedString("StatusBarUpdater.num_elements_selected", String.valueOf(nElements)); //$NON-NLS-1$
			} else { 
				Object elem= selection.getFirstElement();
				if (elem instanceof IJavaElement) {
					return formatJavaElementMessage((IJavaElement) elem);
				} else if (elem instanceof IResource) {
					return formatResourceMessage((IResource) elem);
				}
			}
		}
		return "";  //$NON-NLS-1$
	}
		
	private String formatJavaElementMessage(IJavaElement element) {
		int type= element.getElementType();
		String name= fJavaTextLabelProvider.getTextLabel(element);
		
		if (type == IJavaElement.JAVA_MODEL || type == IJavaElement.JAVA_PROJECT) {
			return name;
		}
		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root == null) {
			JavaPlugin.logErrorMessage("StatusBarUpdater: root can not be null"); //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		}
		
		try {
			String[] args;
			switch (type) {
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					return getRootPath(root);
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.CLASS_FILE:
				case IJavaElement.COMPILATION_UNIT: 
					args= new String[] { name, getRootPath(root) };
					return JavaUIMessages.getFormattedString("StatusBarUpdater.twopart", args); //$NON-NLS-1$ 
				case IJavaElement.TYPE:
				case IJavaElement.IMPORT_CONTAINER:
				case IJavaElement.IMPORT_DECLARATION:
				case IJavaElement.PACKAGE_DECLARATION:
					args= new String[] { name, getUnderlyingResourcePath(element, root) };
					return JavaUIMessages.getFormattedString("StatusBarUpdater.twopart", args); //$NON-NLS-1$ 
				case IJavaElement.FIELD:
				case IJavaElement.METHOD:
				case IJavaElement.INITIALIZER:
					IType declType= ((IMember) element).getDeclaringType();
					String declTypeName= fJavaTextLabelProvider.getTextLabel(declType);
					args= new String[] { name, declTypeName, getUnderlyingResourcePath(element, root) };
					return JavaUIMessages.getFormattedString("StatusBarUpdater.threepart", args); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}		
		return "";
	}
	
	private String getRootPath(IPackageFragmentRoot root) {
		if (root.isExternal()) {
			return root.getPath().toOSString();
		}
		return root.getPath().toString();		
	}
	
	private String getUnderlyingResourcePath(IJavaElement elem, IPackageFragmentRoot root) throws JavaModelException {
		if (root.isArchive()) {
			return getRootPath(root);
		}
		
		IOpenable openable= JavaModelUtil.getOpenable(elem);
		if (openable instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit) openable;
			if (cu.isWorkingCopy()) {
				return cu.getOriginalElement().getUnderlyingResource().getFullPath().toString();
			}
			return cu.getUnderlyingResource().getFullPath().toString();
		} else if (openable instanceof IClassFile) {
			return ((IClassFile)openable).getUnderlyingResource().getFullPath().toString();
		}
		return "";
	}
	
	private String formatResourceMessage(IResource element) {
		return element.getFullPath().toString();
	}	

}