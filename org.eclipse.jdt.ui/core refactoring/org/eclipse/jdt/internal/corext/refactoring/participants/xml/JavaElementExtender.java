/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;


public class JavaElementExtender extends TypeExtender {

	private static final String PROPERTY_IS_AVAILABLE= "isAvailable"; //$NON-NLS-1$
	private static final String CAN_DELETE= "canDelete"; //$NON-NLS-1$
	
	public Object invoke(Object receiver, String method, Object[] args) throws CoreException {
		IJavaElement jElement= (IJavaElement)receiver;
		if (PROPERTY_IS_AVAILABLE.equals(method)) {
			return Boolean.valueOf(Checks.isAvailable(jElement));
		} else if (CAN_DELETE.equals(method)) {
			return Boolean.valueOf(canDelete(jElement));
		}
		Assert.isTrue(false);
		return null;
	}
	
	private boolean canDelete(IJavaElement element) throws CoreException {
		if (! element.exists())
			return false;
		
		if (element instanceof IJavaModel || element instanceof IJavaProject)
			return false;
		
		if (element.getParent() != null && element.getParent().isReadOnly())
			return false;
		
		if (element instanceof IPackageFragmentRoot){
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			if (root.isExternal() || Checks.isClasspathDelete(root)) //TODO rename isClasspathDelete
				return false;
		}
		if (element instanceof IPackageFragment && isEmptySuperPackage((IPackageFragment)element))
			return false;
		
		if (isFromExternalArchive(element))
			return false;
					
		if (element instanceof IMember && ((IMember)element).isBinary())
			return false;
		
		if (ReorgUtils.isDeletedFromEditor(element))
			return false;								
				
		return true;
	}
	
	private static boolean isFromExternalArchive(IJavaElement element) {
		return element.getResource() == null && ! isWorkingCopyElement(element);
	}
	
	private static boolean isWorkingCopyElement(IJavaElement element) {
		if (element instanceof IWorkingCopy) 
			return ((IWorkingCopy)element).isWorkingCopy();
		if (ReorgUtils.isInsideCompilationUnit(element))
			return ReorgUtils.getCompilationUnit(element).isWorkingCopy();
		return false;
	}

	private static boolean isEmptySuperPackage(IPackageFragment pack) throws JavaModelException {
		return  pack.hasSubpackages() &&
				pack.getNonJavaResources().length == 0 &&
				pack.getChildren().length == 0;
	}		
}
