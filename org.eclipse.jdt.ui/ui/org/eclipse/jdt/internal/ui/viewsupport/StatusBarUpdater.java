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
	
	private final int LABEL_FLAGS= JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH |
			JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.M_EXCEPTIONS | 
		 	JavaElementLabels.F_APP_TYPE_SIGNATURE;
		 	
	private IStatusLineManager fStatusLineManager;
	
	public StatusBarUpdater(IStatusLineManager statusLineManager) {
		fStatusLineManager= statusLineManager;
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
		return JavaElementLabels.getElementLabel(element, LABEL_FLAGS);
	}
		
	private String formatResourceMessage(IResource element) {
		return element.getFullPath().toString();
	}	

}